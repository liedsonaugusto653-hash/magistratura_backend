package ao.magistratura.controller;

import ao.magistratura.dto.auth.LoginRequest;
import ao.magistratura.dto.auth.LoginResponse;
import ao.magistratura.dto.auth.RecuperarPasswordRequest;
import ao.magistratura.dto.auth.RedefinirPasswordRequest;
import ao.magistratura.dto.auth.RegistoRequest;
import ao.magistratura.dto.auth.UtilizadorResponse;
import ao.magistratura.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, registo e sessão do estudante")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica um estudante e devolve um token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registo")
    @Operation(summary = "Regista um novo estudante e devolve um token JWT")
    public ResponseEntity<LoginResponse> registo(@Valid @RequestBody RegistoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registo(request));
    }

    @PostMapping("/recuperar-password")
    @Operation(summary = "Inicia o processo de recuperação de password (resposta genérica)")
    public ResponseEntity<Void> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        authService.pedirRecuperacaoPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-password")
    @Operation(summary = "Redefine a password a partir de um token válido")
    public ResponseEntity<Void> redefinirPassword(@Valid @RequestBody RedefinirPasswordRequest request) {
        authService.redefinirPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Termina a sessão (stateless — o cliente descarta o token)")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve os dados do estudante autenticado")
    public ResponseEntity<UtilizadorResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Actualiza o perfil do estudante autenticado (nome, email, fotografia). Devolve JWT actualizado.")
    public ResponseEntity<LoginResponse> atualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody ao.magistratura.dto.auth.AtualizarPerfilRequest request) {
        return ResponseEntity.ok(authService.atualizarPerfil(authentication.getName(), request));
    }

    @PostMapping("/me/password")
    @Operation(summary = "Altera a palavra-passe do estudante autenticado")
    public ResponseEntity<Void> alterarPassword(
            Authentication authentication,
            @Valid @RequestBody ao.magistratura.dto.auth.AlterarPasswordRequest request) {
        authService.alterarPassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/preferencias")
    @Operation(summary = "Guarda preferências de interface / estudo do estudante")
    public ResponseEntity<UtilizadorResponse> preferencias(
            Authentication authentication,
            @Valid @RequestBody ao.magistratura.dto.auth.AtualizarPreferenciasRequest request) {
        return ResponseEntity.ok(authService.atualizarPreferencias(authentication.getName(), request));
    }
}
