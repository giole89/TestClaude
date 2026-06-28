package com.finai.service;

import com.finai.domain.entity.SimPosition;
import com.finai.domain.entity.SimTrade;
import com.finai.domain.entity.SimWallet;
import com.finai.dto.quote.QuoteDto;
import com.finai.dto.simulator.*;
import com.finai.exception.FinaiException;
import com.finai.repository.SimPositionRepository;
import com.finai.repository.SimTradeRepository;
import com.finai.repository.SimWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic per l'ambiente di simulazione investimenti (paper trading).
 *
 * <p>Permette di comprare/vendere titoli reali con moneta virtuale, ai prezzi
 * live di Yahoo Finance, per studiare l'andamento di un investimento senza
 * rischio di capitale reale. Wallet, posizioni e storico sono persistiti su
 * Postgres (singola istanza, nessuna autenticazione multi-utente).</p>
 */
@Service
@Transactional(readOnly = true)
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);
    private static final String WALLET_ID = "default";
    private static final BigDecimal DEFAULT_STARTING_BALANCE = BigDecimal.valueOf(100_000);

    private final SimWalletRepository   walletRepo;
    private final SimPositionRepository positionRepo;
    private final SimTradeRepository    tradeRepo;
    private final YahooFinanceService   yahoo;

    public SimulatorService(SimWalletRepository walletRepo,
                            SimPositionRepository positionRepo,
                            SimTradeRepository tradeRepo,
                            YahooFinanceService yahoo) {
        this.walletRepo   = walletRepo;
        this.positionRepo = positionRepo;
        this.tradeRepo    = tradeRepo;
        this.yahoo        = yahoo;
    }

    private SimWallet loadWallet() {
        return walletRepo.findById(WALLET_ID).orElseGet(() -> {
            SimWallet w = new SimWallet();
            w.setId(WALLET_ID);
            w.setCashBalance(DEFAULT_STARTING_BALANCE);
            w.setStartingBalance(DEFAULT_STARTING_BALANCE);
            return walletRepo.save(w);
        });
    }

    /** Restituisce wallet (liquidità + capitale iniziale) senza valorizzare le posizioni. */
    public SimWallet getWallet() {
        return loadWallet();
    }

    /**
     * Calcola il riepilogo completo: liquidità, posizioni valorizzate ai prezzi
     * live di Yahoo Finance (fetch batch), valore totale e P&L vs capitale iniziale.
     */
    public SimSummaryDto getSummary() {
        SimWallet wallet = loadWallet();
        List<SimPosition> positions = positionRepo.findAllByOrderByCreatedAtDesc();

        Map<String, Double> prices = fetchLivePrices(positions.stream()
                .map(SimPosition::getTicker).collect(Collectors.toSet()));

        List<SimPositionDto> positionDtos = positions.stream()
                .map(p -> SimPositionDto.of(p, prices.get(p.getTicker())))
                .toList();

        double cash = wallet.getCashBalance().doubleValue();
        double positionsValue = positionDtos.stream().mapToDouble(SimPositionDto::value).sum();
        double totalValue = cash + positionsValue;
        double startingBalance = wallet.getStartingBalance().doubleValue();
        double totalPnl = totalValue - startingBalance;
        double totalPnlPct = startingBalance > 0 ? (totalPnl / startingBalance) * 100.0 : 0.0;

        return new SimSummaryDto(cash, startingBalance, positionsValue, totalValue,
                totalPnl, totalPnlPct, positionDtos);
    }

    /** Storico di tutte le operazioni eseguite, più recenti prime. */
    public List<SimTradeDto> getTrades() {
        return tradeRepo.findAllByOrderByExecutedAtDesc().stream().map(SimTradeDto::from).toList();
    }

    /**
     * Esegue un acquisto simulato al prezzo live corrente del ticker.
     *
     * @throws FinaiException se il prezzo non è disponibile o la liquidità è insufficiente
     */
    @Transactional
    public SimSummaryDto buy(BuyRequest req) {
        String ticker = req.ticker().toUpperCase();
        QuoteDto quote = yahoo.fetchQuote(ticker);
        if (quote == null || quote.price() == null) {
            throw new FinaiException("Prezzo non disponibile per " + ticker, 502);
        }

        BigDecimal price = BigDecimal.valueOf(quote.price());
        BigDecimal qty   = BigDecimal.valueOf(req.qty());
        BigDecimal cost  = price.multiply(qty).setScale(4, RoundingMode.HALF_UP);

        SimWallet wallet = loadWallet();
        if (wallet.getCashBalance().compareTo(cost) < 0) {
            throw new FinaiException("Liquidità insufficiente: disponibili " +
                    wallet.getCashBalance() + ", richiesti " + cost, 422);
        }

        wallet.setCashBalance(wallet.getCashBalance().subtract(cost));
        walletRepo.save(wallet);

        String name = quote.name() != null ? quote.name() : ticker;
        String currency = quote.currency() != null ? quote.currency() : "USD";

        SimPosition position = positionRepo.findByTicker(ticker).orElse(null);
        if (position == null) {
            position = new SimPosition();
            position.setId(UUID.randomUUID().toString());
            position.setTicker(ticker);
            position.setName(name);
            position.setQty(qty);
            position.setAvgPrice(price);
            position.setCurrency(currency);
        } else {
            // Media ponderata tra la posizione esistente e il nuovo acquisto.
            BigDecimal totalCost = position.getAvgPrice().multiply(position.getQty()).add(cost);
            BigDecimal newQty    = position.getQty().add(qty);
            position.setAvgPrice(totalCost.divide(newQty, 4, RoundingMode.HALF_UP));
            position.setQty(newQty);
            position.setName(name);
            position.setCurrency(currency);
        }
        position.setUpdatedAt(Instant.now());
        positionRepo.save(position);

        recordTrade(ticker, name, "BUY", qty, price, cost, null, currency);

        log.info("Sim BUY {} qty={} price={}", ticker, qty, price);
        return getSummary();
    }

    /**
     * Esegue una vendita simulata al prezzo live corrente del ticker.
     *
     * @throws FinaiException se la posizione non esiste, la quantità è insufficiente
     *                         o il prezzo non è disponibile
     */
    @Transactional
    public SimSummaryDto sell(SellRequest req) {
        String ticker = req.ticker().toUpperCase();
        SimPosition position = positionRepo.findByTicker(ticker)
                .orElseThrow(() -> new FinaiException("Nessuna posizione simulata su " + ticker, 404));

        BigDecimal qty = BigDecimal.valueOf(req.qty());
        if (position.getQty().compareTo(qty) < 0) {
            throw new FinaiException("Quantità insufficiente: detenute " +
                    position.getQty() + ", richieste " + qty, 422);
        }

        QuoteDto quote = yahoo.fetchQuote(ticker);
        if (quote == null || quote.price() == null) {
            throw new FinaiException("Prezzo non disponibile per " + ticker, 502);
        }

        BigDecimal price  = BigDecimal.valueOf(quote.price());
        BigDecimal amount = price.multiply(qty).setScale(4, RoundingMode.HALF_UP);
        BigDecimal realizedPnl = price.subtract(position.getAvgPrice())
                .multiply(qty).setScale(4, RoundingMode.HALF_UP);

        SimWallet wallet = loadWallet();
        wallet.setCashBalance(wallet.getCashBalance().add(amount));
        walletRepo.save(wallet);

        BigDecimal remainingQty = position.getQty().subtract(qty);
        if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
            positionRepo.delete(position);
        } else {
            position.setQty(remainingQty);
            position.setUpdatedAt(Instant.now());
            positionRepo.save(position);
        }

        recordTrade(ticker, position.getName(), "SELL", qty, price, amount, realizedPnl, position.getCurrency());

        log.info("Sim SELL {} qty={} price={} pnl={}", ticker, qty, price, realizedPnl);
        return getSummary();
    }

    /**
     * Azzera la simulazione: rimuove posizioni e storico, ripristina la liquidità
     * al capitale iniziale (o al nuovo importo indicato).
     */
    @Transactional
    public SimSummaryDto reset(ResetRequest req) {
        positionRepo.deleteAll();
        tradeRepo.deleteAll();

        SimWallet wallet = loadWallet();
        BigDecimal startingBalance = req.startingBalance() != null
                ? BigDecimal.valueOf(req.startingBalance())
                : wallet.getStartingBalance();

        wallet.setStartingBalance(startingBalance);
        wallet.setCashBalance(startingBalance);
        wallet.setResetAt(Instant.now());
        walletRepo.save(wallet);

        log.info("Sim reset: capitale iniziale {}", startingBalance);
        return getSummary();
    }

    private void recordTrade(String ticker, String name, String side, BigDecimal qty,
                             BigDecimal price, BigDecimal amount, BigDecimal realizedPnl, String currency) {
        SimTrade trade = new SimTrade();
        trade.setId(UUID.randomUUID().toString());
        trade.setTicker(ticker);
        trade.setName(name);
        trade.setSide(side);
        trade.setQty(qty);
        trade.setPrice(price);
        trade.setAmount(amount);
        trade.setRealizedPnl(realizedPnl);
        trade.setCurrency(currency);
        tradeRepo.save(trade);
    }

    private Map<String, Double> fetchLivePrices(Set<String> tickers) {
        if (tickers.isEmpty()) return Map.of();
        try {
            List<QuoteDto> quotes = yahoo.fetchBatch(new ArrayList<>(tickers));
            return quotes.stream()
                    .filter(q -> q.price() != null)
                    .collect(Collectors.toMap(QuoteDto::ticker, QuoteDto::price));
        } catch (Exception e) {
            log.warn("Errore fetch prezzi live simulazione: {}", e.getMessage());
            return Map.of();
        }
    }
}
