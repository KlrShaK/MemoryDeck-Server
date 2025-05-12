package ch.uzh.ifi.hase.soprafs24.rest.dto;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class UserPasswordUpdateDTO {
    private String oldPassword;
    private String newPassword;
}
