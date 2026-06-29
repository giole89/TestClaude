package com.finai.service;

import com.finai.domain.entity.InvestorProfile;
import com.finai.dto.finance.AllocationDto;
import com.finai.dto.finance.RecommendationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Motore di consiglio investimenti basato su regole: incrocia l'obiettivo
 * dichiarato dall'utente (a cosa servono i soldi) con l'orizzonte temporale
 * dichiarato (tra quanto tempo li userà) per suggerire un'allocazione
 * indicativa tra liquidità, obbligazionario e azionario.
 *
 * <p>Non è consulenza finanziaria personalizzata: è un'indicazione generica
 * basata su principi di pianificazione finanziaria comunemente accettati
 * (orizzonte breve → priorità alla sicurezza del capitale; orizzonte lungo →
 * priorità alla crescita).</p>
 */
@Service
public class InvestmentAdvisorService {

    private record Profile(String label, AllocationDto allocation, List<String> instruments) {}

    private static final Map<String, String> GOAL_LABELS = Map.of(
            "EMERGENCY", "costituire un fondo di emergenza",
            "MAJOR_PURCHASE", "un grande acquisto futuro (casa, auto, ecc.)",
            "RETIREMENT", "integrare la pensione / obiettivo di lungo termine",
            "GROWTH", "far crescere il capitale nel tempo",
            "OTHER", "un obiettivo personale"
    );

    private static final Map<String, String> HORIZON_LABELS = Map.of(
            "UNDER_1Y", "meno di 1 anno",
            "Y1_3", "1-3 anni",
            "Y3_5", "3-5 anni",
            "Y5_10", "5-10 anni",
            "OVER_10Y", "oltre 10 anni"
    );

    private static final Profile EMERGENCY_PROFILE = new Profile(
            "Liquidità e sicurezza",
            new AllocationDto(0, 10, 90),
            List.of("Conto deposito svincolabile", "Conto corrente remunerato", "BOT/BTP a brevissima scadenza")
    );

    private static final Map<String, Profile> BY_HORIZON = Map.of(
            "UNDER_1Y", new Profile("Conservativo a breve termine",
                    new AllocationDto(0, 20, 80),
                    List.of("Conto deposito", "Fondi monetari", "BOT a breve termine")),
            "Y1_3", new Profile("Cauto",
                    new AllocationDto(20, 40, 40),
                    List.of("ETF obbligazionari a breve/media duration", "Fondi obbligazionari", "Piccola quota di ETF azionari globali")),
            "Y3_5", new Profile("Bilanciato",
                    new AllocationDto(40, 45, 15),
                    List.of("ETF bilanciati (60/40 o simili)", "Mix di ETF obbligazionari e azionari", "Fondi multi-asset")),
            "Y5_10", new Profile("Dinamico",
                    new AllocationDto(70, 25, 5),
                    List.of("ETF azionari globali ad accumulo", "Piano di accumulo (PAC)", "Quota residua obbligazionaria")),
            "OVER_10Y", new Profile("Aggressivo / Crescita",
                    new AllocationDto(90, 10, 0),
                    List.of("ETF azionari globali a basso costo", "PAC di lungo periodo", "Eventuale quota satellite tematica/settoriale"))
    );

    public RecommendationDto recommend(InvestorProfile p) {
        String goal = p.getGoal() != null ? p.getGoal().toUpperCase(Locale.ROOT) : "OTHER";
        String horizon = p.getHorizon() != null ? p.getHorizon().toUpperCase(Locale.ROOT) : "Y3_5";

        Profile profile = "EMERGENCY".equals(goal)
                ? EMERGENCY_PROFILE
                : BY_HORIZON.getOrDefault(horizon, BY_HORIZON.get("Y3_5"));

        String goalLabel = GOAL_LABELS.getOrDefault(goal, GOAL_LABELS.get("OTHER"));
        String horizonLabel = HORIZON_LABELS.getOrDefault(horizon, horizon);

        String summary = String.format(
                "Hai indicato che questi soldi serviranno per %s e che prevedi di averne bisogno tra %s. " +
                "Per questo profilo ti consigliamo un'allocazione orientata a \"%s\": %.0f%% azionario, %.0f%% obbligazionario, %.0f%% liquidità. " +
                "Questo è un suggerimento generico basato su principi di pianificazione finanziaria, non una consulenza personalizzata: valuta sempre la tua situazione specifica prima di investire.",
                goalLabel, horizonLabel, profile.label(),
                profile.allocation().equityPct(), profile.allocation().bondPct(), profile.allocation().liquidityPct()
        );

        return new RecommendationDto(profile.label(), profile.allocation(), summary, profile.instruments(), goal, horizon,
                null, List.of(), null, null, pacNote());
    }

    /** Punto 5: la quota investibile calcolata dal budget è un surplus mensile ricorrente, non una somma unica: ha più senso investirla gradualmente. */
    private String pacNote() {
        return "La quota investibile stimata è un risparmio che si ripete ogni mese, non una somma unica disponibile oggi: "
                + "per questo motivo, invece di provare a investirla tutta in un'unica soluzione (lump sum), valuta un Piano di "
                + "Accumulo Capitale (PAC), cioè un versamento automatico di importo fisso ogni mese sugli stessi strumenti. "
                + "Il PAC riduce il rischio di investire tutto in un momento sfavorevole del mercato (market timing), mediando nel "
                + "tempo il prezzo di acquisto: è coerente con la natura ricorrente di questo risparmio.";
    }
}
