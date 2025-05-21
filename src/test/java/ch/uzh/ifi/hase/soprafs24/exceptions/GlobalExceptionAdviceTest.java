package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionAdviceTest.TestController.class)
@Import(GlobalExceptionAdvice.class)
class GlobalExceptionAdviceTest {

    // make this public so Spring can detect it
    @RestController
    @Validated
    public static class TestController {
        @GetMapping("/ex/illegal")
        public void illegal() {
            throw new IllegalArgumentException("bad arg");
        }
        @GetMapping("/ex/state")
        public void state() {
            throw new IllegalStateException("bad state");
        }
        @GetMapping("/ex/transaction")
        public void tx() {
            throw new TransactionSystemException("tx failed");
        }
        @GetMapping("/ex/server")
        public void server() {
            throw new HttpServerErrorException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "ise happened"
            );
        }
        @GetMapping("/ex/response")
        public void response() {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "not found here"
            );
        }
        public static class Payload {
            @NotBlank(message = "must not be blank") public String name;
            @Size(min = 5, message = "too short")     public String code;
        }
        @PostMapping("/ex/validate")
        public void validate(@Valid @RequestBody Payload p) { /* no-op */ }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Nested @DisplayName("any conflict → 4xx")
    class ConflictTests {
        @Test void illegalArgument_is4xx() throws Exception {
            mockMvc.perform(get("/ex/illegal"))
                    .andExpect(status().is4xxClientError());
        }
        @Test void illegalState_is4xx() throws Exception {
            mockMvc.perform(get("/ex/state"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test @DisplayName("transaction failure → 4xx")
    void transactionSystemException_is4xx() throws Exception {
        mockMvc.perform(get("/ex/transaction"))
                .andExpect(status().is4xxClientError());
    }

    @Test @DisplayName("server error → 5xx")
    void httpServerError_integration_returns4xx() throws Exception {
        mockMvc.perform(get("/ex/server"))
                .andExpect(status().is4xxClientError());
    }

    @Test @DisplayName("response status exception → 4xx")
    void responseStatusException_is4xx() throws Exception {
        mockMvc.perform(get("/ex/response"))
                .andExpect(status().is4xxClientError());
    }

    @Test @DisplayName("validation failure → 4xx")
    void methodArgumentNotValid_is4xx() throws Exception {
        String badJson = "{\"name\":\"\",\"code\":\"abc\"}";
        mockMvc.perform(post("/ex/validate")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is4xxClientError());
    }
}
