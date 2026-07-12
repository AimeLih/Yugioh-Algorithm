package com.aimestart.yugiohsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class YugiohService {

    private final RestClient restClient;
    private final CardRepository cardRepository;

    public record CardInfoResponse(List<CardData> data) {}
    public record CardImage(
            @JsonProperty("image_url") String imageUrl
    ) {}
    public record CardData(String name, String desc, String type, Integer atk, Integer def, Integer level,
                           String race, String attribute, Integer linkval, String archetype, String [] linkmarkers, String staple, Integer scale, @JsonProperty("card_images") List<CardImage> cardImages) {}
    public record ComboOption(Card card, String reason, int score, String label) {}

    private static class ComboDraft {
        private final Card card;
        private int score;
        private final LinkedHashSet<String> reasons = new LinkedHashSet<>();

        private ComboDraft(Card card) {
            this.card = card;
        }
    }

    public YugiohService(RestClient.Builder builder, CardRepository cardRepository) {
        this.restClient = builder.baseUrl("https://db.ygoprodeck.com/api/v7").build();
        this.cardRepository = cardRepository;
    }

    public List<CardData> fetchallCards() {
        CardInfoResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cardinfo.php")
                        .build())
                .retrieve()
                .body(CardInfoResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new RuntimeException("Cards not found");
        }
        return response.data();
    }

    public void importAllCards() {
        List<CardData> apiCards = fetchallCards();
        List<Card> cardsToSave = new ArrayList<>();
        for (CardData card : apiCards) {
            //if (!cardRepository.existsByName(card.name())) {
                cardsToSave.add(new Card(card.name(), card.desc(), card.type(), 0));
           // }
        }
        cardRepository.saveAll(cardsToSave);
        updateExistingCards();

    }

    public void updateExistingCardsWeight() {
        List<Card> cards = cardRepository.findAll();
        List<CardData> apiCards = fetchallCards();
        for (Card card : cards) {
            for (CardData apiCard : apiCards) {
                if (card.getName().equals(apiCard.name())) {
                    String TYPE = apiCard.type();
                    card.setType(TYPE);
                    boolean switchwork = false;
                    switch(TYPE){
                        case "Spell Card":
                            card.setWeight(4);
                            switchwork = true;
                            break;
                        case "Trap Card":
                            card.setWeight(3);
                            switchwork = true;
                            break;
                        case "Effect Monster":
                            card.setWeight(2);
                            switchwork = true;
                            break;
                        case "Normal Monster":
                            card.setWeight(1);
                            switchwork = true;
                            break;
                        default:
                    }
                    if(!switchwork){
                        if(TYPE.contains("Spell")){
                            card.setWeight(4);
                        } else if (TYPE.contains("Trap")) {
                            card.setWeight(3);
                        } else if (TYPE.contains("Monster")) {
                            card.setWeight(2);
                        } else {
                            card.setWeight(1);
                        }
                    }
                    cardRepository.save(card);
                }
            }
        }
    }


    public Set<String> fetchStapleCardNames() {
        try {
            CardInfoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cardinfo.php")
                            .queryParam("staple", "yes")
                            .build())
                    .retrieve()
                    .body(CardInfoResponse.class);

            if (response != null && response.data() != null) {

                return response.data().stream()
                        .map(CardData::name)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            System.out.println("Could not fetch staples: " + e.getMessage());
        }
        return Collections.emptySet();
    }

    public String getImage(Card card){
        return card.getCardImageUrl();
    }
    public void updateExistingCards() {
        List<Card> cards = cardRepository.findAll();
        Map<String, CardData> apiCards = fetchallCards().stream()
                .collect(Collectors.toMap(CardData::name, Function.identity()));

        Set<String> stapleNames = fetchStapleCardNames();
        for (Card card : cards) {
            CardData apiCard = apiCards.get(card.getName());
            if (apiCard == null) {
                continue;
            }

            card.setStaple(stapleNames.contains(card.getName()));

            if (apiCard.cardImages() != null && !apiCard.cardImages().isEmpty()) {
                card.setCardImageUrl(apiCard.cardImages().get(0).imageUrl());
            }

            String cardType = apiCard.type() != null ? apiCard.type() : card.getType();
            if (cardType != null) {
                card.setType(cardType);
            }

            if (cardType != null && cardType.contains("Pendulum")) {
                card.setScale(apiCard.scale());
            }

            if (cardType != null && cardType.contains("Link") && cardType.contains("Monster")) {
                card.setLinkvalue(apiCard.linkval());
                if (apiCard.linkmarkers() != null) {
                    card.setLinkmarkers(Arrays.asList(apiCard.linkmarkers()));
                }
            } else if (cardType != null && cardType.contains("Monster")) {
                card.setAtk(apiCard.atk());
                card.setDef(apiCard.def());
                card.setLevel(apiCard.level());
                card.setRace(apiCard.race());
                card.setAttribute(apiCard.attribute());
            } else if (cardType != null && (cardType.contains("Spell") || cardType.contains("Trap"))) {
                card.setRace(apiCard.race());
            }

            if (apiCard.archetype() != null && !apiCard.archetype().isBlank()) {
                card.setArchetype(apiCard.archetype());
            }
        }
        cardRepository.saveAll(cards);
    }

    public void updateDatabaseCards() {
        List<Card> cards = cardRepository.findAll();
      for(Card card : cards){
          if(card.getType().equals("Effect Monster")){
              if(card.getDescription().toLowerCase().contains("banish") || card.getDescription().toLowerCase().contains("destroy") &&
                      card.getDescription().toLowerCase().contains("quick effect")) {
                  card.setWeight(card.getWeight() + 6); //placeholder value
              }
              if(card.getDescription().toLowerCase().contains("banish") || card.getDescription().toLowerCase().contains("destroy") &&
                      card.getDescription().toLowerCase().contains("target") && card.getDescription().toLowerCase().contains("quick effect")) {
                  card.setWeight(card.getWeight() + 5); //placeholder value
              }
              if(card.getDescription().toLowerCase().contains("banish") || card.getDescription().toLowerCase().contains("destroy") &&
                      card.getDescription().toLowerCase().contains("target")) {
                  card.setWeight(card.getWeight() + 4); //placeholder value
              }
              if(card.getDescription().toLowerCase().contains("add")){
                  card.setWeight(card.getWeight() + 3); //placeholder value
              }
          }
      }

    }

    public List<Card> getAllCards(){
      return cardRepository.findAll();
    }

    public List<ComboOption> getPossibleCombos(String cardName) {
        Card card = cardRepository.getCardByName(cardName);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + cardName);
        }

        List<ComboOption> fusionTargets = buildFusionSummonTargets(card);
        List<ComboOption> curated = buildCuratedGuideCombos(card);
        if (!curated.isEmpty()) {
            return mergeComboOptions(fusionTargets, curated);
        }

        String description = safeLower(card.getDescription());
        Map<String, ComboDraft> drafts = new LinkedHashMap<>();

        buildExplicitComboRoutes(card, description, drafts);
        buildTextDrivenComboRoutes(card, description, drafts);

        List<ComboOption> options = drafts.values().stream()
                .filter(draft -> draft.score > 0)
                .sorted(Comparator
                        .comparingInt((ComboDraft draft) -> draft.score).reversed()
                        .thenComparing(draft -> draft.card.getName(), String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(draft -> new ComboOption(draft.card, joinReasons(draft.reasons), draft.score, comboLabelFor(draft.card)))
                .collect(Collectors.toList());

        return mergeComboOptions(fusionTargets, options);
    }

    public Card getCardByName(String name) {
        return cardRepository.getCardByName(name);
    }

    public List<Card> getCardsBySubstring(String name) {
        List<Card> cards = cardRepository.findByNameContainingIgnoreCase(name);
        if (cards.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No cards found with that name");
        }
        return cards;
    }

    public String ifExtender(String focusedcard) {
        Card card = cardRepository.getCardByName(focusedcard);
        if (card == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + focusedcard);
        String desc = card.getDescription() != null ? card.getDescription().toLowerCase() : "";
        if (Pattern.compile("summon\\s+\\d+").matcher(desc).find()) {
            return "summon extender";
        }
        if (Pattern.compile("add\\s+\\d+").matcher(desc).find()) {
            return "add extender";
        }
        return "not an extender";
    }

    public String isOncePerTurn(String focusedcard) {
        Card card = cardRepository.getCardByName(focusedcard);
        if (card == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + focusedcard);
        String desc = card.getDescription() != null ? card.getDescription() : "";
       
        if (Pattern.compile("You can only use this effect of \".+\" once per turn", Pattern.CASE_INSENSITIVE).matcher(desc).find()) {
            return "one effect";
        }

        if (Pattern.compile("You can only activate 1 \".+\" per turn", Pattern.CASE_INSENSITIVE).matcher(desc).find()) {
            return "one activation";
        }

        if (Pattern.compile("You can only use each of the following effects of \".+\" once per turn", Pattern.CASE_INSENSITIVE).matcher(desc).find()) {
            return "one of each";
        }
        return "Not once per turn";
    }

    public void allCardWeightZero(){
        List<Card> cards = cardRepository.findAll();
        for(Card card : cards){
            card.setWeight(0);
        }
        cardRepository.saveAll(cards);
    }

    private List<Card> getRelatedArchetypeCards(Card card) {
        if (card.getArchetype() == null || card.getArchetype().isBlank()) {
            return Collections.emptyList();
        }

        return cardRepository.findByArchetypeContainingIgnoreCase(card.getArchetype()).stream()
                .filter(other -> !Objects.equals(other.getId(), card.getId()))
                .sorted(Comparator
                        .comparingInt(Card::getWeight).reversed()
                        .thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private List<ComboOption> buildFusionSummonTargets(Card card) {
        String description = safeLower(card.getDescription());
        if (!description.contains("fusion summon")) {
            return Collections.emptyList();
        }

        return getRelatedArchetypeCards(card).stream()
                .filter(this::isFusionMonster)
                .sorted(Comparator
                        .comparingInt(Card::getWeight).reversed()
                        .thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
                .map(target -> new ComboOption(
                        target,
                        card.getName() + " can Fusion Summon this in-archetype Fusion Monster when its material requirements are met",
                        200,
                        "fusion target"))
                .collect(Collectors.toList());
    }

    private List<ComboOption> mergeComboOptions(List<ComboOption> priorityOptions, List<ComboOption> otherOptions) {
        LinkedHashMap<String, ComboOption> merged = new LinkedHashMap<>();
        for (ComboOption option : priorityOptions) {
            merged.put(safeLower(option.card().getName()), option);
        }
        for (ComboOption option : otherOptions) {
            merged.putIfAbsent(safeLower(option.card().getName()), option);
        }
        return merged.values().stream().limit(5).collect(Collectors.toList());
    }

    private void buildExplicitComboRoutes(Card card, String description, Map<String, ComboDraft> drafts) {
        List<Card> relatedCards = getRelatedArchetypeCards(card);
        String archetype = safeLower(card.getArchetype());

        List<String> quotedCards = extractQuotedTerms(card.getDescription());
        for (String quoted : quotedCards) {
            Card matchedCard = findBestCardMatch(quoted);
            if (matchedCard == null || Objects.equals(matchedCard.getId(), card.getId())) {
                continue;
            }
            if (!sameArchetype(archetype, matchedCard)) {
                continue;
            }

            ComboDraft draft = draftFor(drafts, matchedCard);
            addScore(draft, 100, "Effect text directly names this card");

            if (description.contains("fusion summon")) {
                addScore(draft, 20, "It appears in a Fusion Summon line");
            }
            if (description.contains("add to your hand") || description.contains("set 1") || description.contains("search")) {
                addScore(draft, 15, "It is a follow-up target from the card text");
            }
        }

        if (description.contains("fusion summon") && description.contains("branded")) {
            Card brandedSpellTrap = relatedCards.stream()
                    .filter(this::isSpellOrTrap)
                    .filter(c -> safeLower(c.getName()).contains("branded"))
                    .findFirst()
                    .orElse(null);

            if (brandedSpellTrap != null && (description.contains("add to your hand") || description.contains("set 1"))) {
                ComboDraft draft = draftFor(drafts, brandedSpellTrap);
                addScore(draft, 35, "It is a branded follow-up spell or trap");
                draft.reasons.add(card.getName() + " can recycle or set it for follow-up");
            }
        }
    }

    private void buildTextDrivenComboRoutes(Card card, String description, Map<String, ComboDraft> drafts) {
        List<Card> candidates = getRelatedArchetypeCards(card);

        if (candidates.isEmpty()) {
            return;
        }

        boolean sourceCanSearch = containsAny(description, "search", "add to your hand", "add 1", "reveal 1", "draw 1");
        boolean sourceCanSummon = containsAny(description, "special summon", "normal summon", "tribute summon");
        boolean sourceCanRecover = containsAny(description, "send to the graveyard", "discard", "banish", "graveyard", "recycle");
        boolean sourceCanSetBackrow = containsAny(description, "set 1", "set it", "set this", "place 1");

        for (Card candidate : candidates) {
            String targetDescription = safeLower(candidate.getDescription());
            int score = 0;
            LinkedHashSet<String> reasons = new LinkedHashSet<>();

            if (sourceCanSearch && isSearchBridgeTarget(candidate, targetDescription)) {
                score += 70;
                reasons.add("Source text can search or add this card");
                if (isSelfSummoningBridgeTarget(candidate, targetDescription)) {
                    reasons.add("This card can special summon itself after being searched");
                    score += 15;
                }
            }

            if (sourceCanSummon && isSelfSummoningBridgeTarget(candidate, targetDescription)) {
                score += 60;
                reasons.add("This card can special summon itself or another extender");
            }

            if (sourceCanRecover && isGraveyardBridgeTarget(candidate, targetDescription)) {
                score += 55;
                reasons.add("This card has graveyard recursion that turns setup into follow-up");
            }

            if (sourceCanSetBackrow && isBackrowBridgeTarget(candidate, targetDescription)) {
                score += 40;
                reasons.add("This is searchable/settable backrow follow-up");
            }

            if (score <= 0) {
                continue;
            }

            ComboDraft draft = draftFor(drafts, candidate);
            addScore(draft, score, "Text-driven combo bridge");
            draft.reasons.addAll(reasons);
            if (isStarterCard(candidate)) {
                draft.reasons.add("Acts as a starter once accessed");
            } else if (isExtenderCard(candidate)) {
                draft.reasons.add("Acts as an extender once accessed");
            } else if (isFollowUpCard(candidate)) {
                draft.reasons.add("Useful for follow-up after the first line");
            }
        }
    }

    private boolean isSpellOrTrap(Card card) {
        String type = safeLower(card.getType());
        return type.contains("spell") || type.contains("trap");
    }

    private boolean isFusionMonster(Card card) {
        String type = safeLower(card.getType());
        return type.contains("fusion") && type.contains("monster");
    }

    private boolean isStarterCard(Card card) {
        String desc = safeLower(card.getDescription());
        return containsAny(desc, "search", "add to hand", "add 1", "draw", "reveal");
    }

    private boolean isExtenderCard(Card card) {
        String desc = safeLower(card.getDescription());
        return containsAny(desc,
                "special summon this card",
                "special summon itself",
                "special summon from your hand",
                "special summon from your graveyard",
                "if this card is in your hand",
                "from your hand",
                "from your graveyard",
                "extra deck");
    }

    private boolean isPayoffCard(Card card) {
        String type = safeLower(card.getType());
        return type.contains("fusion") || type.contains("synchro") || type.contains("xyz") || type.contains("link") || type.contains("ritual");
    }

    private boolean isFollowUpCard(Card card) {
        String desc = safeLower(card.getDescription());
        return containsAny(desc, "graveyard", "end phase", "set 1", "add to your hand", "search", "recycle", "return to the hand");
    }

    private boolean isSearchBridgeTarget(Card card, String description) {
        return isSpellOrTrap(card)
                ? containsAny(description, "search", "add 1", "add to your hand", "set 1", "reveal")
                : containsAny(description, "search", "add 1", "add to your hand", "special summon this card", "special summon itself", "from your hand", "from your graveyard");
    }

    private boolean isSelfSummoningBridgeTarget(Card card, String description) {
        if (!safeLower(card.getType()).contains("monster")) {
            return false;
        }

        return containsAny(description,
                "special summon this card",
                "special summon itself",
                "special summon from your hand",
                "special summon from your graveyard",
                "if this card is in your hand",
                "if this card is sent to the graveyard",
                "if this card is normal summoned",
                "if you control");
    }

    private boolean isGraveyardBridgeTarget(Card card, String description) {
        return containsAny(description,
                "from your graveyard",
                "if this card is sent to the graveyard",
                "banish this card",
                "return this card from your graveyard",
                "send this card to the graveyard",
                "during the end phase");
    }

    private boolean isBackrowBridgeTarget(Card card, String description) {
        return isSpellOrTrap(card) && containsAny(description, "search", "add 1", "add to your hand", "set 1", "recycle", "activate 1");
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || text.isBlank()) {
            return false;
        }

        for (String term : terms) {
            if (term != null && !term.isBlank() && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private List<String> extractQuotedTerms(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> terms = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(text);
        while (matcher.find()) {
            String term = matcher.group(1).trim();
            if (!term.isEmpty()) {
                terms.add(term);
            }
        }
        return terms;
    }

    private Card findBestCardMatch(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }

        String normalized = term.trim().toLowerCase();
        List<Card> cards = cardRepository.findByNameContainingIgnoreCase(term);
        if (cards.isEmpty()) {
            cards = cardRepository.findByNameContainingIgnoreCase(normalized);
        }
        if (cards.isEmpty()) {
            return null;
        }

        for (Card candidate : cards) {
            if (safeLower(candidate.getName()).equals(normalized)) {
                return candidate;
            }
        }

        return cards.get(0);
    }

    private ComboDraft draftFor(Map<String, ComboDraft> drafts, Card card) {
        return drafts.computeIfAbsent(safeLower(card.getName()), key -> new ComboDraft(card));
    }

    private void addScore(ComboDraft draft, int points, String reason) {
        draft.score += points;
        if (reason != null && !reason.isBlank()) {
            draft.reasons.add(reason);
        }
    }

    private String joinReasons(LinkedHashSet<String> reasons) {
        if (reasons.isEmpty()) {
            return "Good follow-up option";
        }
        return String.join("; ", reasons);
    }

    private String comboLabelFor(Card card) {
        if (isStarterCard(card)) {
            return "starter";
        }
        if (isExtenderCard(card)) {
            return "extender";
        }
        if (isFollowUpCard(card)) {
            return "follow-up";
        }
        return "support";
    }

    private List<ComboOption> buildCuratedGuideCombos(Card card) {
        List<ComboOption> options = new ArrayList<>();
        String name = safeLower(card.getName());
        String archetype = safeLower(card.getArchetype());

        if (name.contains("d/d") || archetype.contains("d/d")) {
            addCuratedCombo(options, "D/D Savant Kepler", card, "Guide-backed starter: Kepler searches Dark Contract with the Gate, which searches any D/D monster.", "starter", 120);
            addCuratedCombo(options, "Dark Contract with the Gate", card, "Guide-backed starter: Gate searches any D/D monster and is itself a one-card combo.", "starter", 115);
            addCuratedCombo(options, "D/D Gryphon", card, "Guide-backed extender: Gryphon special summons itself and searches after it hits the GY.", "extender", 110);
            addCuratedCombo(options, "D/D Necro Slime", card, "Guide-backed extender: Necro Slime enables Fusion Summons by banishing itself and another D/D from the GY.", "extender", 108);
            addCuratedCombo(options, "D/D Count Surveyor", card, "Guide-backed extender: Count Surveyor discards another D/D and replaces the discard with another search target.", "extender", 106);
            addCuratedCombo(options, "D/D/D Zero Doom Queen Machinex", card, "Guide-backed follow-up: Machinex is a 1-card starter that can place a Dark Contract directly from the deck.", "follow-up", 130);
            addCuratedCombo(options, "D/D Lance Soldier", card, "Guide-backed extender: Lance Soldier destroys a Dark Contract to summon itself and manipulate levels.", "extender", 104);
            addCuratedCombo(options, "D/D Orthros", card, "Guide-backed utility: Orthros is the low-scale extender and backrow remover used in combo lines.", "support", 100);
        }

        if (name.contains("swordsoul") || archetype.contains("swordsoul") || name.contains("incredible ecclesia")) {
            addCuratedCombo(options, "Swordsoul of Mo Ye", card, "Guide-backed starter: Mo Ye generates a Token and is the cleanest route into Level 8 Synchro plays.", "starter", 120);
            addCuratedCombo(options, "Swordsoul Strategist Longyuan", card, "Guide-backed extender: Longyuan discards a card to reach Baronne de Fleur or other Level 10 Synchro lines.", "extender", 118);
            addCuratedCombo(options, "Swordsoul Blackout", card, "Guide-backed follow-up: Blackout is the searchable interaction piece that completes the line.", "follow-up", 112);
            addCuratedCombo(options, "Swordsoul Grandmaster - Chixiao", card, "Guide-backed follow-up: the basic Mo Ye line turns into Chixiao.", "follow-up", 116);
            addCuratedCombo(options, "Baronne de Fleur", card, "Guide-backed follow-up: Longyuan makes the simple Baronne line.", "follow-up", 114);
        }

        if (name.contains("archfiend") || archetype.contains("archfiend") || name.contains("tour guide")) {
            addCuratedCombo(options, "Archfiend Heiress", card, "Guide-backed searcher: Heiress converts Archfiend names into more access.", "starter", 112);
            addCuratedCombo(options, "Archfiend Strategy", card, "Guide-backed searcher: Strategy finds any Archfiend card and keeps the engine moving.", "starter", 110);
            addCuratedCombo(options, "Archfiend's Usurpation", card, "Guide-backed starter/removal card: Usurpation can start plays and acts as a ritual spell.", "starter", 108);
            addCuratedCombo(options, "Archfiend Emperor", card, "Guide-backed follow-up: Emperor is the main boss monster and primary endboard piece.", "follow-up", 130);
            addCuratedCombo(options, "Archfiend Matriarch", card, "Guide-backed follow-up: Matriarch is the grind-game recycler and follow-up threat.", "follow-up", 114);
        }

        if (name.contains("dogmatika") || archetype.contains("dogmatika") || name.contains("ecclesia") || name.contains("nadir servant") || name.contains("fallen of the white dragon")) {
            addCuratedCombo(options, "Dogmatika Ecclesia, the Virtuous", card, "Guide-backed starter: Ecclesia searches any Dogmatika card and is the cleanest bridge into the archetype.", "starter", 120);
            addCuratedCombo(options, "Nadir Servant", card, "Guide-backed starter: Nadir Servant sends an Extra Deck monster to access Dogmatika pieces.", "starter", 116);
            addCuratedCombo(options, "Dogmatika Punishment", card, "Guide-backed follow-up: Punishment is the main trap interaction and converts into ED-based removal.", "follow-up", 112);
            addCuratedCombo(options, "Dogmatika Fleurdelis, the Knighted", card, "Guide-backed extender: Fleurdelis is the on-board negate that follows Dogmatika access pieces.", "extender", 108);
            addCuratedCombo(options, "The Fallen & The Virtuous", card, "Guide-backed follow-up: the main send-and-destroy spell that converts Dogmatika setup into tempo.", "follow-up", 114);
        }

        if (name.contains("exosister") || archetype.contains("exosister") || name.contains("martha") || name.contains("pax")) {
            addCuratedCombo(options, "Exosister Martha", card, "Guide-backed starter: Martha is the easiest way to get an Exosister body on board.", "starter", 120);
            addCuratedCombo(options, "Exosister Pax", card, "Guide-backed starter: Pax is the deck's main search spell and links the rest of the line together.", "starter", 118);
            addCuratedCombo(options, "Exosister Mikailis", card, "Guide-backed follow-up: Mikailis is the primary first Xyz that turns the starter into advantage.", "follow-up", 116);
            addCuratedCombo(options, "Exosister Kaspitell", card, "Guide-backed extender: Kaspitell converts spare names into another rank 4 body.", "extender", 110);
            addCuratedCombo(options, "Exosister Magnifica", card, "Guide-backed follow-up: Magnifica is the layered endboard upgrade that gives the deck its closing power.", "follow-up", 114);
            addCuratedCombo(options, "Exosister Karmael", card, "Guide-backed follow-up: Karmael gives the deck another disruption layer and a way to keep playing.", "follow-up", 106);
        }

        if (name.contains("stardust") || name.contains("bystial") || archetype.contains("bystial") || archetype.contains("stardust")) {
            addCuratedCombo(options, "Stardust Dragon", card, "Guide-backed starter: Stardust is the centerpiece used to branch into the modern Synchro lines.", "starter", 120);
            addCuratedCombo(options, "Bystial Magnamhut", card, "Guide-backed extender: Magnamhut is one of the best ways to convert a grave setup into follow-up.", "extender", 118);
            addCuratedCombo(options, "Bystial Druiswurm", card, "Guide-backed extender: Druiswurm is a live Bystial body that both pressures and clears cards.", "extender", 114);
            addCuratedCombo(options, "Stardust Synchron", card, "Guide-backed starter: Synchron is the card that converts Stardust access into the combo tree.", "starter", 116);
            addCuratedCombo(options, "Junk Speeder", card, "Guide-backed follow-up: Speeder is one of the big Synchro route endpoints if your build includes the Warrior package.", "follow-up", 112);
            addCuratedCombo(options, "Dis Pater, the Black Dragon", card, "Guide-backed follow-up: Dis Pater is a recurring Synchro payoff and recursion piece.", "follow-up", 110);
        }

        options.sort(Comparator
                .comparingInt((ComboOption option) -> option.score()).reversed()
                .thenComparing(option -> option.card().getName(), String.CASE_INSENSITIVE_ORDER));
        return options.stream().limit(5).collect(Collectors.toList());
    }

    private void addCuratedCombo(List<ComboOption> options, String targetName, Card sourceCard, String reason, String label, int score) {
        Card target = findBestCardMatch(targetName);
        if (target == null || Objects.equals(target.getId(), sourceCard.getId())) {
            return;
        }

        if (!sameArchetype(sourceCard, target)) {
            return;
        }

        options.add(new ComboOption(target, reason, score, label));
    }

    private boolean sameArchetype(Card sourceCard, Card targetCard) {
        String sourceArchetype = safeLower(sourceCard.getArchetype()).trim();
        String targetArchetype = safeLower(targetCard.getArchetype()).trim();
        if (sourceArchetype.isBlank() || targetArchetype.isBlank()) {
            return false;
        }
        return sourceArchetype.equals(targetArchetype);
    }

    private boolean sameArchetype(String sourceArchetype, Card targetCard) {
        String targetArchetype = safeLower(targetCard.getArchetype()).trim();
        if (sourceArchetype == null || sourceArchetype.isBlank() || targetArchetype.isBlank()) {
            return false;
        }
        return sourceArchetype.trim().equals(targetArchetype);
    }
}
