package ec.gob.mdh.sara.controller;

import ec.gob.mdh.sara.dto.FaceTestRequest;
import ec.gob.mdh.sara.dto.FaceTestResponse;
import ec.gob.mdh.sara.service.FaceNetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comparar")
@RequiredArgsConstructor
@Tag(name = "Comparación de Rostros", description = "Compara dos fotografías y devuelve el porcentaje de similitud facial")
public class FaceNetTestController {

    private final FaceNetService faceNetService;

    @PostMapping
    @Operation(
        summary = "Comparar dos fotografías",
        description = """
            Recibe dos imágenes en Base64 y retorna:
            - porcentaje: % de similitud entre los rostros (0-100)
            - esMismaPersona: true si la similitud supera el umbral (36.3%)
            - error: descripción si alguna imagen no pudo procesarse
            """
    )
    public ResponseEntity<FaceTestResponse> comparar(@Valid @RequestBody FaceTestRequest request) {
        FaceNetService.ComparacionCompleta resultado =
                faceNetService.compararConDetalle(request.getImagen1(), request.getImagen2());

        if (resultado.tieneError()) {
            return ResponseEntity.unprocessableEntity()
                    .body(FaceTestResponse.error(resultado.error()));
        }

        return ResponseEntity.ok(FaceTestResponse.ok(resultado.porcentaje(), resultado.esMismaPersona()));
    }
}
