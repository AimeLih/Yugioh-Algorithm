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

    public List<String> getPossibleCombos(String cardName) {
        Card card = cardRepository.getCardByName(cardName);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + cardName);
        }

        List<String> combos = new ArrayList<>();
        String description = safeLower(card.getDescription());
        List<Card> relatedCards = getRelatedArchetypeCards(card);

        combos.addAll(buildExplicitComboRoutes(card, description, relatedCards));
        combos.addAll(buildArchetypeComboRoutes(card, relatedCards));
        combos.addAll(buildTextureComboRoutes(card, description, relatedCards));

        List<String> deduped = new ArrayList<>(new LinkedHashSet<>(combos));
        if (deduped.isEmpty()) {
            deduped.add("No specific combo routes identified for this card yet.");
        }

        return deduped;
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

    private List<String> buildExplicitComboRoutes(Card card, String description, List<Card> relatedCards) {
        List<String> routes = new ArrayList<>();

        List<String> quotedCards = extractQuotedTerms(card.getDescription());
        for (String quoted : quotedCards) {
            Card matchedCard = findBestCardMatch(quoted);
            if (matchedCard == null || Objects.equals(matchedCard.getId(), card.getId())) {
                continue;
            }

            if (description.contains("fusion summon") && description.contains(safeLower(matchedCard.getName()))) {
                routes.add(matchedCard.getName() + " + 1 LIGHT monster -> Fusion Summon " + card.getName() + ".");
                continue;
            }

            if (description.contains("add to your hand") || description.contains("set 1")) {
                routes.add(card.getName() + " -> use " + matchedCard.getName() + " as the follow-up search or set target.");
                continue;
            }

            routes.add(matchedCard.getName() + " -> pairs with " + card.getName() + " for the text line on this card.");
        }

        if (description.contains("fusion summon")) {
            Card brandedSpellTrap = relatedCards.stream()
                    .filter(this::isSpellOrTrap)
                    .filter(c -> safeLower(c.getName()).contains("branded"))
                    .findFirst()
                    .orElse(null);

            if (brandedSpellTrap != null && (description.contains("add to your hand") || description.contains("set 1"))) {
                routes.add(card.getName() + " in GY -> add or set " + brandedSpellTrap.getName() + " for the next turn.");
            }
        }

        return routes;
    }

    private List<String> buildArchetypeComboRoutes(Card card, List<Card> relatedCards) {
        List<String> routes = new ArrayList<>();
        if (relatedCards.isEmpty()) {
            return routes;
        }

        List<Card> starters = relatedCards.stream().filter(this::isStarterCard).limit(2).collect(Collectors.toList());
        List<Card> extenders = relatedCards.stream().filter(this::isExtenderCard).limit(2).collect(Collectors.toList());
        List<Card> payoffCards = relatedCards.stream().filter(this::isPayoffCard).limit(2).collect(Collectors.toList());
        List<Card> followUps = relatedCards.stream().filter(this::isFollowUpCard).limit(2).collect(Collectors.toList());
        List<Card> spellTrapSupport = relatedCards.stream().filter(this::isSpellOrTrap).limit(3).collect(Collectors.toList());

        if (!starters.isEmpty()) {
            Card starter = starters.get(0);
            routes.add(starter.getName() + " -> accesses " + card.getName() + " to start the " + archetypeLabel(card) + " engine.");
        }

        if (!extenders.isEmpty()) {
            Card extender = extenders.get(0);
            String payoff = !payoffCards.isEmpty() ? payoffCards.get(0).getName() : card.getName();
            routes.add(extender.getName() + " -> extends into " + card.getName() + " and converts into " + payoff + ".");
        }

        if (!spellTrapSupport.isEmpty() && (safeLower(card.getDescription()).contains("add to your hand") || safeLower(card.getDescription()).contains("set 1"))) {
            routes.add(card.getName() + " -> adds or sets " + joinCardNames(spellTrapSupport) + " for follow-up.");
        }

        if (!followUps.isEmpty()) {
            Card followUp = followUps.get(0);
            routes.add(card.getName() + " -> keeps the turn going into " + followUp.getName() + " as the follow-up piece.");
        }

        return routes;
    }

    private List<String> buildTextureComboRoutes(Card card, String description, List<Card> relatedCards) {
        List<String> routes = new ArrayList<>();
        if (description.contains("special summon") && !relatedCards.isEmpty()) {
            Card extender = relatedCards.stream().filter(this::isExtenderCard).findFirst().orElse(relatedCards.get(0));
            routes.add(extender.getName() + " -> special summons into " + card.getName() + " or another " + archetypeLabel(card) + " card.");
        }

        if (description.contains("send to graveyard") || description.contains("discard")) {
            Card gyPayoff = relatedCards.stream().filter(this::isFollowUpCard).findFirst().orElse(null);
            if (gyPayoff != null) {
                routes.add(card.getName() + " -> dumps resources so " + gyPayoff.getName() + " becomes live.");
            }
        }

        if (description.contains("banish")) {
            Card banishSupport = relatedCards.stream()
                    .filter(c -> safeLower(c.getDescription()).contains("banish") || safeLower(c.getName()).contains("banish"))
                    .findFirst()
                    .orElse(null);
            if (banishSupport != null) {
                routes.add(card.getName() + " -> use " + banishSupport.getName() + " to turn the banish effect into follow-up.");
            }
        }

        if (description.contains("negate")) {
            Card interaction = relatedCards.stream().filter(c -> safeLower(c.getDescription()).contains("negate")).findFirst().orElse(null);
            if (interaction != null) {
                routes.add(interaction.getName() + " -> protects " + card.getName() + " by covering the interaction step.");
            }
        }

        return routes;
    }

    private String archetypeLabel(Card card) {
        return card.getArchetype() == null || card.getArchetype().isBlank() ? "deck" : card.getArchetype();
    }

    private String joinCardNames(List<Card> cards) {
        return cards.stream().map(Card::getName).collect(Collectors.joining(" / "));
    }

    private boolean isSpellOrTrap(Card card) {
        String type = safeLower(card.getType());
        return type.contains("spell") || type.contains("trap");
    }

    private boolean isStarterCard(Card card) {
        String desc = safeLower(card.getDescription());
        return desc.contains("search") || desc.contains("add to hand") || desc.contains("draw") || desc.contains("reveal");
    }

    private boolean isExtenderCard(Card card) {
        String desc = safeLower(card.getDescription());
        return desc.contains("special summon") || desc.contains("from your hand") || desc.contains("from your graveyard") || desc.contains("extra deck");
    }

    private boolean isPayoffCard(Card card) {
        String type = safeLower(card.getType());
        return type.contains("fusion") || type.contains("synchro") || type.contains("xyz") || type.contains("link") || type.contains("ritual");
    }

    private boolean isFollowUpCard(Card card) {
        String desc = safeLower(card.getDescription());
        return desc.contains("graveyard") || desc.contains("end phase") || desc.contains("set 1") || desc.contains("add to your hand") || desc.contains("search");
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
}
