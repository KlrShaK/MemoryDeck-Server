package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.DeckDTO;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DeckMapper;
import ch.uzh.ifi.hase.soprafs24.service.FlashcardService;


import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    @GetMapping("/decks")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DeckDTO> getDecksForUser(@RequestParam Long userId) {
        List<Deck> decks = flashcardService.getDecks(userId);
        return DeckMapper.toDTOList(decks);
    }

    @GetMapping("/decks/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public DeckDTO getDeckById(@PathVariable Long id) {
        // Fetch user from the service layer
        Deck deck = flashcardService.getDeckById(id);

        // Convert entity to DTO and return
        return DeckMapper.toDTO(deck);
    }

    @GetMapping("/decks/public")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DeckDTO> getPublicDecks() {
        List<Deck> publicDecks = flashcardService.getPublicDecks();
        return DeckMapper.toDTOList(publicDecks);
    }

    @PostMapping("/decks/addDeck")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public DeckDTO createDeck(@RequestParam Long userId, @Valid @RequestBody DeckDTO deckDTO) {
        Deck deck = DeckMapper.toEntity(deckDTO);

        Deck createdDeck = flashcardService.createDeck(userId, deck);
        return DeckMapper.toDTO(createdDeck);
    }

    @PutMapping("/decks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void updateDeck(@PathVariable Long id, @RequestBody DeckDTO deckDTO) {
        flashcardService.updateDeck(id, DeckMapper.toEntity(deckDTO));
    }

    @DeleteMapping("/decks/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void deleteDeck(@PathVariable Long id) {
        flashcardService.deleteDeck(id);
    }





}
    

