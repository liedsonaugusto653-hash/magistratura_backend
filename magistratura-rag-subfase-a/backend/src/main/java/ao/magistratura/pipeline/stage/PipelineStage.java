package ao.magistratura.pipeline.stage;

import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;

/**
 * Contrato de uma etapa isolada do pipeline. Cada implementação tem uma
 * única responsabilidade e pode ser testada / substituída independentemente.
 */
public interface PipelineStage {

    PipelineEtapa etapa();

    void executar(PipelineContexto ctx) throws Exception;
}
