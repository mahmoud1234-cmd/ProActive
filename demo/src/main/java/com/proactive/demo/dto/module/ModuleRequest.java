package com.proactive.demo.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleRequest {

    @NotBlank
    @Size(min = 2, max = 200)
    private String name;

    private String description;

    private Integer progressPct;

    private Integer sortOrder;

    /** null = module racine, sinon ID du parent */
    private Long parentId;
}
