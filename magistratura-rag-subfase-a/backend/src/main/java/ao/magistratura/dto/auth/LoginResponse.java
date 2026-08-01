package ao.magistratura.dto.auth;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmMs,
        UtilizadorResponse utilizador
) {
    public LoginResponse(String token, long expiraEmMs, UtilizadorResponse utilizador) {
        this(token, "Bearer", expiraEmMs, utilizador);
    }
}
