package com.proactive.demo.dto.permission;

import com.proactive.demo.model.AccessLevel;
import com.proactive.demo.model.PermissionFeature;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionAssignmentRequest {

    @NotNull
    private PermissionFeature feature;

    @NotNull
    private AccessLevel accessLevel;
}
