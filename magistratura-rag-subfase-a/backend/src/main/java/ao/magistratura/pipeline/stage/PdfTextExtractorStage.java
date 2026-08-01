package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.EstadoDocumento;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.service.pdf.DocumentoProcessamentoResultado;
import ao.magistratura.service.pdf.MetodoExtracao;
import ao.magistratura.service.pdf.OcrExtractorService;
import ao.magistratura.service.DocumentoProgressoService;
import ao.magistratura.service.pdf.PaginaTexto;
import ao.magistratura.service.pdf.PdfAnalysisResult;
import ao.magistratura.service.pdf.PdfExtractorService;
import ao.magistratura.service.pdf.PdfTipo;
import ao.magistratura.service.pdf.TextoJuridicoNormalizer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Extracção de texto com fallback inteligente:
 * <ol>
 *   <li>PDFBox (camada de texto nativa)</li>
 *   <li>Se insuficiente e OCR activo → Tesseract</li>
 *   <li>Normalização jurídica determinística</li>
 * </ol>
 * Nunca deixa o pipeline concluir com texto vazio sem erro explícito.
 */
@Component
@RequiredArgsConstructor
public class PdfTextExtractorStage implements PipelineStage {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractorStage.class);

    @Value("${app.pipeline.ocr.min-chars-uteis:40}")
    private int minCharsUteis;

    @Value("${app.pipeline.ocr.min-chars-por-pagina:15}")
    private int minCharsPorPagina;

    @Value("${app.pipeline.ocr.enabled:true}")
    private boolean ocrEnabled;

    private final PdfExtractorService pdfExtractorService;
    private final OcrExtractorService ocrExtractorService;
    private final DocumentoProgressoService documentoProgressoService;
    private final TextoJuridicoNormalizer textoJuridicoNormalizer;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.EXTRAINDO_PDF;
    }

    @Override
    public void executar(PipelineContexto ctx) throws Exception {
        if (ctx.getDocumento() != null) {
            ctx.getDocumento().setEstado(EstadoDocumento.EXTRAINDO_TEXTO);
        }

        DocumentoProcessamentoResultado rel = ctx.getResultadoProcessamento();
        if (rel == null) {
            rel = new DocumentoProcessamentoResultado();
            ctx.setResultadoProcessamento(rel);
        }

        PdfAnalysisResult analise = ctx.getPdfAnalysis();
        if (analise != null) {
            rel.setTipoPdf(analise.getTipo());
            rel.setPaginas(analise.getPaginas());
            analise.getAvisos().forEach(rel::addAviso);

            // PROTECTED → falha imediata SEMPRE (sem OCR).
            // Rasterizar PDFs com restrições no PDFBox bloqueia a thread de forma
            // irrecuperável; timeouts com Future.cancel não interrompem o render nativo.
            if (analise.getTipo() == PdfTipo.PROTECTED) {
                String msg = analise.getMotivo() != null && !analise.getMotivo().isBlank()
                        ? analise.getMotivo()
                        : "PDF protegido ou com restrições de cópia. "
                          + "Exporte uma versão sem protecção e volte a importar.";
                marcarFalha(ctx, rel, analise.getPaginas(), 0, msg);
                return;
            }
        }

        // 1) Tentativa nativa PDFBox (pode devolver vazio em scans / restrição de cópia)
        List<PaginaTexto> paginas = List.of();
        try {
            paginas = pdfExtractorService.extrairPorPagina(ctx.getFicheiro());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage()
                    : "Falha ao abrir/extrair o PDF.";
            boolean ocrPlaneado = analise != null && analise.isOcrNecessario();
            boolean passwordUser = msg.toLowerCase().contains("password")
                    && !msg.toLowerCase().contains("imprimir");
            // Se OCR está previsto (scan / print permitido), não abortar — seguir para OCR
            if (ocrPlaneado && !passwordUser) {
                log.warn("Extracção nativa falhou; a continuar para OCR: {}", msg);
                paginas = List.of();
            } else if (msg.toLowerCase().contains("password") || msg.toLowerCase().contains("protegido")
                    || msg.toLowerCase().contains("timeout")) {
                marcarFalha(ctx, rel, analise != null ? analise.getPaginas() : 0, 0, msg);
                return;
            } else {
                throw e;
            }
        }
        long charsNativos = contarCharsUteis(paginas);
        MetodoExtracao metodo = MetodoExtracao.PDFBOX;

        log.info("PDFBox: {} páginas, {} chars úteis (mínimo={})",
                paginas.size(), charsNativos, minCharsUteis);

        boolean precisaOcr = charsNativos < minCharsUteis
                || (analise != null && analise.isOcrNecessario());

        // Páginas sem texto nativo suficiente (PDFs híbridos: capa digital + miolo scan)
        java.util.Set<Integer> paginasFracas = new java.util.HashSet<>();
        if (paginas != null) {
            for (PaginaTexto p : paginas) {
                String t = p.texto() != null ? p.texto().replace("", "").trim() : "";
                if (t.length() < minCharsPorPagina) {
                    paginasFracas.add(p.numeroPagina());
                }
            }
        }
        boolean hibridoParcial = charsNativos >= minCharsUteis
                && !paginasFracas.isEmpty()
                && paginasFracas.size() < paginas.size();

        // 2) Fallback OCR — documento inteiro ou só páginas fracas
        if (precisaOcr || hibridoParcial) {
            if (!ocrEnabled) {
                if (precisaOcr) {
                    marcarFalha(ctx, rel, paginas.size(), charsNativos,
                            "Texto nativo insuficiente e OCR está desactivado (app.pipeline.ocr.enabled=false). "
                                    + "Use um PDF com texto seleccionável ou active o OCR.");
                    return;
                }
                // híbrido parcial sem OCR: segue com o que há
            } else if (!ocrExtractorService.isDisponivel()) {
                if (precisaOcr) {
                    marcarFalha(ctx, rel, paginas.size(), charsNativos,
                            "Texto nativo insuficiente e motor OCR (Tesseract) indisponível. "
                                    + "Instale tesseract-ocr e o pacote de idioma português (por), "
                                    + "ou forneça um PDF com camada de texto. "
                                    + "No Windows: defina PIPELINE_OCR_DATAPATH para a pasta tessdata.");
                    return;
                }
            } else {
                if (ctx.getDocumento() != null) {
                    ctx.getDocumento().setEstado(EstadoDocumento.OCR_EM_EXECUCAO);
                }
                UUID docId = ctx.getDocumento() != null ? ctx.getDocumento().getId() : null;
                java.util.Set<Integer> alvoOcr = precisaOcr ? null : paginasFracas;

                log.info("A activar OCR (Tesseract) — texto nativo={} tipo={} páginasOCR={}",
                        charsNativos,
                        analise != null ? analise.getTipo() : "?",
                        alvoOcr == null ? "TODAS" : alvoOcr.size());

                if (docId != null) {
                    documentoProgressoService.mensagem(docId,
                            alvoOcr == null
                                    ? "A ler o documento digitalizado (OCR) página a página…"
                                    : "A completar páginas sem texto com OCR…");
                }

                List<PaginaTexto> ocrPaginas = ocrExtractorService.extrairPorPagina(
                        ctx.getFicheiro(),
                        alvoOcr,
                        (ok, total) -> {
                            if (docId != null) {
                                documentoProgressoService.actualizar(docId, ok, total,
                                        "OCR: página " + ok + " de " + total + "…");
                            }
                        });

                if (alvoOcr == null) {
                    // Scan / IMAGE completo — substitui tudo
                    long charsOcr = contarCharsUteis(ocrPaginas);
                    if (charsOcr > charsNativos) {
                        paginas = ocrPaginas;
                        metodo = charsNativos > 0 ? MetodoExtracao.HIBRIDO : MetodoExtracao.OCR_TESSERACT;
                        rel.setOcrUsado(true);
                        rel.addAviso("OCR Tesseract com pré-processamento (" + charsOcr + " chars).");
                    } else if (charsNativos < minCharsUteis) {
                        marcarFalha(ctx, rel, paginas.size(), Math.max(charsNativos, charsOcr),
                                "OCR executado mas o texto obtido continua insuficiente ("
                                        + charsOcr + " chars). Verifique a qualidade do scan, "
                                        + "o idioma (PIPELINE_OCR_LANGUAGE=por) e o DPI (recomendado 250–300).");
                        return;
                    }
                } else {
                    // Híbrido: funde OCR só nas páginas fracas
                    paginas = fundirPaginas(paginas, ocrPaginas, minCharsPorPagina);
                    long charsFinaisMerge = contarCharsUteis(paginas);
                    if (charsFinaisMerge > charsNativos) {
                        metodo = MetodoExtracao.HIBRIDO;
                        rel.setOcrUsado(true);
                        rel.addAviso("OCR selectivo em " + alvoOcr.size()
                                + " página(s) sem texto nativo (" + charsFinaisMerge + " chars no total).");
                    }
                }
            }
        }

        // 3) Normalização jurídica
        paginas = textoJuridicoNormalizer.normalizarPaginas(paginas);
        long charsFinais = contarCharsUteis(paginas);

        if (paginas.isEmpty() || charsFinais < minCharsUteis) {
            marcarFalha(ctx, rel, paginas.size(), charsFinais,
                    "Após extracção e normalização restam apenas " + charsFinais
                            + " caracteres úteis — impossível estruturar artigos.");
            return;
        }

        ctx.setPaginas(paginas);
        rel.setMetodoExtracao(metodo);
        rel.setCaracteres(charsFinais);
        rel.setPaginas(paginas.size());

        log.info("Texto pronto: método={} páginas={} chars={}", metodo, paginas.size(), charsFinais);
    }


    /**
     * Mantém texto nativo forte; substitui páginas fracas pelo resultado OCR correspondente.
     */
    private List<PaginaTexto> fundirPaginas(List<PaginaTexto> nativas, List<PaginaTexto> ocr,
                                           int limiarChars) {
        java.util.Map<Integer, String> ocrMap = new java.util.HashMap<>();
        if (ocr != null) {
            for (PaginaTexto p : ocr) {
                ocrMap.put(p.numeroPagina(), p.texto() != null ? p.texto() : "");
            }
        }
        List<PaginaTexto> out = new java.util.ArrayList<>();
        for (PaginaTexto p : nativas) {
            String t = p.texto() != null ? p.texto().replace("\u000c", "").trim() : "";
            if (t.length() >= limiarChars) {
                out.add(p);
            } else {
                String o = ocrMap.getOrDefault(p.numeroPagina(), "");
                out.add(new PaginaTexto(p.numeroPagina(),
                        o.trim().length() > t.length() ? o : p.texto()));
            }
        }
        return out;
    }

    private long contarCharsUteis(List<PaginaTexto> paginas) {
        if (paginas == null) {
            return 0;
        }
        long total = 0;
        for (PaginaTexto p : paginas) {
            if (p.texto() == null) {
                continue;
            }
            String limpo = p.texto().replace("", "").trim();
            total += limpo.length();
        }
        return total;
    }

    private void marcarFalha(PipelineContexto ctx, DocumentoProcessamentoResultado rel,
                             int paginas, long chars, String mensagem) {
        rel.setMetodoExtracao(MetodoExtracao.NENHUM);
        rel.setCaracteres(chars);
        rel.setPaginas(paginas);
        rel.setArtigosEncontrados(0);
        rel.setConfianca(0);
        rel.addAviso(mensagem);
        if (ctx.getDocumento() != null) {
            ctx.getDocumento().setEstado(EstadoDocumento.FALHA_EXTRACAO);
            ctx.getDocumento().setRevisaoNecessaria(true);
            ctx.getDocumento().setObservacoesProcessamento(mensagem);
            String curta = mensagem.length() > 200 ? mensagem.substring(0, 200) : mensagem;
            ctx.getDocumento().setMensagemProgresso(curta);
            ctx.getDocumento().setProgressoPercentagem(0);
            if (ctx.getPdfAnalysis() != null && ctx.getPdfAnalysis().getTipo() != null) {
                try {
                    ctx.getDocumento().setTipoPdf(ctx.getPdfAnalysis().getTipo().name());
                } catch (Exception ignored) {
                }
            }
        }
        log.error("Falha extracção texto: {}", mensagem);
        throw new RegraNegocioException(mensagem);
    }
}
