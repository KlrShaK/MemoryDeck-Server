package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers all endpoints in UserController:
 *   GET /users/{id}, PUT /users/{id}, GET /users,
 *   POST /users, POST /login, DELETE /users/logout/{id}, POST /register
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    // helper to serialize any DTO to JSON
    private String toJson(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setName("Firstname Lastname");
        user.setUsername("firstname@lastname");
        user.setStatus(UserStatus.OFFLINE);

        given(userService.getUsers()).willReturn(List.of(user));

        // when / then
        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",       hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Firstname Lastname")))
                .andExpect(jsonPath("$[0].username", is("firstname@lastname")))
                .andExpect(jsonPath("$[0].status", is("OFFLINE")));
    }

    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        UserPostDTO in = new UserPostDTO();
        in.setName("Test User");
        in.setUsername("testUsername");

        User created = new User();
        created.setId(1L);
        created.setName(in.getName());
        created.setUsername(in.getUsername());
        created.setStatus(UserStatus.ONLINE);

        given(userService.createUser(any(User.class))).willReturn(created);

        // when / then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(in)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",       is(1)))
                .andExpect(jsonPath("$.name",     is("Test User")))
                .andExpect(jsonPath("$.username", is("testUsername")))
                .andExpect(jsonPath("$.status",   is("ONLINE")));
    }

    @Test
    public void getUserById_existingUser_returnsDto() throws Exception {
        // given
        User u = new User();
        u.setId(42L);
        u.setName("Jane Doe");
        u.setUsername("jane");
        u.setStatus(UserStatus.ONLINE);

        given(userService.getUserById(42L)).willReturn(u);

        // when / then
        mockMvc.perform(get("/users/42")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       is(42)))
                .andExpect(jsonPath("$.name",     is("Jane Doe")))
                .andExpect(jsonPath("$.username", is("jane")))
                .andExpect(jsonPath("$.status",   is("ONLINE")));
    }

    @Test
    public void getUserById_notFound_returns404() throws Exception {
        given(userService.getUserById(99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/users/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateUserInfo_validInput_returnsNoContent() throws Exception {
        UserPostDTO update = new UserPostDTO();
        update.setName("New Name");
        update.setUsername("newuser");

        // no need to stub userService.updateUser—void is fine
        mockMvc.perform(put("/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(update)))
                .andExpect(status().isNoContent());

        Mockito.verify(userService).updateUser(eq(5L), any(User.class));
    }

    @Test
    public void loginUser_validCredentials_returnsDto() throws Exception {
        UserLoginDTO login = new UserLoginDTO();
        login.setUsername("foo");
        login.setPassword("bar");

        User u = new User();
        u.setId(7L);
        u.setName("Foo Bar");
        u.setUsername("foo");
        u.setStatus(UserStatus.ONLINE);

        given(userService.loginUser("foo", "bar")).willReturn(u);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       is(7)))
                .andExpect(jsonPath("$.name",     is("Foo Bar")))
                .andExpect(jsonPath("$.username", is("foo")))
                .andExpect(jsonPath("$.status",   is("ONLINE")));
    }

    @Test
    public void loginUser_badCredentials_returnsUnauthorized() throws Exception {
        UserLoginDTO login = new UserLoginDTO();
        login.setUsername("x");
        login.setPassword("y");

        given(userService.loginUser("x", "y"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void logoutById_returnsDto() throws Exception {
        User u = new User();
        u.setId(13L);
        u.setName("Log Out");
        u.setUsername("logout");
        u.setStatus(UserStatus.OFFLINE);

        given(userService.logoutUser(13L)).willReturn(u);

        mockMvc.perform(delete("/users/logout/13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       is(13)))
                .andExpect(jsonPath("$.name",     is("Log Out")))
                .andExpect(jsonPath("$.username", is("logout")))
                .andExpect(jsonPath("$.status",   is("OFFLINE")));
    }

    @Test
    public void registerUser_validInput_userRegistered() throws Exception {
        UserPostDTO in = new UserPostDTO();
        in.setName("Newbie");
        in.setUsername("newuser");

        User created = new User();
        created.setId(99L);
        created.setName("Newbie");
        created.setUsername("newuser");
        created.setStatus(UserStatus.ONLINE);

        // register and createUser both call createUser(...) under the hood
        given(userService.createUser(any(User.class))).willReturn(created);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(in)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",       is(99)))
                .andExpect(jsonPath("$.name",     is("Newbie")))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.status",   is("ONLINE")));
    }
}
