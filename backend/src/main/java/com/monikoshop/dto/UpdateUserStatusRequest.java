package com.monikoshop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "وضعیت الزامی است")
    private Boolean enabled;
}
