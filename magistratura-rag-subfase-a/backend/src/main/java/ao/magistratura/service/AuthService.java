package ao.magistratura.service;

import ao.magistratura.dto.auth.LoginRequest;
import ao.magistratura.dto.auth.LoginResponse;
import ao.magistratura.dto.auth.RecuperarPasswordRequest;
import ao.magistratura.dto.auth.RedefinirPasswordRequest;
import ao.magistratura.dto.auth.RegistoRequest;
import ao.magistratura.dto.auth.UtilizadorResponse;
import ao.magistratura.entity.PasswordResetToken;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.repository.PasswordResetTokenRepository;
import ao.magistratura.repository.UtilizadorRepository;
import ao.magistratura.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilizadorRepository utilizadorRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Utilizador utilizador = utilizadorRepository.findByEmail(request.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));

        utilizador.setUltimoLogin(Instant.now());
        utilizadorRepository.save(utilizador);

        String token = jwtService.gerarToken(paraUserDetails(utilizador));

        return new LoginResponse(token, jwtService.getExpirationMs(), mapParaResponse(utilizador));
    }

    public UtilizadorResponse me(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
        return mapParaResponse(utilizador);
    }

    @Transactional
    public LoginResponse registo(RegistoRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        if (utilizadorRepository.existsByEmail(emailNormalizado)) {
            throw new RegraNegocioException("Já existe uma conta com este email");
        }

        Utilizador utilizador = Utilizador.builder()
                .nome(request.nome().trim())
                .email(emailNormalizado)
                .passwordHash(passwordEncoder.encode(request.password()))
                .ativo(true)
                .build();

        try {
            utilizador = utilizadorRepository.save(utilizador);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("Já existe uma conta com este email");
        }

        String token = jwtService.gerarToken(paraUserDetails(utilizador));

        return new LoginResponse(token, jwtService.getExpirationMs(), mapParaResponse(utilizador));
    }

    @Transactional
    public void pedirRecuperacaoPassword(RecuperarPasswordRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        utilizadorRepository.findByEmail(emailNormalizado).ifPresent(utilizador -> {
            passwordResetTokenRepository.deleteByUtilizadorId(utilizador.getId());

            String tokenBruto = gerarTokenAleatorio();
            PasswordResetToken tokenEntidade = PasswordResetToken.builder()
                    .utilizadorId(utilizador.getId())
                    .token(tokenBruto)
                    .expiraEm(Instant.now().plus(1, ChronoUnit.HOURS))
                    .usado(false)
                    .build();
            passwordResetTokenRepository.save(tokenEntidade);

            String link = frontendBaseUrl + "/redefinir-password?token=" + tokenBruto;
            emailService.enviarRecuperacaoPassword(utilizador.getEmail(), utilizador.getNome(), link);
        });
    }

    @Transactional
    public void redefinirPassword(RedefinirPasswordRequest request) {
        PasswordResetToken tokenEntidade = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new RegraNegocioException("Token inválido ou expirado"));

        if (tokenEntidade.isUsado() || tokenEntidade.getExpiraEm().isBefore(Instant.now())) {
            throw new RegraNegocioException("Token inválido ou expirado");
        }

        Utilizador utilizador = utilizadorRepository.findById(tokenEntidade.getUtilizadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));

        utilizador.setPasswordHash(passwordEncoder.encode(request.novaPassword()));
        utilizadorRepository.save(utilizador);

        tokenEntidade.setUsado(true);
        passwordResetTokenRepository.save(tokenEntidade);
    }

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private UserDetails paraUserDetails(Utilizador utilizador) {
        return org.springframework.security.core.userdetails.User
                .withUsername(utilizador.getEmail())
                .password(utilizador.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();
    }

    private UtilizadorResponse mapParaResponse(Utilizador u) {
        return new UtilizadorResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getFotografiaUrl(),
                u.getDataCriacao(),
                u.getUltimoLogin(),
                u.getPreferenciasJson()
        );
    }

    /**
     * Actualiza nome, email (opcional) e fotografia.
     * Se o email mudar, devolve um LoginResponse com JWT novo (o subject do token é o email).
     */
    @Transactional
    public LoginResponse atualizarPerfil(String emailActual, ao.magistratura.dto.auth.AtualizarPerfilRequest request) {
        Utilizador u = utilizadorRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));

        u.setNome(request.nome().trim());

        if (request.email() != null && !request.email().isBlank()) {
            String novoEmail = request.email().trim().toLowerCase();
            if (!novoEmail.equalsIgnoreCase(emailActual)) {
                if (utilizadorRepository.existsByEmail(novoEmail)) {
                    throw new RegraNegocioException("Já existe uma conta com este email");
                }
                u.setEmail(novoEmail);
            }
        }

        if (request.fotografiaUrl() != null) {
            String url = request.fotografiaUrl().trim();
            u.setFotografiaUrl(url.isEmpty() ? null : url);
        }

        try {
            u = utilizadorRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("Já existe uma conta com este email");
        }

        String token = jwtService.gerarToken(paraUserDetails(u));
        return new LoginResponse(token, jwtService.getExpirationMs(), mapParaResponse(u));
    }

    @Transactional
    public void alterarPassword(String email, ao.magistratura.dto.auth.AlterarPasswordRequest request) {
        Utilizador u = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
        if (!passwordEncoder.matches(request.passwordAtual(), u.getPasswordHash())) {
            throw new RegraNegocioException("A palavra-passe actual está incorrecta");
        }
        if (request.novaPassword().equals(request.passwordAtual())) {
            throw new RegraNegocioException("A nova palavra-passe deve ser diferente da actual");
        }
        u.setPasswordHash(passwordEncoder.encode(request.novaPassword()));
        utilizadorRepository.save(u);
    }

    @Transactional
    public UtilizadorResponse atualizarPreferencias(String email, ao.magistratura.dto.auth.AtualizarPreferenciasRequest request) {
        Utilizador u = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
        String json = request.preferenciasJson();
        if (json != null && json.length() > 4000) {
            throw new RegraNegocioException("Preferências demasiado longas");
        }
        if (json != null && !json.isBlank() && !json.trim().startsWith("{")) {
            throw new RegraNegocioException("Preferências inválidas");
        }

        String merged = mergePreferenciasJson(u.getPreferenciasJson(), json);
        u.setPreferenciasJson(merged);
        return mapParaResponse(utilizadorRepository.save(u));
    }

    /**
     * Junta o JSON novo com o existente. Chaves extra no servidor são preservadas.
     */
    private String mergePreferenciasJson(String existentes, String novas) {
        if (novas == null || novas.isBlank()) {
            return existentes == null || existentes.isBlank() ? null : existentes.trim();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode base =
                    existentes != null && !existentes.isBlank() && existentes.trim().startsWith("{")
                            ? (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(existentes)
                            : mapper.createObjectNode();
            com.fasterxml.jackson.databind.JsonNode incoming = mapper.readTree(novas);
            if (!incoming.isObject()) {
                throw new RegraNegocioException("Preferências inválidas");
            }
            incoming.fields().forEachRemaining(e -> base.set(e.getKey(), e.getValue()));
            if (base.has("guiaNivel")) {
                String g = base.get("guiaNivel").asText("");
                if (!g.equals("normal") && !g.equals("minimo") && !g.equals("desligado")) {
                    base.put("guiaNivel", "normal");
                }
            }
            return mapper.writeValueAsString(base);
        } catch (RegraNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RegraNegocioException("Preferências inválidas");
        }
    }
}
