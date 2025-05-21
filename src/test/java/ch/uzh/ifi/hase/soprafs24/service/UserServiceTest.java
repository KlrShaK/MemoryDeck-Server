package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------- getUsers() ----------

    @Test
    void getUsers_returnsAllUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getUsers();
        assertSame(users, result);  // returns exactly what repository returned :contentReference[oaicite:0]{index=0}
    }

    // ---------- createUser() ----------

    @Test
    void createUser_withoutPassword_setsDefaultsAndSaves() {
        User input = new User();
        input.setUsername("alice");

        when(userRepository.findByUsername("alice")).thenReturn(null);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User created = userService.createUser(input);

        assertNotNull(created.getToken(),      "token should be generated");
        assertEquals(UserStatus.ONLINE, created.getStatus());
        assertNotNull(created.getCreationDate(),"creationDate should be set");
        assertNull(created.getPassword(),     "since no password was supplied");
        verify(userRepository).save(created);
        verify(userRepository).flush();       // flush() is called after save :contentReference[oaicite:1]{index=1}
    }

    @Test
    void createUser_withPassword_encodesPassword() {
        User input = new User();
        input.setUsername("bob");
        input.setPassword("secret");

        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User created = userService.createUser(input);

        assertNotNull(created.getPassword(), "password should be set");
        assertNotEquals("secret", created.getPassword());
        assertTrue(
                new BCryptPasswordEncoder()
                        .matches("secret", created.getPassword()),
                "BCrypt should match the raw password"
        );
        verify(userRepository).save(created);
        verify(userRepository).flush();
    }

    @Test
    void createUser_nullUsername_throwsBadRequest() {
        User input = new User();
        input.setUsername(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(input)
        );
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Username must not be empty"));
    }

    @Test
    void createUser_emptyUsername_throwsBadRequest() {
        User input = new User();
        input.setUsername("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(input)
        );
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Username must not be empty"));
    }

    @Test
    void createUser_duplicateUsername_throwsConflict() {
        User input = new User();
        input.setUsername("charlie");

        when(userRepository.findByUsername("charlie"))
                .thenReturn(new User());  // simulate existing user

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(input)
        );
        assertEquals(409, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Username 'charlie' is already taken"));
    }

    // ---------- updateUser() ----------

    @Test
    void updateUser_existing_updatesFields() {
        User existing = new User();
        existing.setId(100L);
        existing.setUsername("old");
        existing.setBirthday(new Date(0));

        when(userRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        // for the username‐uniqueness check
        when(userRepository.findByUsername("new"))
                .thenReturn(null);

        User updates = new User();
        updates.setUsername("new");
        Date later = new Date(123456789);
        updates.setBirthday(later);

        userService.updateUser(100L, updates);

        assertEquals("new", existing.getUsername());
        assertEquals(later, existing.getBirthday());
        verify(userRepository).save(existing);  // no flush on updateUser :contentReference[oaicite:2]{index=2}
    }

    @Test
    void updateUser_partialNull_doesNotOverwriteNulls() {
        User existing = new User();
        existing.setId(200L);
        existing.setUsername("keep");
        existing.setBirthday(new Date(0));

        when(userRepository.findById(200L))
                .thenReturn(Optional.of(existing));

        User updates = new User();
        updates.setUsername(null);
        updates.setBirthday(null);

        userService.updateUser(200L, updates);

        assertEquals("keep", existing.getUsername());
        assertEquals(new Date(0), existing.getBirthday());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_notFound_throwsNotFound() {
        when(userRepository.findById(300L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(300L, new User())
        );
        assertEquals(404, ex.getStatus().value());
    }

    // ---------- getUserById() ----------

    @Test
    void getUserById_found_returnsUser() {
        User u = new User();
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(u));

        assertSame(u, userService.getUserById(10L));
    }

    @Test
    void getUserById_notFound_throwsNotFound() {
        when(userRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserById(20L)
        );
        assertEquals(404, ex.getStatus().value());
        assertTrue(ex.getReason().contains("User with ID 20 not found"));
    }

    // ---------- loginUser() ----------

    @Test
    void loginUser_nullUsername_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser(null, "pw")
        );
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Username must not be empty"));
    }

    @Test
    void loginUser_emptyUsername_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser("   ", "pw")
        );
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void loginUser_nullPassword_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser("user", null)
        );
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Password must not be empty"));
    }

    @Test
    void loginUser_emptyPassword_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser("user", " ")
        );
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void loginUser_userNotFound_throwsUnauthorized() {
        when(userRepository.findByUsername("nope"))
                .thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser("nope", "pw")
        );
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getReason().contains("No user registered with username 'nope'"));
    }

    @Test
    void loginUser_invalidPassword_throwsUnauthorized() {
        String raw = "pass";
        String encoded = new BCryptPasswordEncoder().encode("other");
        User u = new User();
        u.setUsername("user");
        u.setPassword(encoded);

        when(userRepository.findByUsername("user"))
                .thenReturn(u);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser("user", raw)
        );
        assertEquals(401, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Invalid credentials"));
    }

    @Test
    void loginUser_success_generatesTokenAndSaves() {
        String raw = "mypw";
        String encoded = new BCryptPasswordEncoder().encode(raw);
        User u = new User();
        u.setUsername("u");
        u.setPassword(encoded);
        u.setStatus(UserStatus.OFFLINE);

        when(userRepository.findByUsername("u"))
                .thenReturn(u);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User loggedIn = userService.loginUser("u", raw);

        assertNotNull(loggedIn.getToken());
        assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
        verify(userRepository).save(u);
        verify(userRepository).flush();
    }

    // ---------- logoutUser() ----------

    @Test
    void logoutUser_success_clearsTokenAndSaves() {
        User u = new User();
        u.setId(55L);
        u.setToken("abc");
        u.setStatus(UserStatus.ONLINE);

        when(userRepository.findById(55L))
                .thenReturn(Optional.of(u));

        User loggedOut = userService.logoutUser(55L);

        assertNull(loggedOut.getToken());
        assertEquals(UserStatus.OFFLINE, loggedOut.getStatus());
        verify(userRepository).save(u);
        verify(userRepository).flush();
    }

    @Test
    void logoutUser_notFound_throwsNotFound() {
        when(userRepository.findById(66L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.logoutUser(66L)
        );
        assertEquals(404, ex.getStatus().value());
    }
}
