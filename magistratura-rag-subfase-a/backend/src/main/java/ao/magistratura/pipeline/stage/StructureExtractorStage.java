package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.EstadoDocumento;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.service.pdf.DocumentoProcessamentoResultado;
import ao.magistratura.service.pdf.EstruturaJuridicaParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delega no {@link EstruturaJuridicaParser} existente (determinístico).
 */
@Component
@RequiredArgsConstructor
public class StructureExtractorStage implements PipelineStage {

    private static final Logger log = LoggerFactory.getLogger(StructureExtractorStage.class);

    private final EstruturaJuridicaParser estruturaJuridicaParser;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.EXTRAINDO_ESTRUTURA;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        if (ctx.getDocumento() != null) {
            ctx.getDocumento().setEstado(EstadoDocumento.ESTRUTURANDO);
        }
        EstruturaJuridicaParser.Resultado resultado = estruturaJuridicaParser.processar(ctx.getPaginas());
        ctx.setArtigosExtraidos(resultado.artigos());
        ctx.setOcorrenciasSoltas(resultado.ocorrenciasSoltasArtigo());

        DocumentoProcessamentoResultado rel = ctx.getResultadoProcessamento();
        if (rel != null) {
            rel.setArtigosEncontrados(resultado.artigos().size());
            rel.setOcorrenciasSoltasArtigo(resultado.ocorrenciasSoltasArtigo());
        }

        log.info("Estrutura: {} artigos, {} ocorrências soltas de 'Artigo N'",
                resultado.artigos().size(), resultado.ocorrenciasSoltasArtigo());
    }
}
