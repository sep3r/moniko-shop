package com.monikoshop.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRolesRequest {

    @NotEmpty(message = "حداقل یک نقش الزامی است")
    private Set<String> roles;
}
