package com.proactive.demo.dto.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionBulkUpdateRequest {

    @Valid
    @NotEmpty
    private List<PermissionAssignmentRequest> permissions;
}
