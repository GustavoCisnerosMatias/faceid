package com.facecompare.controller;

import com.facecompare.dto.CompareRequest;
import com.facecompare.dto.CompareResponse;
import com.facecompare.service.FaceCompareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comparar")
@RequiredArgsConstructor
@Tag(name = "Comparación de Rostros")
public class FaceCompareController {

    private final FaceCompareService faceCompareService;

    @PostMapping
    @Operation(
        summary = "Comparar dos fotografías",
        description = "Recibe dos imágenes en Base64 y retorna el porcentaje de similitud facial (0-100) y si corresponden a la misma persona."
    )
    public ResponseEntity<CompareResponse> comparar(@Valid @RequestBody CompareRequest request) {
        FaceCompareService.Result resultado =
                faceCompareService.comparar(request.getImagen1(), request.getImagen2());

        if (resultado.tieneError()) {
            return ResponseEntity.unprocessableEntity().body(CompareResponse.error(resultado.error()));
        }

        return ResponseEntity.ok(CompareResponse.ok(resultado.porcentaje(), resultado.esMismaPersona()));
    }
}
