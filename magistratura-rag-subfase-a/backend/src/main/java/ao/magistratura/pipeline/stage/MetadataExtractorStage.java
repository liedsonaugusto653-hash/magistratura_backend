package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.Documento;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import org.springframework.stereotype.Component;

/**
 * Consolida metadados do documento a partir do contexto (páginas, diploma).
 * Extração de metadados embutidos no PDF pode ser acrescentada aqui sem
 * alterar o orquestrador.
 */
@Component
public class MetadataExtractorStage implements PipelineStage {

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.EXTRAINDO_METADADOS;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        Documento doc = ctx.getDocumento();
        if (doc.getNumeroPaginas() == null && ctx.getPaginas() != null) {
            doc.setNumeroPaginas(ctx.getPaginas().size());
        }
        if (ctx.getDiploma() != null) {
            doc.setDiploma(ctx.getDiploma());
            if (doc.getDataPublicacao() == null && ctx.getDiploma().getDataPublicacao() != null) {
                doc.setDataPublicacao(ctx.getDiploma().getDataPublicacao());
            }
        }
    }
}
