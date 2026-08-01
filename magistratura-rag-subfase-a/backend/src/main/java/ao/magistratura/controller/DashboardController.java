package ao.magistratura.controller;

import ao.magistratura.dto.dashboard.DashboardResponse;
import ao.magistratura.service.DashboardService;
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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Resumo de progresso do estudante autenticado")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Devolve o dashboard do estudante autenticado (JWT obrigatório)")
    public ResponseEntity<DashboardResponse> obter(Authentication authentication) {
        // O utilizador é obtido exclusivamente do contexto de segurança JWT.
        // Não se aceita utilizadorId vindo do frontend.
        String email = authentication.getName();
        return ResponseEntity.ok(dashboardService.obterDashboard(email));
    }
}
