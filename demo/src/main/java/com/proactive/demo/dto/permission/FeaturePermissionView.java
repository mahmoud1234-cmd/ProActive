package com.proactive.demo.dto.permission;

import com.proactive.demo.model.AccessLevel;
import com.proactive.demo.model.PermissionFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturePermissionView {
    private PermissionFeature feature;
    private AccessLevel accessLevel;
    private boolean custom;
}
