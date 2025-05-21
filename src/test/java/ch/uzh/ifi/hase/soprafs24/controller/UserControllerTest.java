package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPasswordUpdateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserService userService;

    // helper to serialize any DTO to JSON
    private String toJson(Object dto) throws Exception {
        return mapper.writeValueAsString(dto);
    }

    @Nested @DisplayName("GET /users/{id}")
    class GetUserById {

        @Test @DisplayName("200 when user exists")
        void whenExists_returnsDto() throws Exception {
            User u = new User();
            u.setId(42L);
            u.setName("Alice");
            u.setUsername("alice");
            u.setStatus(UserStatus.ONLINE);
            u.setToken(UUID.randomUUID().toString());
            u.setCreationDate(new Date());

            given(userService.getUserById(42L)).willReturn(u);

            mockMvc.perform(get("/users/{id}", 42L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }

        @Test @DisplayName("404 when not found")
        void whenNotFound_responds404() throws Exception {
            given(userService.getUserById(99L))
                    .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            mockMvc.perform(get("/users/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested @DisplayName("PUT /users/{id}")
    class UpdateUserInfo {

        @Test @DisplayName("204 on valid input")
        void validInput_returnsNoContent() throws Exception {
            UserPostDTO dto = new UserPostDTO();
            dto.setName("Bob");
            dto.setUsername("bob123");

            mockMvc.perform(put("/users/{id}", 5L)
                            .contentType("application/json")
                            .content(toJson(dto)))
                    .andExpect(status().isNoContent());

            then(userService).should().updateUser(eq(5L), any(User.class));
        }
    }

    @Nested @DisplayName("PUT /users/{id}/password")
    class UpdatePassword {

        @Test @DisplayName("204 on success")
        void success_returnsNoContent() throws Exception {
            UserPasswordUpdateDTO dto = new UserPasswordUpdateDTO();
            dto.setOldPassword("old");
            dto.setNewPassword("new!");

            mockMvc.perform(put("/users/{id}/password", 7L)
                            .contentType("application/json")
                            .content(toJson(dto)))
                    .andExpect(status().isNoContent());

            then(userService)
                    .should()
                    .updatePassword(7L, "old", "new!");
        }
    }

    @Nested @DisplayName("GET /users")
    class GetAllUsers {

        @Test @DisplayName("200 returns list of DTOs")
        void returnsList() throws Exception {
            User u = new User();
            u.setId(1L);
            u.setName("C");
            u.setUsername("cuser");
            u.setStatus(UserStatus.OFFLINE);
            u.setToken("t");
            u.setCreationDate(new Date());

            given(userService.getUsers()).willReturn(List.of(u));

            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("C"))
                    .andExpect(jsonPath("$[0].username").value("cuser"))
                    .andExpect(jsonPath("$[0].status").value("OFFLINE"));
        }
    }

    @Nested @DisplayName("POST /users (createUser)")
    class CreateUser {

        @Test @DisplayName("201 returns created DTO")
        void returnsCreatedDto() throws Exception {
            UserPostDTO in = new UserPostDTO();
            in.setName("D");
            in.setUsername("duser");

            User created = new User();
            created.setId(2L);
            created.setName("D");
            created.setUsername("duser");
            created.setStatus(UserStatus.ONLINE);
            created.setToken("tok");
            created.setCreationDate(new Date());

            given(userService.createUser(any(User.class))).willReturn(created);

            mockMvc.perform(post("/users")
                            .contentType("application/json")
                            .content(toJson(in)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("D"))
                    .andExpect(jsonPath("$.username").value("duser"))
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }
    }

    @Nested @DisplayName("POST /login")
    class LoginUser {

        @Test @DisplayName("200 on valid credentials")
        void validCredentials_returnsDto() throws Exception {
            UserLoginDTO login = new UserLoginDTO();
            login.setUsername("zz");
            login.setPassword("pp");

            User u = new User();
            u.setId(3L);
            u.setName("Z Z");
            u.setUsername("zz");
            u.setStatus(UserStatus.ONLINE);
            u.setToken("tok");
            u.setCreationDate(new Date());

            given(userService.loginUser("zz","pp")).willReturn(u);

            mockMvc.perform(post("/login")
                            .contentType("application/json")
                            .content(toJson(login)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.username").value("zz"))
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }

        @Test @DisplayName("401 on bad credentials")
        void badCredentials_returnsUnauthorized() throws Exception {
            UserLoginDTO login = new UserLoginDTO();
            login.setUsername("x");
            login.setPassword("y");

            given(userService.loginUser("x","y"))
                    .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid"));

            mockMvc.perform(post("/login")
                            .contentType("application/json")
                            .content(toJson(login)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested @DisplayName("DELETE /users/logout/{id}")
    class LogoutUser {

        @Test @DisplayName("200 returns updated DTO")
        void returnsDto() throws Exception {
            User u = new User();
            u.setId(8L);
            u.setName("Lo");
            u.setUsername("lo");
            u.setStatus(UserStatus.OFFLINE);
            u.setToken(null);
            u.setCreationDate(new Date());

            given(userService.logoutUser(8L)).willReturn(u);

            mockMvc.perform(delete("/users/logout/{id}", 8L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(8))
                    .andExpect(jsonPath("$.username").value("lo"))
                    .andExpect(jsonPath("$.status").value("OFFLINE"));
        }
    }

    @Nested @DisplayName("POST /register")
    class RegisterUser {

        @Test @DisplayName("201 same as createUser")
        void worksLikeCreate() throws Exception {
            UserPostDTO in = new UserPostDTO();
            in.setName("R");
            in.setUsername("ruser");

            User created = new User();
            created.setId(9L);
            created.setName("R");
            created.setUsername("ruser");
            created.setStatus(UserStatus.ONLINE);
            created.setToken("tk");
            created.setCreationDate(new Date());

            given(userService.createUser(any(User.class))).willReturn(created);

            mockMvc.perform(post("/register")
                            .contentType("application/json")
                            .content(toJson(in)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.username").value("ruser"))
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }
    }
}
