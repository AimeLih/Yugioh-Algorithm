import { useRef, useState } from 'react'
import './App.css'

function typeColor(type) {
  if (!type) return '#1a3a5c'
  const t = type.toLowerCase()
  if (t.includes('spell')) return '#1d7a4b'
  if (t.includes('trap')) return '#7a1d6b'
  if (t.includes('fusion')) return '#8b3db5'
  if (t.includes('synchro')) return '#4a4a5a'
  if (t.includes('xyz')) return '#1a1a2a'
  if (t.includes('link')) return '#0b3d8c'
  if (t.includes('ritual')) return '#2a4fa0'
  if (t.includes('pendulum')) return '#2a7a6a'
  if (t.includes('normal')) return '#8b7510'
  return '#8b4a00'
}

function Stars({ count }) {
  if (!count) return null
  return <span className="level-stars">{'★'.repeat(Math.min(count, 12))}</span>
}

function WikiRow({ label, children }) {
  if (children === null || children === undefined || children === '') return null
  return (
      <div className="wiki-row">
        <span className="wiki-row-label">{label}</span>
        <span className="wiki-row-value">{children}</span>
      </div>
  )
}

function Badge({ label, color }) {
  return <span className="badge" style={{ background: color }}>{label}</span>
}

function canActivateFromZone(entry, zone) {
  const text = (entry.card.description || '').toLowerCase()
  const reason = entry.moveReason || ''
  if (zone === 'graveyard') {
    return text.split(/(?<=\.)\s+|\n+/).some(effect => {
      if (/destroyed(?: by battle| by card effect)? and sent to (?:the |your )?(?:gy|graveyard)/.test(effect)) {
        return reason === 'destroy'
      }
      if (/if this card is discarded/.test(effect)) {
        return reason === 'discard'
      }
      if (/if this card is tributed/.test(effect)) {
        return reason === 'tribute'
      }
      if (/used as fusion material/.test(effect)) {
        return reason === 'fusion-material'
      }
      return /(?:sent to|in|from|while .* in) (?:the |your )?(?:gy|graveyard)/.test(effect)
          || /banish (?:this card|it) from (?:the |your )?(?:gy|graveyard)/.test(effect)
    })
  }
  if (zone === 'banished') {
    return /(?:if|when|while) (?:this card|it) is banished/.test(text)
        || /(?:from|among) your banished/.test(text)
  }
  return false
}

function SimulatedZones({ zones, activatedZoneEffects, onActivateZoneCard }) {
  const zoneDefinitions = [
    { key: 'field', label: 'Field' },
    { key: 'graveyard', label: 'Graveyard' },
    { key: 'banished', label: 'Banished' },
    { key: 'extraDeck', label: 'Face-up Extra Deck' },
  ]

  return (
      <div className="simulated-zones">
        {zoneDefinitions.map(zone => (
            <section key={zone.key} className={`sim-zone ${zone.key}`}>
              <div className="sim-zone-heading">
                <span>{zone.label}</span>
                <span className="sim-zone-count">{zones[zone.key].length}</span>
              </div>
              <div className="sim-zone-cards">
                {zones[zone.key].length === 0 ? (
                    <div className="sim-zone-empty">No cards</div>
                ) : zones[zone.key].map(entry => {
                  const liveEffect = canActivateFromZone(entry, zone.key)
                  const used = activatedZoneEffects.includes(entry.instanceId)
                  return (
                      <button
                          key={entry.instanceId}
                          type="button"
                          className={`sim-zone-card${liveEffect ? ' live' : ''}${used ? ' used' : ''}`}
                          onClick={() => liveEffect && !used && onActivateZoneCard(entry, zone.key)}
                          disabled={!liveEffect || used}
                          title={liveEffect
                            ? used
                              ? 'This zone effect was already used in this route'
                              : `Activate ${entry.card.name} from the ${zone.label}`
                            : 'This card has no effect that activates from this zone'}
                      >
                        <span className="sim-zone-card-name">{entry.card.name}</span>
                        {liveEffect && (
                            <span className="sim-zone-effect-state">
                              {used ? 'Effect used' : 'Effect available'}
                            </span>
                        )}
                      </button>
                  )
                })}
              </div>
            </section>
        ))}
      </div>
  )
}

function MaterialPicker({
  pendingOption,
  materialPlan,
  loading,
  error,
  selections,
  search,
  onSearchChange,
  onAddMaterial,
  onRemoveMaterial,
  onConfirm,
  onCancel,
  zones,
}) {
  if (!pendingOption) return null

  const complete = materialPlan?.slots.every(
      (slot, index) => (selections[index]?.length ?? 0) === slot.count,
  )
  const availableFrom = (materialPlan?.availableFrom || '').toLowerCase()
  const hasOpenCardPool = availableFrom.includes('hand')
      || (availableFrom.includes('deck') && !availableFrom.includes('extra deck'))

  function simulatedLocations(card) {
    return ['field', 'graveyard', 'banished', 'extraDeck'].filter(zone =>
      availableFrom.includes(zone === 'extraDeck' ? 'extra deck' : zone)
        && zones[zone].some(entry => entry.card.id === card.id))
  }

  function availableCopies(card) {
    if (hasOpenCardPool) return 3
    return simulatedLocations(card).reduce(
        (total, zone) => total + zones[zone].filter(entry => entry.card.id === card.id).length,
        0,
    )
  }

  function selectedCopies(card) {
    return Object.values(selections)
        .flat()
        .filter(selectedCard => selectedCard.id === card.id)
        .length
  }

  return (
      <div className="material-picker-backdrop" role="presentation">
        <section className="material-picker" role="dialog" aria-modal="true" aria-labelledby="material-picker-title">
          <div className="material-picker-header">
            <div>
              <div className="material-picker-kicker">Pay Route Cost</div>
              <h3 id="material-picker-title">{pendingOption.card.name}</h3>
            </div>
            <button type="button" className="material-picker-close" onClick={onCancel}>Close</button>
          </div>

          {loading ? (
              <div className="material-picker-status">Finding legal Fusion Materials...</div>
          ) : error ? (
              <div className="material-picker-error">{error}</div>
          ) : materialPlan && (
              <>
                <div className="material-plan-summary">
                  <span>{materialPlan.action}</span>
                  <span>From: {materialPlan.availableFrom}</span>
                  <span>Send to: {materialPlan.destination}</span>
                </div>
                {materialPlan.restrictions?.map(restriction => (
                    <div key={restriction} className="material-plan-restriction">{restriction}</div>
                ))}
                <input
                    className="material-search"
                    value={search}
                    onChange={event => onSearchChange(event.target.value)}
                    placeholder="Filter legal materials..."
                />
                <div className="material-slots">
                  {materialPlan.slots.map((slot, slotIndex) => {
                    const selectedCards = selections[slotIndex] ?? []
                    const matchingCards = slot.eligibleCards.filter(card =>
                      card.name.toLowerCase().includes(search.toLowerCase())
                        && availableCopies(card) > 0)
                    return (
                        <section key={`${slot.requirement}-${slotIndex}`} className="material-slot">
                          <div className="material-slot-heading">
                            <span>{slot.count}x {slot.requirement}</span>
                            <span>{selectedCards.length}/{slot.count} selected</span>
                          </div>
                          {selectedCards.length > 0 && (
                              <div className="selected-materials">
                                {selectedCards.map((card, selectedIndex) => (
                                    <button
                                        key={`${card.id ?? card.name}-${selectedIndex}`}
                                        type="button"
                                        onClick={() => onRemoveMaterial(slotIndex, selectedIndex)}
                                    >
                                      {card.name} ×
                                    </button>
                                ))}
                              </div>
                          )}
                          <div className="material-card-list">
                            {matchingCards.length === 0 ? (
                                <div className="material-no-results">No legal cards match this filter.</div>
                            ) : matchingCards.map(card => (
                                <button
                                    key={card.id ?? card.name}
                                    type="button"
                                    className="material-card-choice"
                                    onClick={() => onAddMaterial(slotIndex, card)}
                                    disabled={selectedCards.length >= slot.count
                                      || selectedCopies(card) >= availableCopies(card)}
                                >
                                  <span>{card.name}</span>
                                  <span>
                                    {simulatedLocations(card).length > 0
                                      ? `In ${simulatedLocations(card).join(' / ')}`
                                      : materialPlan.availableFrom}
                                  </span>
                                </button>
                            ))}
                          </div>
                        </section>
                    )
                  })}
                </div>
                <div className="material-picker-actions">
                  <button type="button" className="material-cancel-btn" onClick={onCancel}>Cancel</button>
                  <button type="button" className="material-confirm-btn" onClick={onConfirm} disabled={!complete}>
                    Pay Cost &amp; Continue
                  </button>
                </div>
              </>
          )}
        </section>
      </div>
  )
}

function ComboOptionCard({ option, comboPath, onChooseCard, timingLockReason }) {
  const alreadyUsed = option.oncePerTurn && comboPath.some(
      step => step.name?.toLowerCase() === option.card.name?.toLowerCase(),
  )
  const unavailable = alreadyUsed || Boolean(timingLockReason)

  return (
      <button
          type="button"
          className={`combo-option-card${unavailable ? ' unavailable' : ''}`}
          onClick={() => onChooseCard(option)}
          disabled={unavailable}
          title={alreadyUsed
            ? 'This once-per-turn card was already used in this route'
            : timingLockReason || undefined}
      >
        <div className="combo-option-label">{option.label}</div>
        <div className="combo-option-name">{option.card.name}</div>
        <div className="combo-option-type">{option.card.type}</div>
        {alreadyUsed && <div className="combo-option-used">Already used this turn</div>}
        {timingLockReason && <div className="combo-option-timing-lock">{timingLockReason}</div>}
        <div className="combo-option-meta">
          {option.timing && (
              <span className={`combo-meta-chip${option.timing === 'Immediate' ? '' : ' delayed'}`}>
                {option.timing}
              </span>
          )}
          {option.sourceZone && (
              <span className="combo-meta-chip">From: {option.sourceZone}</span>
          )}
          {option.destination && option.destination !== 'Varies' && (
              <span className="combo-meta-chip">To: {option.destination}</span>
          )}
        </div>
        {option.cost && (
            <div className="combo-option-cost">
              <span className="combo-option-cost-label">Cost</span>
              {option.cost}
            </div>
        )}
        <div className="combo-option-reason">{option.reason}</div>
      </button>
  )
}

function CardDetail({
  card,
  oncePerTurn,
  extender,
  comboPath,
  comboOptions,
  comboLoading,
  onChooseCard,
  onBackCombo,
  zones,
  activatedZoneEffects,
  onActivateZoneCard,
  currentTurn,
  phase,
  activeCardTurn,
  onAdvancePhase,
}) {
  const color = typeColor(card.type)
  const t = (card.type || '').toLowerCase()
  const isMonster = t.includes('monster')
  const isLink = t.includes('link')
  const isPendulum = t.includes('pendulum')
  const hasImage = Boolean(card.cardImageUrl)
  const hasBadges = oncePerTurn || extender === 'summon extender' || extender === 'add extender' || card.staple === true || card.weight > 0
  const continuingOptions = comboOptions?.filter(option => option.label !== 'ender') ?? []
  const enderOptions = comboOptions?.filter(option => option.label === 'ender') ?? []

  function timingLockReason(option) {
    const timing = option.timing || ''
    const destination = (option.destination || '').toLowerCase()
    const targetType = (option.card.type || '').toLowerCase()
    const mainMonsterCount = zones.field.filter(entry =>
      (entry.card.type || '').toLowerCase().includes('monster')
        && !/(fusion|synchro|xyz|link)/.test((entry.card.type || '').toLowerCase())).length
    const spellTrapCount = zones.field.filter(entry =>
      /(spell|trap)/.test((entry.card.type || '').toLowerCase())).length

    if (destination.includes('monster zone')
        && targetType.includes('monster')
        && !targetType.includes('fusion')
        && mainMonsterCount >= 5) {
      return 'All 5 Main Monster Zones are occupied'
    }
    if (destination.includes('spell & trap zone') && spellTrapCount >= 5) {
      return 'All 5 Spell & Trap Zones are occupied'
    }
    if (timing.includes('After being Set') && currentTurn <= activeCardTurn) {
      return 'Trap was Set this turn'
    }
    if (timing.includes('Next turn') && currentTurn <= activeCardTurn) {
      return 'Available next turn'
    }
    if (timing.includes('End Phase') && phase !== 'End Phase') {
      return 'Requires the End Phase'
    }
    if (timing.includes('Standby Phase') && phase !== 'Standby Phase') {
      return 'Requires the Standby Phase'
    }
    if (timing.includes('Battle Phase') && phase !== 'Battle Phase') {
      return 'Requires the Battle Phase'
    }
    return null
  }

  return (
      <div className="wiki-card">
        <div className="wiki-banner" style={{ background: color }}>
          <h2 className="wiki-name">{card.name}</h2>
        </div>

        <div className="wiki-body">
          <div className="wiki-image-col">
            <div className="wiki-image-frame" style={{ borderColor: color }}>
              {hasImage ? (
                <img
                    src={card.cardImageUrl}
                    alt={card.name}
                    className="wiki-image"
                />
              ) : (
                <div className="wiki-image-placeholder">Image unavailable</div>
              )}
            </div>
          </div>

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
                      {extender === 'add extender' && <Badge label="Add Extender" color="#1a4f8b" />}
                      {card.staple === true && <Badge label="Staple" color="#b5891e" />}
                      {card.weight > 0 && <Badge label={`Weight ${card.weight}`} color="#334" />}
                    </span>
                  </div>
              )}
            </div>

            <div className="wiki-section-header" style={{ background: color }}>
              Card Text
            </div>
            <div className="wiki-card-text">{card.description}</div>

            <div className="wiki-section-header" style={{ background: color }}>
              Build Combo
            </div>
            <div className="wiki-combo-panel">
              <div className="combo-builder-top">
                <div className="combo-path" aria-label="Combo path">
                  {comboPath.map((step, index) => (
                      <span key={`${step.id ?? step.name}-${index}`} className="combo-path-step">
                        <span className={`combo-path-chip${index === comboPath.length - 1 ? ' active' : ''}`}>
                          <span className="combo-path-index">{index + 1}</span>
                          <span className="combo-path-name">{step.name}</span>
                        </span>
                        {index < comboPath.length - 1 && <span className="combo-path-arrow">→</span>}
                      </span>
                  ))}
                </div>
                <button
                    type="button"
                    className="combo-back-btn"
                    onClick={onBackCombo}
                    disabled={comboPath.length <= 1}
                >
                  Back
                </button>
              </div>
              <div className="simulator-clock">
                <div>
                  <span>Turn {currentTurn}</span>
                  <strong>{phase}</strong>
                </div>
                <button type="button" onClick={onAdvancePhase}>
                  Advance Phase
                </button>
              </div>

              {comboLoading ? (
                  <div className="wiki-combo-loading">Loading next combo options...</div>
              ) : comboOptions && comboOptions.length > 0 ? (
                  <div className="combo-option-groups">
                    {continuingOptions.length > 0 && (
                        <section className="combo-option-group">
                          <div className="combo-option-group-title">Continue Combo</div>
                          <div className="combo-option-grid">
                            {continuingOptions.map(option => (
                                <ComboOptionCard
                                    key={option.card.id ?? option.card.name}
                                    option={option}
                                    comboPath={comboPath}
                                    onChooseCard={onChooseCard}
                                    timingLockReason={timingLockReason(option)}
                                />
                            ))}
                          </div>
                        </section>
                    )}
                    {enderOptions.length > 0 && (
                        <section className="combo-option-group enders">
                          <div className="combo-option-group-title">Combo Enders</div>
                          <div className="combo-option-grid">
                            {enderOptions.map(option => (
                                <ComboOptionCard
                                    key={option.card.id ?? option.card.name}
                                    option={option}
                                    comboPath={comboPath}
                                    onChooseCard={onChooseCard}
                                    timingLockReason={timingLockReason(option)}
                                />
                            ))}
                          </div>
                        </section>
                    )}
                  </div>
              ) : (
                  <div className="wiki-combo-empty">
                    This combo can end here. Go back to explore another branch.
                  </div>
              )}
            </div>

            <div className="wiki-section-header zone-section-header">
              Simulated Zones
            </div>
            <SimulatedZones
                zones={zones}
                activatedZoneEffects={activatedZoneEffects}
                onActivateZoneCard={onActivateZoneCard}
            />
          </div>
        </div>
      </div>
  )
}

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

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export default function App() {
  const comboRequestId = useRef(0)
  const zoneInstanceId = useRef(0)
  const [query, setQuery] = useState('')
  const [mode, setMode] = useState('search')
  const [cards, setCards] = useState([])
  const [selected, setSelected] = useState(null)
  const [oncePerTurn, setOncePerTurn] = useState(null)
  const [extender, setExtender] = useState(null)
  const [comboPath, setComboPath] = useState([])
  const [comboOptions, setComboOptions] = useState([])
  const [zones, setZones] = useState({ field: [], graveyard: [], banished: [], extraDeck: [] })
  const [comboHistory, setComboHistory] = useState([])
  const [activatedZoneEffects, setActivatedZoneEffects] = useState([])
  const [activeZoneContext, setActiveZoneContext] = useState(null)
  const [currentTurn, setCurrentTurn] = useState(1)
  const [phase, setPhase] = useState('Main Phase')
  const [activeCardTurn, setActiveCardTurn] = useState(1)
  const [pendingOption, setPendingOption] = useState(null)
  const [materialPlan, setMaterialPlan] = useState(null)
  const [materialSelections, setMaterialSelections] = useState({})
  const [materialSearch, setMaterialSearch] = useState('')
  const [materialLoading, setMaterialLoading] = useState(false)
  const [materialError, setMaterialError] = useState(null)
  const [comboLoading, setComboLoading] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  function zoneEntry(card, moveReason = 'placed') {
    return { instanceId: `zone-${++zoneInstanceId.current}`, card, moveReason }
  }

  function snapshotComboState() {
    return {
      comboPath,
      zones,
      activatedZoneEffects,
      activeZoneContext,
      currentTurn,
      phase,
      activeCardTurn,
    }
  }

  async function fetchCardExtras(card) {
    setOncePerTurn(null)
    setExtender(null)
    try {
      const [optRes, extRes] = await Promise.all([
        fetch(`${API}/yugioh/card/onceprturn?name=${encodeURIComponent(card.name)}`),
        fetch(`${API}/yugioh/card/pattern?name=${encodeURIComponent(card.name)}`),
      ])
      if (optRes.ok) {
        const oncePerTurnText = (await optRes.text()).trim()
        setOncePerTurn(oncePerTurnText.toLowerCase() === 'not once per turn' ? null : oncePerTurnText)
      }
      if (extRes.ok) setExtender((await extRes.text()).trim())
    } catch {
      // extras are non-critical
    }
  }

  async function fetchComboOptions(card, zoneContext = null) {
    const requestId = ++comboRequestId.current
    setComboLoading(true)
    setComboOptions([])
    try {
      const zoneQuery = zoneContext ? `&zone=${encodeURIComponent(zoneContext)}` : ''
      const res = await fetch(`${API}/yugioh/card/combos?name=${encodeURIComponent(card.name)}${zoneQuery}`)
      if (!res.ok) throw new Error('Failed to load combo options')
      const data = await res.json()
      if (comboRequestId.current === requestId) {
        setComboOptions(Array.isArray(data) ? data : [])
      }
    } catch {
      if (comboRequestId.current === requestId) {
        setComboOptions([])
      }
    } finally {
      if (comboRequestId.current === requestId) {
        setComboLoading(false)
      }
    }
  }

  async function openRootCard(card) {
    setSelected(card)
    setComboPath([card])
    setZones({ field: [zoneEntry(card)], graveyard: [], banished: [], extraDeck: [] })
    setComboHistory([])
    setActivatedZoneEffects([])
    setActiveZoneContext(null)
    setCurrentTurn(1)
    setPhase('Main Phase')
    setActiveCardTurn(1)
    closeMaterialPicker()
    await fetchCardExtras(card)
    await fetchComboOptions(card)
  }

  async function finishComboChoice(
      option,
      paidMaterials = [],
      materialDestination = null,
      paymentReason = 'send',
  ) {
    const card = option.card
    setComboHistory(prev => [...prev, snapshotComboState()])
    setZones(prev => {
      const next = {
        field: [...prev.field],
        graveyard: [...prev.graveyard],
        banished: [...prev.banished],
        extraDeck: [...prev.extraDeck],
      }

      for (const material of paidMaterials) {
        let originZone = null
        for (const zoneName of ['field', 'graveyard', 'banished', 'extraDeck']) {
          const existingIndex = next[zoneName].findIndex(entry => entry.card.id === material.id)
          if (existingIndex >= 0) {
            next[zoneName].splice(existingIndex, 1)
            originZone = zoneName
            break
          }
        }
        if (materialDestination === 'Graveyard') {
          const isToken = (material.type || '').toLowerCase().includes('token')
              || material.name.toLowerCase().includes('token')
          if (isToken) {
            continue
          }
          const isFieldPendulum = originZone === 'field'
              && (material.type || '').toLowerCase().includes('pendulum')
          if (isFieldPendulum) {
            next.extraDeck.push(zoneEntry(material, 'pendulum-replacement'))
          } else {
            next.graveyard.push(zoneEntry(material, paymentReason))
          }
        } else if (materialDestination === 'Banished') {
          next.banished.push(zoneEntry(material, 'banish'))
        }
      }

      const destination = (option.destination || '').toLowerCase()
      if (destination.includes('monster zone') || destination.includes('pendulum zone')) {
        next.field.push(zoneEntry(card))
      }
      return next
    })
    setSelected(card)
    setComboPath(prev => [...prev, card])
    setActiveZoneContext(null)
    setActiveCardTurn(currentTurn)
    await fetchCardExtras(card)
    await fetchComboOptions(card)
  }

  async function chooseComboOption(option) {
    const isPaidFusion = option.cost?.includes('Materials:')
        && (option.card.type || '').toLowerCase().includes('fusion')
    if (!option.cost) {
      await finishComboChoice(option)
      return
    }

    setPendingOption(option)
    setMaterialPlan(null)
    setMaterialSelections({})
    setMaterialSearch('')
    setMaterialError(null)
    setMaterialLoading(true)
    try {
      const plannerPath = isPaidFusion ? 'fusion-materials' : 'cost-materials'
      const response = await fetch(
          `${API}/yugioh/card/${plannerPath}?source=${encodeURIComponent(selected.name)}&target=${encodeURIComponent(option.card.name)}`,
      )
      if (!response.ok) throw new Error('Could not determine legal cards for this route cost.')
      setMaterialPlan(await response.json())
    } catch (requestError) {
      setMaterialError(requestError.message)
    } finally {
      setMaterialLoading(false)
    }
  }

  function closeMaterialPicker() {
    setPendingOption(null)
    setMaterialPlan(null)
    setMaterialSelections({})
    setMaterialSearch('')
    setMaterialError(null)
    setMaterialLoading(false)
  }

  function addMaterial(slotIndex, card) {
    setMaterialSelections(prev => {
      const current = prev[slotIndex] ?? []
      const maximum = materialPlan.slots[slotIndex].count
      if (current.length >= maximum) return prev
      return { ...prev, [slotIndex]: [...current, card] }
    })
  }

  function removeMaterial(slotIndex, selectedIndex) {
    setMaterialSelections(prev => ({
      ...prev,
      [slotIndex]: (prev[slotIndex] ?? []).filter((_, index) => index !== selectedIndex),
    }))
  }

  async function confirmMaterialPayment() {
    if (!pendingOption || !materialPlan) return
    const complete = materialPlan.slots.every(
        (slot, index) => (materialSelections[index]?.length ?? 0) === slot.count,
    )
    if (!complete) return

    const materials = materialPlan.slots.flatMap((_, index) => materialSelections[index] ?? [])
    const option = pendingOption
    const destination = materialPlan.destination
    const action = (materialPlan.action || '').toLowerCase()
    const isFusionPayment = (option.card.type || '').toLowerCase().includes('fusion')
    const paymentReason = isFusionPayment
      ? 'fusion-material'
      : action.includes('discard')
        ? 'discard'
        : action.includes('tribute')
          ? 'tribute'
          : action.includes('destroy')
            ? 'destroy'
            : action.includes('banish')
              ? 'banish'
              : 'send'
    closeMaterialPicker()
    await finishComboChoice(option, materials, destination, paymentReason)
  }

  async function activateZoneCard(entry, zone) {
    setComboHistory(prev => [...prev, snapshotComboState()])
    setActivatedZoneEffects(prev => [...prev, entry.instanceId])
    setActiveZoneContext(zone)
    setActiveCardTurn(currentTurn)
    setSelected(entry.card)
    setComboPath(prev => [...prev, entry.card])
    await fetchCardExtras(entry.card)
    await fetchComboOptions(entry.card, zone)
  }

  async function goBackCombo() {
    if (comboHistory.length === 0) return
    const previousState = comboHistory[comboHistory.length - 1]
    const previousCard = previousState.comboPath[previousState.comboPath.length - 1]
    setComboHistory(prev => prev.slice(0, -1))
    setComboPath(previousState.comboPath)
    setZones(previousState.zones)
    setActivatedZoneEffects(previousState.activatedZoneEffects)
    setActiveZoneContext(previousState.activeZoneContext)
    setCurrentTurn(previousState.currentTurn)
    setPhase(previousState.phase)
    setActiveCardTurn(previousState.activeCardTurn)
    setSelected(previousCard)
    closeMaterialPicker()
    await fetchCardExtras(previousCard)
    await fetchComboOptions(previousCard, previousState.activeZoneContext)
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
    setComboPath([])
    setComboOptions([])
    setComboLoading(false)
    setZones({ field: [], graveyard: [], banished: [], extraDeck: [] })
    setComboHistory([])
    setActivatedZoneEffects([])
    setActiveZoneContext(null)
    setCurrentTurn(1)
    setPhase('Main Phase')
    setActiveCardTurn(1)
    closeMaterialPicker()

    try {
      if (mode === 'exact') {
        const res = await fetch(`${API}/yugioh/card?name=${encodeURIComponent(query)}`)
        if (!res.ok) throw new Error('Card not found')
        const card = await res.json()
        setCards([card])
        await openRootCard(card)
      } else {
        const res = await fetch(`${API}/yugioh/card/substring?name=${encodeURIComponent(query)}`)
        if (!res.ok) throw new Error('No cards found')
        const list = await res.json()
        setCards(list)
        if (list.length === 1) await openRootCard(list[0])
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const showList = cards.length > 1

  function advancePhase() {
    if (phase === 'Standby Phase') {
      setPhase('Main Phase')
    } else if (phase === 'Main Phase') {
      setPhase('Battle Phase')
    } else if (phase === 'Battle Phase') {
      setPhase('End Phase')
    } else {
      setCurrentTurn(turn => turn + 1)
      setPhase('Standby Phase')
    }
  }

  return (
      <div className="app">
        <header className="app-header">
          <div className="header-title">
            <span className="title-accent">Yu-Gi-Oh!</span> Card Search
          </div>
        </header>

        <main className="app-main">
          <form className="search-form" onSubmit={handleSearch}>
            <div className="search-row">
              <input
                  className="search-input"
                  type="text"
                  placeholder="Enter a card name..."
                  value={query}
                  onChange={e => setQuery(e.target.value)}
              />
              <button className="search-btn" type="submit" disabled={loading}>
                {loading ? '...' : 'Search'}
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

          {error && <div className="error-msg">{error}</div>}

          {loading && (
              <div className="loading">
                <div className="spinner" />
              </div>
          )}

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
                              onClick={() => openRootCard(c)}
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
                          comboPath={comboPath}
                          comboOptions={comboOptions}
                          comboLoading={comboLoading}
                          onChooseCard={chooseComboOption}
                          onBackCombo={goBackCombo}
                          zones={zones}
                          activatedZoneEffects={activatedZoneEffects}
                          onActivateZoneCard={activateZoneCard}
                          currentTurn={currentTurn}
                          phase={phase}
                          activeCardTurn={activeCardTurn}
                          onAdvancePhase={advancePhase}
                      />
                  ) : !error && cards.length === 0 && (
                      <div className="empty-state">
                        <div className="empty-state-icon">*</div>
                        <p>Search for a card to begin</p>
                      </div>
                  )}
                </div>
              </div>
          )}
        </main>
        <MaterialPicker
            pendingOption={pendingOption}
            materialPlan={materialPlan}
            loading={materialLoading}
            error={materialError}
            selections={materialSelections}
            search={materialSearch}
            onSearchChange={setMaterialSearch}
            onAddMaterial={addMaterial}
            onRemoveMaterial={removeMaterial}
            onConfirm={confirmMaterialPayment}
            onCancel={closeMaterialPicker}
            zones={zones}
        />
      </div>
  )
}
