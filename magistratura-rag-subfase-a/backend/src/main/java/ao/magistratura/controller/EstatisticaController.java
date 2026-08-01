package ao.magistratura.controller;

import ao.magistratura.dto.estatistica.EstatisticaResponse;
import ao.magistratura.service.EstatisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estatisticas")
@RequiredArgsConstructor
@Tag(name = "Estatísticas", description = "Progresso consolidado do estudante autenticado")
@SecurityRequirement(name = "bearerAuth")
public class EstatisticaController {

    private final EstatisticaService estatisticaService;

    @GetMapping
    @Operation(summary = "Devolve as estatísticas de estudo do estudante autenticado (dados reais)")
    public ResponseEntity<EstatisticaResponse> obter(Authentication authentication) {
        return ResponseEntity.ok(estatisticaService.obter(authentication.getName()));
    }
}
