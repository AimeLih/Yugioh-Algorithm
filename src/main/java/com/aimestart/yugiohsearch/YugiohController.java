package com.aimestart.yugiohsearch;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/yugioh")
@CrossOrigin(originPatterns = {"http://localhost:5173"})
public class YugiohController {

    private final YugiohService yugiohService;

    public YugiohController(YugiohService yugiohService) {
        this.yugiohService = yugiohService;
    }

    @PostMapping("/import")
    public void importAllCards(){
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
    public List<YugiohService.ComboOption> getPossibleCombos(@RequestParam String name) {
        return yugiohService.getPossibleCombos(name);
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
    public void updatingCards(){
        yugiohService.updateExistingCardsWeight();
    }

    @PutMapping("/card/update/database")
    public void updatingExistingCards(){
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
    public void allCardWeightZero(){
        yugiohService.allCardWeightZero();
    }
}
