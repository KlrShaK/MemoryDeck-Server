package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.DeckDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FlashcardDTO;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DeckMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.FlashcardMapper;
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
        // If numberOfCards is not provided, default to 5 when AI generation is enabled
        System.out.println("NUMBER OF AI CARDS: " + deckDTO.getNumberofAICards());
        int numberOfCards = (deckDTO.getIsAiGenerated() != null && deckDTO.getIsAiGenerated() && deckDTO.getNumberofAICards() != null)
                ? deckDTO.getNumberofAICards()
                : ((deckDTO.getIsAiGenerated() != null && deckDTO.getIsAiGenerated()) ? 5 : 0);
        Deck createdDeck = flashcardService.createDeck(userId, deck, numberOfCards);
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

    @GetMapping("/decks/{deckId}/flashcards")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<FlashcardDTO> getAllFlashcardsForDeck(@PathVariable Long deckId) {
        List<Flashcard> flashcards = flashcardService.getAllFlashcardsForDeck(deckId);
        return FlashcardMapper.toDTOList(flashcards);
    }

    @GetMapping("/flashcards/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public FlashcardDTO getFlashcardById(@PathVariable Long id) {
        // Fetch user from the service layer
        Flashcard flashcard = flashcardService.getCardById(id);

        // Convert entity to DTO and return
        return FlashcardMapper.toDTO(flashcard);
    }

    @PutMapping("/flashcards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void updateFlashcardInfo(@PathVariable Long id, @RequestBody Flashcard updatedFlashcard) {
        updatedFlashcard.setId(id); // Ensure the ID is correctly set
        flashcardService.updateFlashcard(updatedFlashcard);
    }

    @DeleteMapping("/decks/{deckId}/flashcards/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void deleteFlashcard(@PathVariable Long id,@PathVariable Long deckId) {
        flashcardService.deleteFlashcard(id);
    }





}
    

