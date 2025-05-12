package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;


/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
@Entity
@Table(name = "user")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private String name;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = true, unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus status;

  // defining additional variables in User representation
  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Date creationDate;

  @Column
  private Date birthday;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Deck> decks = new ArrayList<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Score> scores = new ArrayList<>();

  @OneToMany(mappedBy = "fromUser", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Invitation> invitationsSent = new ArrayList<>();

  @OneToMany(mappedBy = "toUser", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Invitation> invitationsReceived = new ArrayList<>();

}
