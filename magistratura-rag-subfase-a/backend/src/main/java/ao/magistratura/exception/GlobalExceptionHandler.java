package ao.magistratura.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ApiError erro = new ApiError(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        log.warn("Regra de negócio violada: {}", ex.getMessage());
        ApiError erro = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }

    @ExceptionHandler(IAIndisponivelException.class)
    public ResponseEntity<ApiError> handleIAIndisponivel(IAIndisponivelException ex, HttpServletRequest req) {
        log.error("Tutor IA indisponível: {}", ex.getMessage());
        // Mensagem real da causa (Ollama timeout, JSON inválido, diploma sem artigos, etc.)
        // em vez de texto genérico que esconde o diagnóstico.
        String detalhe = ex.getMessage();
        if (detalhe == null || detalhe.isBlank()) {
            detalhe = "O Tutor IA está indisponível de momento. Tenta novamente dentro de instantes.";
        }
        ApiError erro = new ApiError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable",
                detalhe, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(erro);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> handleCredenciais(Exception ex, HttpServletRequest req) {
        log.warn("Falha de autenticação: {}", ex.getMessage());
        ApiError erro = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Credenciais inválidas", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        ApiError erro = new ApiError(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Erro de validação", req.getRequestURI(), detalhes);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado", ex);
        ApiError erro = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Ocorreu um erro inesperado. Tente novamente.", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
