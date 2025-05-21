package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.DeckDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FlashcardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DeckMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.FlashcardMapper;
import ch.uzh.ifi.hase.soprafs24.service.FlashcardService;
import ch.uzh.ifi.hase.soprafs24.service.GoogleCloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlashcardController.class)
class FlashcardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private FlashcardService          flashcardService;
    @MockBean private GoogleCloudStorageService googleCloudStorageService;
    @MockBean private FlashcardMapper           flashcardMapper;
    @MockBean private DeckMapper                deckMapper;

    // ────────────────────────────────────────────────────────────────────────
    @Nested
    class DeckEndpoints {

        @Test
        void getDecksForUser_returnsDtoList() throws Exception {
            Deck deck = new Deck();
            deck.setId(1L);
            deck.setTitle("D1");
            deck.setDeckCategory(FlashcardCategory.HISTORY);
            deck.setIsPublic(false);
            deck.setIsAiGenerated(true);
            deck.setAiPrompt("p1");
            deck.setFlashcards(List.of());

            DeckDTO dto = new DeckDTO();
            dto.setId(1L);
            dto.setTitle("D1");
            dto.setDeckCategory(FlashcardCategory.HISTORY);
            dto.setIsPublic(false);
            dto.setIsAiGenerated(true);
            dto.setAiPrompt("p1");
            dto.setFlashcards(List.of());

            when(flashcardService.getDecks(10L)).thenReturn(List.of(deck));
            when(deckMapper.toDTOList(anyList())).thenReturn(List.of(dto));

            mockMvc.perform(get("/decks").param("userId","10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("D1"))
                    .andExpect(jsonPath("$[0].deckCategory").value("HISTORY"))
                    .andExpect(jsonPath("$[0].isAiGenerated").value(true));
        }

        @Test
        void getDeckById_returnsDto() throws Exception {
            Deck deck = new Deck();
            deck.setId(2L);
            deck.setTitle("D2");
            DeckDTO dto = new DeckDTO();
            dto.setId(2L);
            dto.setTitle("D2");

            when(flashcardService.getDeckById(2L)).thenReturn(deck);
            when(deckMapper.toDTO(any(Deck.class))).thenReturn(dto);

            mockMvc.perform(get("/decks/{id}",2L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.title").value("D2"));
        }

        @Test
        void getPublicDecks_returnsDtoList() throws Exception {
            Deck deck = new Deck();
            deck.setId(3L);
            deck.setIsPublic(true);
            DeckDTO dto = new DeckDTO();
            dto.setId(3L);
            dto.setIsPublic(true);

            when(flashcardService.getPublicDecks()).thenReturn(List.of(deck));
            when(deckMapper.toDTOList(anyList())).thenReturn(List.of(dto));

            mockMvc.perform(get("/decks/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(3))
                    .andExpect(jsonPath("$[0].isPublic").value(true));
        }

        @Nested
        class CreateDeckBranching {
            @Test
            void createDeck_aiFalse_usesZero() throws Exception {
                DeckDTO dto = new DeckDTO();
                dto.setIsAiGenerated(false);

                Deck entity = new Deck();
                when(deckMapper.toEntity(any(DeckDTO.class))).thenReturn(entity);
                when(flashcardService.createDeck(eq(5L), eq(entity), eq(0))).thenReturn(entity);
                when(deckMapper.toDTO(any(Deck.class))).thenReturn(new DeckDTO());

                mockMvc.perform(post("/decks/addDeck")
                                .param("userId","5")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());

                verify(flashcardService).createDeck(5L, entity, 0);
            }

            @Test
            void createDeck_aiTrueNullNumber_usesDefault5() throws Exception {
                DeckDTO dto = new DeckDTO();
                dto.setIsAiGenerated(true);
                dto.setNumberOfAICards(null);

                Deck entity = new Deck();
                when(deckMapper.toEntity(any(DeckDTO.class))).thenReturn(entity);
                when(flashcardService.createDeck(eq(6L), eq(entity), eq(5))).thenReturn(entity);
                when(deckMapper.toDTO(any(Deck.class))).thenReturn(new DeckDTO());

                mockMvc.perform(post("/decks/addDeck")
                                .param("userId","6")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());

                verify(flashcardService).createDeck(6L, entity, 5);
            }

            @Test
            void createDeck_aiTrueWithNumber_usesThat() throws Exception {
                DeckDTO dto = new DeckDTO();
                dto.setIsAiGenerated(true);
                dto.setNumberOfAICards(7);

                Deck entity = new Deck();
                when(deckMapper.toEntity(any(DeckDTO.class))).thenReturn(entity);
                when(flashcardService.createDeck(eq(8L), eq(entity), eq(7))).thenReturn(entity);
                when(deckMapper.toDTO(any(Deck.class))).thenReturn(new DeckDTO());

                mockMvc.perform(post("/decks/addDeck")
                                .param("userId","8")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());

                verify(flashcardService).createDeck(8L, entity, 7);
            }
        }

        @Test
        void updateDeck_delegatesAndReturnsNoContent() throws Exception {
            DeckDTO dto = new DeckDTO();
            Deck entity = new Deck();

            when(deckMapper.toEntity(any(DeckDTO.class))).thenReturn(entity);
            doNothing().when(flashcardService).updateDeck(eq(9L), eq(entity));

            mockMvc.perform(put("/decks/{id}",9L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNoContent());

            verify(flashcardService).updateDeck(9L, entity);
        }

        @Test
        void deleteDeck_delegatesAndReturnsOk() throws Exception {
            doNothing().when(flashcardService).deleteDeck(10L);

            mockMvc.perform(delete("/decks/{id}",10L))
                    .andExpect(status().isOk());

            verify(flashcardService).deleteDeck(10L);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested
    class FlashcardEndpoints {

        @Test
        void getAllFlashcardsForDeck_returnsDtoList() throws Exception {
            Flashcard fc = new Flashcard();
            fc.setId(11L);
            FlashcardDTO dto = new FlashcardDTO();
            dto.setId(11L);

            when(flashcardService.getAllFlashcardsForDeck(12L)).thenReturn(List.of(fc));
            when(flashcardMapper.toDTOList(anyList())).thenReturn(List.of(dto));

            mockMvc.perform(get("/decks/{deckId}/flashcards",12L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(11));
        }

        @Test
        void getFlashcardById_returnsDto() throws Exception {
            Flashcard fc = new Flashcard();
            fc.setId(13L);
            FlashcardDTO dto = new FlashcardDTO();
            dto.setId(13L);

            when(flashcardService.getCardById(14L)).thenReturn(fc);
            when(flashcardMapper.toDTO(any(Flashcard.class))).thenReturn(dto);

            mockMvc.perform(get("/flashcards/{id}",14L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(13));
        }

        @Test
        void updateFlashcardInfo_delegatesAndReturnsNoContent() throws Exception {
            FlashcardDTO dto = new FlashcardDTO();
            Flashcard entity = new Flashcard();

            when(flashcardMapper.toEntity(any(FlashcardDTO.class))).thenReturn(entity);
            doNothing().when(flashcardService).updateFlashcard(eq(15L), eq(entity));

            mockMvc.perform(put("/flashcards/{id}",15L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNoContent());

            verify(flashcardService).updateFlashcard(15L, entity);
        }

        @Test
        void deleteFlashcard_delegatesAndReturnsOk() throws Exception {
            doNothing().when(flashcardService).deleteFlashcard(16L);

            mockMvc.perform(delete("/decks/{deckId}/flashcards/{id}",100L,16L))
                    .andExpect(status().isOk());

            verify(flashcardService).deleteFlashcard(16L);
        }

        @Test
        void createFlashcard_returnsCreatedDto() throws Exception {
            FlashcardDTO dto = new FlashcardDTO();
            Flashcard entity = new Flashcard();
            Flashcard created = new Flashcard();
            created.setId(17L);
            FlashcardDTO out = new FlashcardDTO();
            out.setId(17L);

            when(flashcardMapper.toEntity(any(FlashcardDTO.class))).thenReturn(entity);
            when(flashcardService.createFlashcard(eq(101L), eq(entity))).thenReturn(created);
            when(flashcardMapper.toDTO(any(Flashcard.class))).thenReturn(out);

            mockMvc.perform(post("/decks/{deckId}/flashcards/addFlashcard",101L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(17));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested
    class ImageEndpoints {

        @Test
        void uploadImage_emptyFile_returnsBadRequest() throws Exception {
            MockMultipartFile empty = new MockMultipartFile(
                    "file","empty.txt","text/plain",new byte[0]
            );

            mockMvc.perform(multipart("/flashcards/upload-image").file(empty))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("No file uploaded"));
        }

        @Test
        void uploadImage_ioException_returnsServerError() throws Exception {
            MockMultipartFile faulty = new MockMultipartFile(
                    "file", "f.png", "image/png", "data".getBytes()
            ) {
                @Override
                public byte[] getBytes() throws IOException {
                    throw new IOException("fail");
                }
            };

            mockMvc.perform(multipart("/flashcards/upload-image").file(faulty))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Failed to upload image"));
        }

        @Test
        void uploadImage_success_returnsUrl() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file","i.png","image/png","x".getBytes()
            );
            when(googleCloudStorageService.uploadFile(
                    any(), anyString()
            )).thenReturn("url1");

            mockMvc.perform(multipart("/flashcards/upload-image").file(file))
                    .andExpect(status().isOk())
                    .andExpect(content().string("url1"));
        }

        @Test
        void deleteImage_noUrl_returnsBadRequest() throws Exception {
            mockMvc.perform(delete("/flashcards/delete-image")
                            .param("imageUrl"," "))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Image URL must be provided"));
        }

        @Test
        void deleteImage_notFound_returns404() throws Exception {
            when(googleCloudStorageService.deleteFile("u1")).thenReturn(false);

            mockMvc.perform(delete("/flashcards/delete-image")
                            .param("imageUrl","u1"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("Image not found or already deleted"));
            verify(flashcardService, never()).removeImageFromFlashcard(any());
        }

        @Test
        void deleteImage_success_callsService_andReturnsOk() throws Exception {
            when(googleCloudStorageService.deleteFile("u2")).thenReturn(true);

            mockMvc.perform(delete("/flashcards/delete-image")
                            .param("imageUrl","u2"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Image deleted successfully"));
            verify(flashcardService).removeImageFromFlashcard("u2");
        }

        @Test
        void deleteImage_exception_returnsServerError() throws Exception {
            when(googleCloudStorageService.deleteFile("bad"))
                    .thenThrow(RuntimeException.class);

            mockMvc.perform(delete("/flashcards/delete-image")
                            .param("imageUrl","bad"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Failed to delete image"));
        }

        @Test
        void getFlashcardImage_notFound() throws Exception {
            when(googleCloudStorageService.getFileFromBucket("fileX"))
                    .thenReturn(null);

            mockMvc.perform(get("/flashcards/image")
                            .param("imageUrl","http://.../fileX"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getFlashcardImage_success_streamsBytesAndContentType() throws Exception {
            Blob blob = mock(Blob.class);
            when(blob.getContent()).thenReturn("abc".getBytes());
            when(blob.getContentType()).thenReturn("image/jpg");
            when(googleCloudStorageService.getFileFromBucket("fileY"))
                    .thenReturn(blob);

            mockMvc.perform(get("/flashcards/image")
                            .param("imageUrl","https://foo/bar/fileY"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            "Content-Type","image/jpg"
                    ))
                    .andExpect(content().bytes("abc".getBytes()));
        }
    }
}
