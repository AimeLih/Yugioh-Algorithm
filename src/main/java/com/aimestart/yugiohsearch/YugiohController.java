package com.aimestart.yugiohsearch;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/yugioh")

public class YugiohController {

    private final YugiohService yugiohService;

    public YugiohController(YugiohService yugiohService) {
        this.yugiohService = yugiohService;

    }


    @PostMapping("/import")
    public List<Card> importAllCards(){
        return yugiohService.importAllCards();
    }
    @GetMapping("/card")
    public Card getCardByName(@RequestParam String name){
        return yugiohService.getCardByName(name);
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
}
