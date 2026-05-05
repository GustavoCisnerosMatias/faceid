package com.facecompare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompareRequest {

    @NotBlank(message = "imagen1 es requerida")
    private String imagen1;

    @NotBlank(message = "imagen2 es requerida")
    private String imagen2;
}
