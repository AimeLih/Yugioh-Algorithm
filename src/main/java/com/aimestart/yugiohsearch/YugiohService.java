package com.aimestart.yugiohsearch;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class YugiohService {

    private final RestClient restClient;
    private final CardRepository cardRepository;

    public record CardInfoResponse(List<CardData> data) {}

    public record CardData(String name, String desc, String type, Integer atk, Integer def, Integer level,
                           String race, String attribute, Integer linkval, String archetype, String [] linkmarkers, String Staple, Integer scale) {}

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

    public List<Card> importAllCards() {
        List<CardData> apiCards = fetchallCards();
        List<Card> cardsToSave = new ArrayList<>();
        for (CardData card : apiCards) {
            if (!cardRepository.existsByName(card.name())) {
                cardsToSave.add(new Card(card.name(), card.desc(), card.type(), 0));
            }
        }
        return cardRepository.saveAll(cardsToSave);
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

    public void updateExistingCards() {
        List<Card> cards = cardRepository.findAll();
        Map<String, CardData> apiCards = fetchallCards().stream()
                .collect(Collectors.toMap(CardData::name, Function.identity()));

       for (Card card : cards) {
           if(card.isStaple() == null){
               card.setStaple(true);
               cardRepository.save(card);
           }
           /* CardData apiCard = apiCards.get(card.getName());

            if (apiCard == null) {
                continue;
            }
                if("yes".equalsIgnoreCase(apiCard.Staple())) {
                    card.setStaple(true);
                } else {
                    card.setStaple(false);
                }

                if(card.getType().contains("Pendulum")){
                    card.setScale(apiCard.scale());
                    cardRepository.save(card);
                }
                if(card.getType().contains("Link") && card.getType().contains("Monster")){
                    card.setLinkvalue(apiCard.linkval());
                    card.setLinkmarkers(List.of(apiCard.linkmarkers()));
                    if(apiCard.archetype() != null) {
                        card.setArchetype(apiCard.archetype());
                    }
                    cardRepository.save(card);
                    continue;
                }
                if(card.getType().contains("Monster")){
                    card.setAtk(apiCard.atk());
                    card.setDef(apiCard.def());
                    card.setLevel(apiCard.level());
                    card.setRace(apiCard.race());
                    card.setAttribute(apiCard.attribute());
                    if(apiCard.archetype() != null) {
                        card.setArchetype(apiCard.archetype());
                    }
                    cardRepository.save(card);
                    continue;
                }
                if(card.getType().contains("Spell") || card.getType().contains("Trap")) {
                    card.setRace(apiCard.race());
                    if (apiCard.archetype() != null) {
                        card.setArchetype(apiCard.archetype());
                    }
                    cardRepository.save(card);
                }
*/
        }
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

    public Card getCardByName(String name) {
        return cardRepository.getCardByName(name);
    }

    public List<Card> getCardsBySubstring(String name) {
        String cardname = name;
        List<Card> cards = new ArrayList<>();
        for (Card card : cardRepository.findAll()) {
            if (card.getName().contains(cardname)) {
                cards.add(card);
            }
        }
        if (cards.isEmpty()) {
            throw new RuntimeException("No cards found with that name");
        }
        return cards;
    }

    public String ifExtender(Card focusedcard) {
        String desc = focusedcard.getDescription().toLowerCase();
        if (Pattern.compile("summon\\s+\\d+").matcher(desc).find()) {
            return "summon extender";
        }
        if (Pattern.compile("add\\s+\\d+").matcher(desc).find()) {
            return "add extender";
        }
        return "not an extender";
    }
}
