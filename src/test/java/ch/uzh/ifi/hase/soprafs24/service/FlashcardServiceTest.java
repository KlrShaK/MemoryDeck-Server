package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.FlashcardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlashcardServiceTest {

    @InjectMocks
    private FlashcardService flashcardService;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private ChatGptService chatGptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        flashcardService = new FlashcardService(flashcardRepository, userRepository, deckRepository, chatGptService);
    }


    /**
     * A dummy ChatGPT‐style wrapper that contains
     * an escaped JSON payload under output[0].content[0].text
     */
    private String dummyGenerateFlashcardsResponse() {
        String dummyInner =
                "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"description\": \"What is the capital of France?\",\n" +
                        "      \"answer\": \"Paris\",\n" +
                        "      \"wrong_answers\": [\"Berlin\", \"Madrid\", \"Rome\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        String escaped = dummyInner
                .replace("\\", "\\\\")
                .replace("\"",  "\\\"")
                .replace("\n",  "\\n");
        return "{\"output\":[{\"content\":[{\"text\":\"" + escaped + "\"}]}]}";
    }

    /**
     * Test the private parseFlashcardsFromJson via reflection,
     * for a valid payload.
     */
    @Test
    void parseFlashcardsFromJson_validJson_returnsList() throws Exception {
        String innerJson =
                "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"description\": \"What is the capital of France?\",\n" +
                        "      \"answer\": \"Paris\",\n" +
                        "      \"wrong_answers\": [\"Berlin\", \"Madrid\", \"Rome\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        // reflectively call private method
        Method m = FlashcardService.class
                .getDeclaredMethod("parseFlashcardsFromJson", String.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Flashcard> cards = (List<Flashcard>) m.invoke(flashcardService, innerJson);

        assertEquals(1, cards.size());
        Flashcard c = cards.get(0);
        assertEquals("What is the capital of France?", c.getDescription());
        assertEquals("Paris",                    c.getAnswer());
        assertArrayEquals(new String[]{"Berlin","Madrid","Rome"}, c.getWrongAnswers());
    }

    /**
     * When the JSON is missing a top‐level "flashcards" array,
     * parseFlashcardsFromJson should throw a ResponseStatusException.
     */
    @Test
    void parseFlashcardsFromJson_missingFlashcards_throws() throws Exception {
        String badJson = "{ \"foo\": [] }";

        Method m = FlashcardService.class
                .getDeclaredMethod("parseFlashcardsFromJson", String.class);
        m.setAccessible(true);

        InvocationTargetException ite = assertThrows(
                InvocationTargetException.class,
                () -> m.invoke(flashcardService, badJson)
        );
        assertTrue(
                ite.getCause() instanceof ResponseStatusException,
                "Expected a ResponseStatusException when 'flashcards' key is missing"
        );
    }

    /**
     * Test createDeck(...) in the AI‐generated branch without
     * making any real HTTP calls. We stub ChatGptService to return
     * our dummy wrapper, then stub extractGeneratedText to return
     * the unescaped inner JSON, and finally verify that createDeck
     * populates the Deck.flashcards list.
     */
    @Test
    void createDeck_aiGenerated_success() {
        // 1) stub user lookup
        User u = new User();
        u.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // 2) prepare a deck marked as AI‐generated
        Deck deck = new Deck();
        deck.setTitle("AI Deck");
        deck.setDeckCategory(FlashcardCategory.SCIENCE);
        deck.setIsAiGenerated(true);
        deck.setAiPrompt("some prompt");

        // 3) stub ChatGptService
        String wrapper = dummyGenerateFlashcardsResponse();
        when(chatGptService.generateFlashcards(anyString(), eq(10))).thenReturn(wrapper);

        String innerJson =
                "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"description\": \"What is the capital of France?\",\n" +
                        "      \"answer\": \"Paris\",\n" +
                        "      \"wrong_answers\": [\"Berlin\", \"Madrid\", \"Rome\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        when(chatGptService.extractGeneratedText(wrapper)).thenReturn(innerJson);

        // 4) call under test
        Deck result = flashcardService.createDeck(1L, deck, 10);

        // 5) verify
        assertNotNull(result.getFlashcards());
        assertEquals(1, result.getFlashcards().size());

        Flashcard created = result.getFlashcards().get(0);
        assertEquals("What is the capital of France?", created.getDescription());
        assertEquals("Paris",                    created.getAnswer());
        assertArrayEquals(new String[]{"Berlin","Madrid","Rome"}, created.getWrongAnswers());

        verify(deckRepository).save(deck);
    }

    /**
     * If the userId doesn't exist, createDeck should throw.
     */
    @Test
    void createDeck_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Deck dummy = new Deck();
        assertThrows(
                ResponseStatusException.class,
                () -> flashcardService.createDeck(1L, dummy, 5),
                "Expected a 404 if user not found"
        );
    }


    @Test
    void getDecks_success() {
        User user = new User();
        Deck deck = new Deck();
        user.setDecks(List.of(deck));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        List<Deck> result = flashcardService.getDecks(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getDecks_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> flashcardService.getDecks(1L));
    }

    @Test
    void getPublicDecks_success() {
        Deck d = new Deck();
        when(deckRepository.findByIsPublicTrue()).thenReturn(List.of(d));
        assertEquals(1, flashcardService.getPublicDecks().size());
    }

    @Test
    void getDeckById_success() {
        Deck d = new Deck();
        when(deckRepository.findById(1L)).thenReturn(Optional.of(d));
        assertEquals(d, flashcardService.getDeckById(1L));
    }

    @Test
    void getDeckById_notFound_throws() {
        when(deckRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> flashcardService.getDeckById(1L));
    }

    // NEED TO FIX API KEY FIRST
    // @Test
    // void createDeck_aiGenerated_success() {
    //     Deck deck = new Deck();
    //     deck.setTitle("AI Deck");
    //     deck.setDeckCategory(FlashcardCategory.SCIENCE);
    //     deck.setIsAiGenerated(true);
    //     deck.setAiPrompt("some prompt");

    //     User user = new User();
    //     user.setId(1L);
    //     when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    //     when(chatGptService.generateFlashcards(anyString(), eq(10))).thenReturn("{\"flashcards\":[{\"description\":\"Q1\",\"answer\":\"A1\",\"wrong_answers\":[\"W1\"]}]}");
    //     when(chatGptService.extractGeneratedText(anyString())).thenCallRealMethod(); // override if mocked

    //     Deck savedDeck = new Deck();
    //     when(deckRepository.save(any(Deck.class))).thenReturn(savedDeck);

    //     Deck result = flashcardService.createDeck(1L, deck, 10);
    //     assertNotNull(result);
    // }

    @Test
    void updateDeck_success() {
        Deck existing = new Deck();
        existing.setTitle("Old");
        existing.setDeckCategory(FlashcardCategory.HISTORY);
        existing.setFlashcards(List.of(new Flashcard()));
        when(deckRepository.findById(1L)).thenReturn(Optional.of(existing));

        Deck updated = new Deck();
        updated.setTitle("New");
        updated.setDeckCategory(FlashcardCategory.SCIENCE);
        updated.setIsPublic(true);

        flashcardService.updateDeck(1L, updated);
        verify(flashcardRepository).saveAll(anyList());
        verify(deckRepository).save(existing);
    }

    @Test
    void deleteDeck_success() {
        when(deckRepository.existsById(1L)).thenReturn(true);
        flashcardService.deleteDeck(1L);
        verify(deckRepository).deleteById(1L);
    }

    @Test
    void deleteDeck_notFound_throws() {
        when(deckRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> flashcardService.deleteDeck(1L));
    }

    @Test
    void getAllFlashcardsForDeck_success() {
        Deck d = new Deck();
        d.setFlashcards(List.of(new Flashcard()));
        when(deckRepository.findById(1L)).thenReturn(Optional.of(d));
        assertEquals(1, flashcardService.getAllFlashcardsForDeck(1L).size());
    }

    @Test
    void getCardById_success() {
        Flashcard card = new Flashcard();
        when(flashcardRepository.findById(1L)).thenReturn(Optional.of(card));
        assertEquals(card, flashcardService.getCardById(1L));
    }

    @Test
    void getCardById_notFound_throws() {
        when(flashcardRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> flashcardService.getCardById(1L));
    }

    @Test
    void createFlashcard_success() {
        Deck deck = new Deck();
        deck.setDeckCategory(FlashcardCategory.MATH);
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));

        Flashcard card = new Flashcard();
        card.setAnswer("Right");
        card.setWrongAnswers(new String[]{"Wrong1", "Wrong2"});

        when(flashcardRepository.save(any())).thenReturn(card);
        Flashcard result = flashcardService.createFlashcard(1L, card);
        assertEquals(card, result);
    }

    @Test
    void createFlashcard_duplicateAnswer_throws() {
        Deck deck = new Deck();
        deck.setDeckCategory(FlashcardCategory.MATH);
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));

        Flashcard card = new Flashcard();
        card.setAnswer("Duplicate");
        card.setWrongAnswers(new String[]{"Duplicate", "Wrong2"});

        assertThrows(ResponseStatusException.class, () -> flashcardService.createFlashcard(1L, card));
    }

    @Test
    void updateFlashcard_success() {
        Flashcard original = new Flashcard();
        original.setAnswer("OldAnswer");
        original.setWrongAnswers(new String[]{"Wrong1"});

        Flashcard updated = new Flashcard();
        updated.setDescription("Updated");
        updated.setAnswer("NewAnswer");
        updated.setWrongAnswers(new String[]{"NewWrong"});

        when(flashcardRepository.findById(1L)).thenReturn(Optional.of(original));
        flashcardService.updateFlashcard(1L, updated);
        verify(flashcardRepository).save(original);
    }

    @Test
    void updateFlashcard_notFound_throws() {
        when(flashcardRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> flashcardService.updateFlashcard(1L, new Flashcard()));
    }

    @Test
    void deleteFlashcard_success() {
        Flashcard card = new Flashcard();
        when(flashcardRepository.findById(1L)).thenReturn(Optional.of(card));
        flashcardService.deleteFlashcard(1L);
        verify(flashcardRepository).delete(card);
    }

    @Test
    void deleteFlashcard_notFound_throws() {
        when(flashcardRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> flashcardService.deleteFlashcard(1L));
    }

    @Test
    void removeImageFromFlashcard_success() {
        Flashcard card = new Flashcard();
        card.setImageUrl("url");
        when(flashcardRepository.findByImageUrl("url")).thenReturn(card);
        flashcardService.removeImageFromFlashcard("url");
        verify(flashcardRepository).save(card);
        assertNull(card.getImageUrl());
    }

    @Test
    void removeImageFromFlashcard_notFound_doesNothing() {
        when(flashcardRepository.findByImageUrl("url")).thenReturn(null);
        flashcardService.removeImageFromFlashcard("url");
        verify(flashcardRepository, never()).save(any());
    }
}
