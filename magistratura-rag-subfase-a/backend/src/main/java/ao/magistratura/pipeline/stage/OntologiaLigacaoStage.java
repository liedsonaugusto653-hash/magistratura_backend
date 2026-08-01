package ao.magistratura.pipeline.stage;

import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.service.OntologiaAutoLigacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Após indexação: sugere/cria ligações tópico↔artigo (ontologia).
 * Fail-soft — erros não interrompem o pipeline.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OntologiaLigacaoStage implements PipelineStage {

    private final OntologiaAutoLigacaoService autoLigacaoService;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.LIGANDO_ONTOLOGIA;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        if (ctx.getDocumento() == null || ctx.getDocumento().getId() == null) {
            return;
        }
        if (!autoLigacaoService.isEnabled()) {
            log.debug("Ontologia auto-ligação desligada (app.ontologia.auto-ligar=false)");
            return;
        }
        int n = autoLigacaoService.ligarAposDocumento(ctx.getDocumento().getId());
        log.info("Etapa LIGANDO_ONTOLOGIA: {} ligação(ões) para documento={}",
                n, ctx.getDocumento().getId());
    }
}
