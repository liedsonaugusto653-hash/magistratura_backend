package ao.magistratura.pipeline.stage;

import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DocumentValidatorStage implements PipelineStage {

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.VALIDADO;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        if (ctx.getDocumento() == null) {
            throw new RegraNegocioException("Documento em falta no contexto do pipeline.");
        }
        File f = ctx.getFicheiro();
        if (f == null || !f.isFile()) {
            throw new RegraNegocioException("Ficheiro PDF não encontrado em disco.");
        }
        if (ctx.getDiploma() == null) {
            throw new RegraNegocioException("É necessário associar um diploma antes de processar.");
        }
        String hash = ctx.getDocumento().getHashFicheiro();
        if (hash == null || hash.isBlank()) {
            throw new RegraNegocioException("Documento sem hash SHA-256 — reimporte o ficheiro.");
        }
    }
}
