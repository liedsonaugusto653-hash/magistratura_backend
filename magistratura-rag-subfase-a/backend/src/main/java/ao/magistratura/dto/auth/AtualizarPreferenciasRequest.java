package ao.magistratura.dto.auth;

import jakarta.validation.constraints.Size;

/**
 * Preferências de estudo / interface (JSON).
 * Chaves conhecidas pelo frontend:
 * <ul>
 *   <li>{@code sidebarIniciaColapsada} (boolean)</li>
 *   <li>{@code confirmarAntesDeEliminar} (boolean)</li>
 *   <li>{@code mostrarDicas} (boolean)</li>
 *   <li>{@code guiaNivel} ({@code normal} | {@code minimo} | {@code desligado})</li>
 * </ul>
 * O serviço faz merge com o JSON já guardado.
 */
public record AtualizarPreferenciasRequest(
        @Size(max = 4000) String preferenciasJson
) {}
