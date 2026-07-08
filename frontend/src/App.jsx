import { useEffect, useState } from 'react'
import './App.css'

// ── Helpers ────────────────────────────────────────────────────────────
function typeColor(type) {
  if (!type) return '#1a3a5c'
  const t = type.toLowerCase()
  if (t.includes('spell'))   return '#1d7a4b'
  if (t.includes('trap'))    return '#7a1d6b'
  if (t.includes('fusion'))  return '#8b3db5'
  if (t.includes('synchro')) return '#4a4a5a'
  if (t.includes('xyz'))     return '#1a1a2a'
  if (t.includes('link'))    return '#0b3d8c'
  if (t.includes('ritual'))  return '#2a4fa0'
  if (t.includes('pendulum')) return '#2a7a6a'
  if (t.includes('normal'))  return '#8b7510'
  return '#8b4a00'
}

function Stars({ count }) {
  if (!count) return null
  return <span className="level-stars">{'★'.repeat(Math.min(count, 12))}</span>
}

// ── WikiRow ────────────────────────────────────────────────────────────
function WikiRow({ label, children }) {
  if (children === null || children === undefined || children === '') return null
  return (
      <div className="wiki-row">
        <span className="wiki-row-label">{label}</span>
        <span className="wiki-row-value">{children}</span>
      </div>
  )
}

// ── CardBadge ──────────────────────────────────────────────────────────
function Badge({ label, color }) {
  return <span className="badge" style={{ background: color }}>{label}</span>
}

// ── CardDetail ─────────────────────────────────────────────────────────
function CardDetail({ card, oncePerTurn, extender }) {
  const [combos, setCombos] = useState([])
  const [combosLoading, setCombosLoading] = useState(false)
  const color = typeColor(card.type)
  const t = (card.type || '').toLowerCase()
  const isMonster  = t.includes('monster')
  const isLink     = t.includes('link')
  const isPendulum = t.includes('pendulum')
  const hasImage = Boolean(card.cardImageUrl)

  const hasBadges = oncePerTurn || extender === 'summon extender' || extender === 'add extender' || card.staple === true || card.weight > 0

  useEffect(() => {
    let active = true

    async function loadCombos() {
      setCombos([])
      setCombosLoading(true)
      try {
        const res = await fetch(`${API}/yugioh/card/combos?name=${encodeURIComponent(card.name)}`)
        if (!res.ok) throw new Error('Failed to load combos')
        const data = await res.json()
        if (active) setCombos(Array.isArray(data) ? data : [])
      } catch {
        if (active) setCombos([])
      } finally {
        if (active) setCombosLoading(false)
      }
    }

    loadCombos()

    return () => {
      active = false
    }
  }, [card.name])

  return (
      <div className="wiki-card">
        {/* Top banner with card name */}
        <div className="wiki-banner" style={{ background: color }}>
          <h2 className="wiki-name">{card.name}</h2>
        </div>

        <div className="wiki-body">
          {/* Left column: card image */}
          <div className="wiki-image-col">
            <div className="wiki-image-frame" style={{ borderColor: color }}>
              {hasImage ? (
                <img
                    src={card.cardImageUrl}
                    alt={card.name}
                    className="wiki-image"
                />
              ) : (
                <div className="wiki-image-placeholder">
                  Image unavailable
                </div>
              )}
            </div>
          </div>

          {/* Right column: structured info rows */}
          <div className="wiki-info-col">
            <div className="wiki-info-panel">
              <WikiRow label="Type">{card.type}</WikiRow>

              {isMonster && (
                  <>
                    {card.attribute && <WikiRow label="Attribute">{card.attribute}</WikiRow>}
                    {card.race && <WikiRow label="Race">{card.race}</WikiRow>}
                    {!isLink && card.level != null && (
                        <WikiRow label="Level"><Stars count={card.level} /></WikiRow>
                    )}
                    {isPendulum && card.scale != null && (
                        <WikiRow label="Pendulum Scale">{card.scale}</WikiRow>
                    )}
                    <WikiRow label="ATK / DEF">
                      {card.atk ?? '?'} {!isLink && <>/ {card.def ?? '?'}</>}
                    </WikiRow>
                    {isLink && card.linkvalue != null && (
                        <WikiRow label="Link Rating">{card.linkvalue}</WikiRow>
                    )}
                    {isLink && card.linkmarkers?.length > 0 && (
                        <WikiRow label="Link Arrows">{card.linkmarkers.join('  ')}</WikiRow>
                    )}
                  </>
              )}

              {card.archetype && (
                  <WikiRow label="Archetype">{card.archetype}</WikiRow>
              )}

              {hasBadges && (
                  <div className="wiki-row">
                    <span className="wiki-row-label">Tags</span>
                    <span className="wiki-row-value wiki-badges">
                  {oncePerTurn && <Badge label="Once Per Turn" color="#7a1d6b" />}
                      {extender === 'summon extender' && <Badge label="Summon Extender" color="#1d6b3a" />}
                      {extender === 'add extender'    && <Badge label="Add Extender"    color="#1a4f8b" />}
                      {card.staple === true           && <Badge label="Staple"          color="#b5891e" />}
                      {card.weight > 0                && <Badge label={`Weight ${card.weight}`} color="#334" />}
                </span>
                  </div>
              )}
            </div>

            {/* Card Text section */}
            <div className="wiki-section-header" style={{ background: color }}>
              Card Text
            </div>
            <div className="wiki-card-text">{card.description}</div>

            {/* Combos section */}
            <div className="wiki-section-header" style={{ background: color }}>
              Possible Combos
            </div>
            <div className="wiki-combo-panel">
              {combosLoading ? (
                <div className="wiki-combo-loading">Loading combo ideas...</div>
              ) : combos && combos.length > 0 ? (
                <ul className="combo-list">
                  {combos.map((combo, index) => (
                    <li key={index}>{combo}</li>
                  ))}
                </ul>
              ) : (
                <div className="wiki-combo-empty">
                  No combo ideas found for this card.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
  )
}

// ── CardListItem ───────────────────────────────────────────────────────
function CardListItem({ card, selected, onClick }) {
  const color = typeColor(card.type)
  return (
      <div
          className={`card-list-item${selected ? ' selected' : ''}`}
          style={{ borderLeftColor: color }}
          onClick={onClick}
      >
        <div className="card-list-name">{card.name}</div>
        <div className="card-list-type" style={{ color }}>{card.type}</div>
      </div>
  )
}

const API = 'http://localhost:8080'

// ── App ────────────────────────────────────────────────────────────────
export default function App() {
  const [query, setQuery]             = useState('')
  const [mode, setMode]               = useState('search')   // 'search' | 'exact'
  const [cards, setCards]             = useState([])
  const [selected, setSelected]       = useState(null)
  const [oncePerTurn, setOncePerTurn] = useState(null)
  const [extender, setExtender]       = useState(null)
  const [loading, setLoading]         = useState(false)
  const [error, setError]             = useState(null)

  async function fetchCardExtras(card) {
    setOncePerTurn(null)
    setExtender(null)
    try {
      const [optRes, extRes] = await Promise.all([
        fetch(`${API}/yugioh/card/onceprturn?name=${encodeURIComponent(card.name)}`),
        fetch(`${API}/yugioh/card/pattern?name=${encodeURIComponent(card.name)}`),
      ])
      if (optRes.ok) setOncePerTurn(await optRes.json())
      if (extRes.ok) setExtender(await extRes.json())
    } catch { /* extras are non-critical */ }
  }

  async function selectCard(card) {
    setSelected(card)
    await fetchCardExtras(card)
  }

  async function handleSearch(e) {
    e.preventDefault()
    if (!query.trim()) return
    setLoading(true)
    setError(null)
    setCards([])
    setSelected(null)
    setOncePerTurn(null)
    setExtender(null)

    try {
      if (mode === 'exact') {
        const res = await fetch(`${API}/yugioh/card?name=${encodeURIComponent(query)}`)
        if (!res.ok) throw new Error('Card not found')
        const card = await res.json()
        setCards([card])
        await selectCard(card)
      } else {
        const res = await fetch(`${API}/yugioh/card/substring?name=${encodeURIComponent(query)}`)
        if (!res.ok) throw new Error('No cards found')
        const list = await res.json()
        setCards(list)
        if (list.length === 1) await selectCard(list[0])
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const showList = cards.length > 1

  return (
      <div className="app">
        <header className="app-header">
          <div className="header-title">
            <span className="title-accent">Yu-Gi-Oh!</span> Card Search
          </div>
        </header>

        <main className="app-main">
          {/* Search bar */}
          <form className="search-form" onSubmit={handleSearch}>
            <div className="search-row">
              <input
                  className="search-input"
                  type="text"
                  placeholder="Enter a card name…"
                  value={query}
                  onChange={e => setQuery(e.target.value)}
              />
              <button className="search-btn" type="submit" disabled={loading}>
                {loading ? '…' : 'Search'}
              </button>
            </div>
            <div className="search-modes">
              <button
                  type="button"
                  className={`mode-pill${mode === 'search' ? ' active' : ''}`}
                  onClick={() => setMode('search')}
              >
                By Name
              </button>
              <button
                  type="button"
                  className={`mode-pill${mode === 'exact' ? ' active' : ''}`}
                  onClick={() => setMode('exact')}
              >
                Exact Match
              </button>
            </div>
          </form>

          {/* Error */}
          {error && <div className="error-msg">{error}</div>}

          {/* Spinner */}
          {loading && (
              <div className="loading">
                <div className="spinner" />
              </div>
          )}

          {/* Results */}
          {!loading && (
              <div className="results-layout">
                {showList && (
                    <div className="card-list-panel">
                      <div className="panel-label">{cards.length} results</div>
                      {cards.map(c => (
                          <CardListItem
                              key={c.id}
                              card={c}
                              selected={selected?.id === c.id}
                              onClick={() => selectCard(c)}
                          />
                      ))}
                    </div>
                )}

                <div className="card-detail-panel">
                  {selected ? (
                      <CardDetail
                          card={selected}
                          oncePerTurn={oncePerTurn}
                          extender={extender}
                      />
                  ) : !error && cards.length === 0 && (
                      <div className="empty-state">
                        <div className="empty-state-icon">⚔</div>
                        <p>Search for a card to begin</p>
                      </div>
                  )}
                </div>
              </div>
          )}
        </main>
      </div>
  )
}
