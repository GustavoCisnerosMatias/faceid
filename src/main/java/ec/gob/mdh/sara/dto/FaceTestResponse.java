package ec.gob.mdh.sara.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaceTestResponse {

    private Double porcentaje;
    private Boolean esMismaPersona;
    private String error;

    public static FaceTestResponse ok(double porcentaje, boolean esMismaPersona) {
        return new FaceTestResponse(porcentaje, esMismaPersona, null);
    }

    public static FaceTestResponse error(String mensaje) {
        return new FaceTestResponse(null, null, mensaje);
    }
}
