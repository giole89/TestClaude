import Database from 'better-sqlite3'
import path from 'path'

const DB_PATH = process.env.DB_PATH || path.join(process.cwd(), 'finai.db')

const db = new Database(DB_PATH)
db.pragma('journal_mode = WAL')
db.pragma('foreign_keys = ON')

db.exec(`
  CREATE TABLE IF NOT EXISTS portfolio_items (
    id          TEXT PRIMARY KEY,
    ticker      TEXT NOT NULL,
    name        TEXT NOT NULL,
    qty         REAL NOT NULL,
    load_price  REAL NOT NULL,
    current_price REAL,
    currency    TEXT DEFAULT 'USD',
    created_at  INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS alerts (
    id          TEXT PRIMARY KEY,
    ticker      TEXT NOT NULL,
    type        TEXT NOT NULL CHECK(type IN ('above','below','change_up','change_down')),
    value       REAL NOT NULL,
    created_at  INTEGER NOT NULL,
    fired       INTEGER DEFAULT 0,
    fired_at    INTEGER,
    fired_price REAL
  );

  CREATE TABLE IF NOT EXISTS ipo_watchlist (
    id            TEXT PRIMARY KEY,
    ticker        TEXT,
    company_name  TEXT NOT NULL,
    expected_date TEXT,
    exchange      TEXT,
    sector        TEXT,
    lockup_days   INTEGER DEFAULT 180,
    ipo_price     REAL,
    notes         TEXT,
    created_at    INTEGER NOT NULL
  );
`)

// ─── Portfolio ────────────────────────────────────────────────────────────────

export interface DBPortfolioItem {
  id: string
  ticker: string
  name: string
  qty: number
  loadPrice: number
  currentPrice?: number
  currency: string
  createdAt: number
}

function rowToPortfolioItem(row: Record<string, unknown>): DBPortfolioItem {
  return {
    id: row.id as string,
    ticker: row.ticker as string,
    name: row.name as string,
    qty: row.qty as number,
    loadPrice: row.load_price as number,
    currentPrice: row.current_price as number | undefined,
    currency: row.currency as string,
    createdAt: row.created_at as number,
  }
}

export const portfolioDb = {
  getAll(): DBPortfolioItem[] {
    return (db.prepare('SELECT * FROM portfolio_items ORDER BY created_at ASC').all() as Record<string, unknown>[])
      .map(rowToPortfolioItem)
  },

  add(item: Omit<DBPortfolioItem, 'createdAt'>): DBPortfolioItem {
    const now = Date.now()
    db.prepare(`
      INSERT INTO portfolio_items (id, ticker, name, qty, load_price, current_price, currency, created_at)
      VALUES (@id, @ticker, @name, @qty, @loadPrice, @currentPrice, @currency, @createdAt)
    `).run({ ...item, currentPrice: item.currentPrice ?? null, createdAt: now })
    return { ...item, createdAt: now }
  },

  remove(id: string): void {
    db.prepare('DELETE FROM portfolio_items WHERE id = ?').run(id)
  },

  updatePrice(ticker: string, price: number, currency?: string): void {
    db.prepare(`
      UPDATE portfolio_items SET current_price = ?, currency = COALESCE(?, currency) WHERE ticker = ?
    `).run(price, currency ?? null, ticker)
  },

  updateName(ticker: string, name: string): void {
    db.prepare('UPDATE portfolio_items SET name = ? WHERE ticker = ?').run(name, ticker)
  },
}

// ─── Alerts ───────────────────────────────────────────────────────────────────

export interface DBAlert {
  id: string
  ticker: string
  type: 'above' | 'below' | 'change_up' | 'change_down'
  value: number
  createdAt: number
  fired: boolean
  firedAt?: number
  firedPrice?: number
}

function rowToAlert(row: Record<string, unknown>): DBAlert {
  return {
    id: row.id as string,
    ticker: row.ticker as string,
    type: row.type as DBAlert['type'],
    value: row.value as number,
    createdAt: row.created_at as number,
    fired: Boolean(row.fired),
    firedAt: row.fired_at as number | undefined,
    firedPrice: row.fired_price as number | undefined,
  }
}

export const alertsDb = {
  getActive(): DBAlert[] {
    return (db.prepare('SELECT * FROM alerts WHERE fired = 0 ORDER BY created_at ASC').all() as Record<string, unknown>[])
      .map(rowToAlert)
  },

  getHistory(): DBAlert[] {
    return (db.prepare('SELECT * FROM alerts WHERE fired = 1 ORDER BY fired_at DESC LIMIT 20').all() as Record<string, unknown>[])
      .map(rowToAlert)
  },

  add(alert: Omit<DBAlert, 'createdAt' | 'fired' | 'firedAt' | 'firedPrice'>): DBAlert {
    const now = Date.now()
    db.prepare(`
      INSERT INTO alerts (id, ticker, type, value, created_at, fired) VALUES (?, ?, ?, ?, ?, 0)
    `).run(alert.id, alert.ticker, alert.type, alert.value, now)
    return { ...alert, createdAt: now, fired: false }
  },

  remove(id: string): void {
    db.prepare('DELETE FROM alerts WHERE id = ?').run(id)
  },

  fire(id: string, price: number): void {
    db.prepare(`
      UPDATE alerts SET fired = 1, fired_at = ?, fired_price = ? WHERE id = ?
    `).run(Date.now(), price, id)
  },
}

// ─── IPO Watchlist ────────────────────────────────────────────────────────────

export interface DBIpoWatchlistItem {
  id: string
  ticker?: string
  companyName: string
  expectedDate?: string
  exchange?: string
  sector?: string
  lockupDays: number
  ipoPrice?: number
  notes?: string
  createdAt: number
}

function rowToIpo(row: Record<string, unknown>): DBIpoWatchlistItem {
  return {
    id: row.id as string,
    ticker: row.ticker as string | undefined,
    companyName: row.company_name as string,
    expectedDate: row.expected_date as string | undefined,
    exchange: row.exchange as string | undefined,
    sector: row.sector as string | undefined,
    lockupDays: row.lockup_days as number,
    ipoPrice: row.ipo_price as number | undefined,
    notes: row.notes as string | undefined,
    createdAt: row.created_at as number,
  }
}

export const ipoWatchlistDb = {
  getAll(): DBIpoWatchlistItem[] {
    return (db.prepare('SELECT * FROM ipo_watchlist ORDER BY created_at DESC').all() as Record<string, unknown>[])
      .map(rowToIpo)
  },

  add(item: Omit<DBIpoWatchlistItem, 'createdAt'>): DBIpoWatchlistItem {
    const now = Date.now()
    db.prepare(`
      INSERT INTO ipo_watchlist (id, ticker, company_name, expected_date, exchange, sector, lockup_days, ipo_price, notes, created_at)
      VALUES (@id, @ticker, @companyName, @expectedDate, @exchange, @sector, @lockupDays, @ipoPrice, @notes, @createdAt)
    `).run({
      id: item.id,
      ticker: item.ticker ?? null,
      companyName: item.companyName,
      expectedDate: item.expectedDate ?? null,
      exchange: item.exchange ?? null,
      sector: item.sector ?? null,
      lockupDays: item.lockupDays,
      ipoPrice: item.ipoPrice ?? null,
      notes: item.notes ?? null,
      createdAt: now,
    })
    return { ...item, createdAt: now }
  },

  remove(id: string): void {
    db.prepare('DELETE FROM ipo_watchlist WHERE id = ?').run(id)
  },

  update(id: string, fields: Partial<Omit<DBIpoWatchlistItem, 'id' | 'createdAt'>>): void {
    const updates: string[] = []
    const values: unknown[] = []
    if (fields.ticker !== undefined) { updates.push('ticker = ?'); values.push(fields.ticker) }
    if (fields.companyName !== undefined) { updates.push('company_name = ?'); values.push(fields.companyName) }
    if (fields.expectedDate !== undefined) { updates.push('expected_date = ?'); values.push(fields.expectedDate) }
    if (fields.exchange !== undefined) { updates.push('exchange = ?'); values.push(fields.exchange) }
    if (fields.sector !== undefined) { updates.push('sector = ?'); values.push(fields.sector) }
    if (fields.lockupDays !== undefined) { updates.push('lockup_days = ?'); values.push(fields.lockupDays) }
    if (fields.ipoPrice !== undefined) { updates.push('ipo_price = ?'); values.push(fields.ipoPrice) }
    if (fields.notes !== undefined) { updates.push('notes = ?'); values.push(fields.notes) }
    if (updates.length === 0) return
    values.push(id)
    db.prepare(`UPDATE ipo_watchlist SET ${updates.join(', ')} WHERE id = ?`).run(...values)
  },
}

export default db
