package com.facecompare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompareResponse {

    private Double porcentaje;
    private Boolean esMismaPersona;
    private String error;

    public static CompareResponse ok(double porcentaje, boolean esMismaPersona) {
        return new CompareResponse(porcentaje, esMismaPersona, null);
    }

    public static CompareResponse error(String mensaje) {
        return new CompareResponse(null, null, mensaje);
    }
}
