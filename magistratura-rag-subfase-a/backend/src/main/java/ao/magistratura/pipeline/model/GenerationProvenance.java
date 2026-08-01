package ao.magistratura.pipeline.model;

import java.time.Instant;

/**
 * Metadados de proveniência a gravar em qualquer artefacto gerado por IA.
 * Permite regenerar seletivamente por pipeline / provider / modelo / prompt.
 */
public record GenerationProvenance(
        String pipelineVersion,
        String aiProvider,
        String aiModel,
        String promptVersion,
        Instant geradoEm,
        String estadoValidacao
) {
    public static final String ESTADO_GERADO = "GERADO";
    public static final String ESTADO_REVISADO = "REVISADO";
    public static final String ESTADO_APROVADO = "APROVADO";

    public static GenerationProvenance of(String aiProvider, String aiModel, String promptVersion) {
        return new GenerationProvenance(
                PipelineVersion.ATUAL,
                aiProvider,
                aiModel,
                promptVersion,
                Instant.now(),
                ESTADO_GERADO
        );
    }
}
