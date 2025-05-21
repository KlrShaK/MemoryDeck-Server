package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

public class ChatGptServiceTest {

    private ChatGptService chatGptService;

    @BeforeEach
    public void setUp() {
        chatGptService = new ChatGptService();
    }

    /**
     * Dummy function to simulate the ChatGPT API response for generating flashcards.
     * Returns a JSON string wrapped as expected by extractGeneratedText.
     */
    private String dummyGenerateFlashcardsResponse() {
        // 1) Build your flashcards JSON as normal (with real newlines for readability)
        String dummy =
                "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"description\": \"What is the capital of France?\",\n" +
                        "      \"answer\": \"Paris\",\n" +
                        "      \"wrong_answers\": [\"Berlin\", \"Madrid\", \"Rome\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        // 2) Escape backslashes first (if there were any), then quotes, then newlines
        String escaped = dummy
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        // 3) Wrap into your API‐style envelope
        return "{\"output\":[{\"content\":[{\"text\":\"" + escaped + "\"}]}]}";
    }


    @Test
    public void extractGeneratedText_dummyResponse_parsedCorrectly() {
        String responseJson = dummyGenerateFlashcardsResponse();
        String extracted = chatGptService.extractGeneratedText(responseJson);

        String expected =
                "{\n" +
                        "  \"flashcards\": [\n" +
                        "    {\n" +
                        "      \"description\": \"What is the capital of France?\",\n" +
                        "      \"answer\": \"Paris\",\n" +
                        "      \"wrong_answers\": [\"Berlin\", \"Madrid\", \"Rome\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        assertEquals(expected, extracted);
    }

    @Test
    public void extractGeneratedText_missingFlashcards_throwsException() {
        // Content JSON without the "flashcards" key
        String invalidContent = "{ \"foo\": [] }";
        String wrapper = "{\"output\":[{\"content\":[{\"text\":\"" + invalidContent.replace("\"", "\\\"") + "\"}]}]}";

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> chatGptService.extractGeneratedText(wrapper));
        assertTrue(exception.getMessage().contains("does not contain a 'flashcards' key"));
    }

    @Test
    public void extractGeneratedText_malformedJson_throwsException() {
        String badJson = "not a json";

        assertThrows(ResponseStatusException.class,
                () -> chatGptService.extractGeneratedText(badJson));
    }
}
