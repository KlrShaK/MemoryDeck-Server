package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.FlashcardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;

    public FlashcardService(FlashcardRepository flashcardRepository,
                            UserRepository userRepository,
                            DeckRepository deckRepository) {
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
    }

    public List<Deck> getDecks(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new ArrayList<>(user.getDecks());
    }

    public List<Deck> getPublicDecks() {
        return deckRepository.findByIsPublicTrue();
    }

    public Deck getDeckById(Long deckId) {
        Optional<Deck> existingDeckOpt = deckRepository.findById(deckId);
        if (existingDeckOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found");
        }
        Deck existingDeck = existingDeckOpt.get();

        return existingDeck;
    }


    // @Transactional
    public Deck createDeck(Long userId, Deck deck) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        deck.setUser(user);

        return deckRepository.save(deck);
    }


    public void updateDeck(Long id, Deck updatedDeck) {
        Deck existingDeck = getDeckById(id);
        existingDeck.setTitle(updatedDeck.getTitle());
        existingDeck.setDeckCategory(updatedDeck.getDeckCategory());
        existingDeck.setIsPublic(updatedDeck.getIsPublic());

        List<Flashcard> flashcards = existingDeck.getFlashcards();

        flashcardRepository.saveAll(flashcards);
        deckRepository.save(existingDeck);
    }

    public void deleteDeck(Long id) {
        if (!deckRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found");
        }
        deckRepository.deleteById(id);
    }







}
