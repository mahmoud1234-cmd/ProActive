package com.proactive.demo.dto;

import com.proactive.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private User.Role role;
    private User.Status status;
    private LocalDateTime createdAt;

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}
