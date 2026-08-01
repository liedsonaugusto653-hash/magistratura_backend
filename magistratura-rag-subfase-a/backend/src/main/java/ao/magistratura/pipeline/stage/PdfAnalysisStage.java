package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.EstadoDocumento;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.service.pdf.PdfAnalysisResult;
import ao.magistratura.service.pdf.PdfAnalysisService;
import ao.magistratura.service.pdf.PdfTipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Classifica o PDF (TEXT / IMAGE / HYBRID / PROTECTED) antes da extracção.
 */
@Component
@RequiredArgsConstructor
public class PdfAnalysisStage implements PipelineStage {

    private final PdfAnalysisService pdfAnalysisService;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.ANALISANDO_PDF;
    }

    @Override
    public void executar(PipelineContexto ctx) throws Exception {
        if (ctx.getDocumento() != null) {
            ctx.getDocumento().setEstado(EstadoDocumento.ANALISANDO);
        }
        PdfAnalysisResult analise = pdfAnalysisService.analisar(ctx.getFicheiro());
        ctx.setPdfAnalysis(analise);
        if (ctx.getDocumento() != null && analise.getPaginas() > 0) {
            ctx.getDocumento().setNumeroPaginas(analise.getPaginas());
        }
        // Tipo no documento para telemetria (se o campo existir)
        if (ctx.getDocumento() != null && analise.getTipo() != null) {
            try {
                ctx.getDocumento().setTipoPdf(analise.getTipo().name());
            } catch (Exception ignored) {
            }
        }
        // PROTECTED bloqueado: a stage de texto fará FALHA_EXTRACAO; aqui só classifica
        if (analise.getTipo() == PdfTipo.PROTECTED && !analise.isOcrNecessario()) {
            // Motivo fica no analysis result para a stage seguinte
        }
    }
}
