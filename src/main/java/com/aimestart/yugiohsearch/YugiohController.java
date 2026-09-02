package com.aimestart.yugiohsearch;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/yugioh")
@CrossOrigin(originPatterns = {"http://localhost:5173",
        "https://yugioh-combo.vercel.app"
})


public class YugiohController {

    private static final String IMPORT_TOKEN_HEADER = "X-Import-Token";

    @Value("${import.token}")
    private String importToken;

    private final YugiohService yugiohService;

    public YugiohController(YugiohService yugiohService) {
        this.yugiohService = yugiohService;
    }
    //imports all cards from the yugioh api database into the Neon database
    @PostMapping("/import")
    public void importAllCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.importAllCards();
    }
    //gets the specific image of a card
    @GetMapping("/card/image")
    public String getImage(@RequestParam String name){
        Card card = yugiohService.getCardByName(name);
        return yugiohService.getImage(card);
    }
    //returns the info of a specific card
    @GetMapping("/card")
    public Card getCardByName(@RequestParam String name){
        return yugiohService.getCardByName(name);
    }
    //Combo logic
    @GetMapping("/card/combos")
    public List<YugiohService.ComboOption> getPossibleCombos(
            @RequestParam String name,
            @RequestParam(required = false) String zone
    ) {
        return yugiohService.getPossibleCombos(name, zone);
    }
    //Fusion logic
    @GetMapping("/card/fusion-materials")
    public YugiohService.FusionMaterialPlan getFusionMaterials(
            @RequestParam String source,
            @RequestParam String target
    ) {
        return yugiohService.getFusionMaterialPlan(source, target);
    }
    //card cost logic
    @GetMapping("/card/cost-materials")
    public YugiohService.FusionMaterialPlan getCostMaterials(
            @RequestParam String source,
            @RequestParam String target
    ) {
        return yugiohService.getCardCostPlan(source, target);
    }
    //returns cards by substrings
    @GetMapping("/card/substring")
    public List<Card> getCardBySubstring(@RequestParam String name){
        return yugiohService.getCardsBySubstring(name);
    }
    //returns all cards in the database
    @Cacheable("Cards")
    @GetMapping("/card/all")
    public List<Card> getAllCards(){
        return yugiohService.getAllCards();
    }
    //updates a cards weight
    @PutMapping("/card/update")
    public void updatingCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.updateExistingCardsWeight();
    }
    //updates cards if their info is outdated - very unlikely to ever happen
    @PutMapping("/card/update/database")
    public void updatingExistingCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.updateExistingCards();
    }
    //checks if its an extender
    @GetMapping("/card/pattern")
    public String patternCard(@RequestParam String name){
        return yugiohService.ifExtender(name);
    }
    //checks for onceperturn
    @GetMapping("/card/onceprturn")
    public String isOncePerTurn(@RequestParam String name){
        return yugiohService.isOncePerTurn(name);
    }

    //puts all cards weight to 0
    @PutMapping("/card/update/zero")
    public void allCardWeightZero(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.allCardWeightZero();
    }

    public record ImportResult(int cardsAdded) {}
    //imports new cards from the yugioh api database into the Neon database
    @PostMapping("/admin/import")
    public ImportResult importNewCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false)
            String providedToken
    ) {
        requireImportToken(providedToken);
        return new ImportResult(yugiohService.importNewCards());
    }

    private void requireImportToken(String providedToken) {
        if (importToken.isBlank()
                || providedToken == null
                || !importToken.equals(providedToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid import token"
            );
        }
    }


}
