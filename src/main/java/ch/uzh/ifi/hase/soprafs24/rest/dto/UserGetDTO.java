package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class UserGetDTO {

  private Long id;
  private String name;
  private String token;
  private String username;
  private UserStatus status;
  private Date creationDate; // adding the additional variables in Get DTO so that we can fetch those information
  private Date birthday;
  private List<Deck> decks;

}
