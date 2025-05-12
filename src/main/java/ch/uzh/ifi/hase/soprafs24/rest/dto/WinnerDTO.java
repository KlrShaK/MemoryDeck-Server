package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class WinnerDTO {
    private Long winnerUserId;   // may be null for draw

    public Long getWinnerUserId() { return winnerUserId; }
    public void setWinnerUserId(Long winnerUserId) { this.winnerUserId = winnerUserId; }
}