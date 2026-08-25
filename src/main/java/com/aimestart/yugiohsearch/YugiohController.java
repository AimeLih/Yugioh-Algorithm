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

    @PostMapping("/import")
    public void importAllCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.importAllCards();
    }

    @GetMapping("/card/image")
    public String getImage(@RequestParam String name){
        Card card = yugiohService.getCardByName(name);
        return yugiohService.getImage(card);
    }
    @GetMapping("/card")
    public Card getCardByName(@RequestParam String name){
        return yugiohService.getCardByName(name);
    }
    @GetMapping("/card/combos")
    public List<YugiohService.ComboOption> getPossibleCombos(
            @RequestParam String name,
            @RequestParam(required = false) String zone
    ) {
        return yugiohService.getPossibleCombos(name, zone);
    }
    @GetMapping("/card/fusion-materials")
    public YugiohService.FusionMaterialPlan getFusionMaterials(
            @RequestParam String source,
            @RequestParam String target
    ) {
        return yugiohService.getFusionMaterialPlan(source, target);
    }
    @GetMapping("/card/cost-materials")
    public YugiohService.FusionMaterialPlan getCostMaterials(
            @RequestParam String source,
            @RequestParam String target
    ) {
        return yugiohService.getCardCostPlan(source, target);
    }
    @GetMapping("/card/substring")
    public List<Card> getCardBySubstring(@RequestParam String name){
        return yugiohService.getCardsBySubstring(name);
    }

    @Cacheable("Cards")
    @GetMapping("/card/all")
    public List<Card> getAllCards(){
        return yugiohService.getAllCards();
    }
    @PutMapping("/card/update")
    public void updatingCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.updateExistingCardsWeight();
    }

    @PutMapping("/card/update/database")
    public void updatingExistingCards(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.updateExistingCards();
    }

    @GetMapping("/card/pattern")
    public String patternCard(@RequestParam String name){
        return yugiohService.ifExtender(name);
    }

    @GetMapping("/card/onceprturn")
    public String isOncePerTurn(@RequestParam String name){
        return yugiohService.isOncePerTurn(name);
    }


    @PutMapping("/card/update/zero")
    public void allCardWeightZero(
            @RequestHeader(value = IMPORT_TOKEN_HEADER, required = false) String providedToken
    ) {
        requireImportToken(providedToken);
        yugiohService.allCardWeightZero();
    }

    public record ImportResult(int cardsAdded) {}

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
