package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Date;

// let Post DTO to get name,username, birthday (optional since we marked it as nullable in User.java) 
// and password from user in order to be able to create a new user
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class UserPostDTO {

  private String name;

  private String username;

  private String password; 

  private Date birthday;
}
