package ao.magistratura.dto.questao;

import java.util.UUID;

/** Listagem / detalhe sem revelar a resposta correta. */
public record QuestaoResponse(
        UUID id,
        String enunciado,
        String opcaoA,
        String opcaoB,
        String opcaoC,
        String opcaoD,
        String nivelDificuldade,
        UUID temaId,
        String temaNome,
        UUID categoriaId,
        String categoriaNome,
        UUID diplomaId,
        String diplomaTitulo
) {}
