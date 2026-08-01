package ao.magistratura.dto.ontologia;

import java.util.List;

/**
 * Caso prático socrático de um conceito: apresenta-se {@code enunciado} +
 * {@code perguntas} para o estudante pensar sozinho ANTES de ver
 * {@code explicacao} (que só aí ancora nos artigos — o artigo é a
 * confirmação, não o ponto de partida).
 */
public record CasoPraticoResponse(String enunciado, List<String> perguntas, String explicacao) {}
