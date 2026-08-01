package ao.magistratura.jornada.web;

import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.jornada.service.JornadaService;
import ao.magistratura.repository.UtilizadorRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API mínima da Caminhada. Progresso narrativo — não é LMS.
 * Disponibilidade depende da biblioteca real (diplomas com artigos).
 */
@RestController
@RequestMapping("/api/jornada")
@RequiredArgsConstructor
@Tag(name = "Caminhada", description = "Progresso narrativo com o João")
@SecurityRequirement(name = "bearerAuth")
public class JornadaController {

    private final JornadaService jornadaService;
    private final UtilizadorRepository utilizadorRepository;

    @GetMapping("/progresso")
    public ResponseEntity<Map<String, Object>> progresso(Authentication authentication) {
        Utilizador u = utilizador(authentication);
        return ResponseEntity.ok(jornadaService.obterProgresso(u.getId()));
    }

    @PutMapping("/progresso")
    public ResponseEntity<Map<String, Object>> guardar(
            Authentication authentication,
            @RequestBody Map<String, Object> body
    ) {
        Utilizador u = utilizador(authentication);
        String momentoId = body.get("momentoId") != null ? body.get("momentoId").toString() : null;
        String cenaId = body.get("cenaId") != null ? body.get("cenaId").toString() : null;
        @SuppressWarnings("unchecked")
        List<String> concluidos = body.get("concluidos") instanceof List
                ? (List<String>) body.get("concluidos")
                : List.of();
        return ResponseEntity.ok(jornadaService.guardarProgresso(u.getId(), momentoId, cenaId, concluidos));
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<Map<String, Object>> disponibilidade() {
        return ResponseEntity.ok(jornadaService.disponibilidade());
    }

    private Utilizador utilizador(Authentication authentication) {
        return utilizadorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
    }
}
