package com.proactive.demo.dto.permission;

import com.proactive.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionViewResponse {
    private Long userId;
    private String email;
    private User.Role role;
    private List<FeaturePermissionView> permissions;
}
