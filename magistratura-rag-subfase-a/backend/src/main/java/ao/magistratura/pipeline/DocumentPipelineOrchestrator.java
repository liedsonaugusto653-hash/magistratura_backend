package ao.magistratura.pipeline;

import ao.magistratura.entity.Documento;
import ao.magistratura.entity.EstadoDocumento;
import ao.magistratura.pipeline.audit.PipelineAuditService;
import ao.magistratura.pipeline.detect.IncrementalChangeDetector;
import ao.magistratura.pipeline.event.PipelineEvents;
import ao.magistratura.pipeline.index.KnowledgeIndexer;
import ao.magistratura.pipeline.model.*;
import ao.magistratura.pipeline.stage.*;
import ao.magistratura.knowledge.metrics.PipelineMetricsCollector;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DocumentoRepository;
import ao.magistratura.service.pdf.DocumentoProcessamentoResultado;
import ao.magistratura.service.pdf.MetodoExtracao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentPipelineOrchestrator.class);
    private static final double LIMIAR_REVISAO = 0.6;

    /** Ordem canónica das etapas de processamento (para reexecução parcial). */
    private static final List<PipelineEtapa> ORDEM_PROCESSAMENTO = List.of(
            PipelineEtapa.VALIDADO,
            PipelineEtapa.ANALISANDO_PDF,
            PipelineEtapa.EXTRAINDO_PDF,
            PipelineEtapa.EXTRAINDO_METADADOS,
            PipelineEtapa.EXTRAINDO_ESTRUTURA,
            PipelineEtapa.PERSISTINDO_ARTIGOS,
            PipelineEtapa.INDEXANDO,
            PipelineEtapa.LIGANDO_ONTOLOGIA,
            PipelineEtapa.GERANDO_CONHECIMENTO
    );

    private final DocumentValidatorStage documentValidatorStage;
    private final PdfAnalysisStage pdfAnalysisStage;
    private final PdfTextExtractorStage pdfTextExtractorStage;
    private final MetadataExtractorStage metadataExtractorStage;
    private final StructureExtractorStage structureExtractorStage;
    private final ArticlePersistenceStage articlePersistenceStage;
    private final KnowledgeIndexerStage knowledgeIndexerStage;
    private final OntologiaLigacaoStage ontologiaLigacaoStage;
    private final KnowledgeGeneratorStage knowledgeGeneratorStage;
    private final IncrementalChangeDetector incrementalChangeDetector;
    private final PipelineAuditService pipelineAuditService;
    private final DocumentoRepository documentoRepository;
    private final ArtigoRepository artigoRepository;
    private final KnowledgeIndexer knowledgeIndexer;
    private final ApplicationEventPublisher events;
    private final PipelineMetricsCollector pipelineMetricsCollector;

    // Sem @Transactional: TX longa deadlocks com DocumentoProgressoService (REQUIRES_NEW).
    public Documento executar(PipelineContexto ctx) {
        return executarDesde(ctx, null);
    }

    // Sem @Transactional: TX longa deadlocks com DocumentoProgressoService (REQUIRES_NEW).
    public Documento reprocessar(PipelineContexto ctx) {
        Documento doc = ctx.getDocumento();
        knowledgeIndexer.removeByDocumento(doc.getId());
        artigoRepository.deleteByDocumentoId(doc.getId());
        doc.setUltimaEtapaOk(null);
        doc.setObservacoesProcessamento(null);
        doc.setRevisaoNecessaria(false);
        doc.setMetodoExtracao(null);
        doc.setConfiancaExtracao(null);
        doc.setTipoPdf(null);
        documentoRepository.save(doc);
        pipelineAuditService.registarSimples(
                doc.getId(), PipelineEtapa.RECEBIDO, PipelineResultado.SUCESSO,
                "Reprocessamento: derivados limpos, pipeline reiniciado",
                doc.getHashFicheiro(), null);
        return executarDesde(ctx, null);
    }

    // Sem @Transactional: TX longa deadlocks com DocumentoProgressoService (REQUIRES_NEW).
    public Documento executarDesde(PipelineContexto ctx, PipelineEtapa desde) {
        Documento documento = ctx.getDocumento();
        String hash = documento.getHashFicheiro();
        long t0 = System.currentTimeMillis();

        if (ctx.getResultadoProcessamento() == null) {
            ctx.setResultadoProcessamento(new DocumentoProcessamentoResultado());
        }

        events.publishEvent(new PipelineEvents.DocumentoRecebido(documento.getId(), hash));
        marcarEtapa(documento, PipelineEtapa.RECEBIDO, false);
        pipelineAuditService.registarSimples(
                documento.getId(), PipelineEtapa.RECEBIDO, PipelineResultado.SUCESSO,
                "Início pipeline " + PipelineVersion.ATUAL
                        + (desde != null ? " (desde " + desde + ")" : ""),
                hash, null);

        IncrementalDecision decisaoInicial = incrementalChangeDetector.avaliarDocumentoExistente(documento);
        ctx.setDecisaoIncremental(decisaoInicial);

        try {
            correrEtapa(ctx, PipelineEtapa.DETECAO_INCREMENTAL, () -> {},
                    "tipo=" + decisaoInicial.getTipo() + " motivo=" + decisaoInicial.getMotivo(), 0, 0);

            if (!decisaoInicial.isProcessarExtracao()) {
                documento.setEstado(EstadoDocumento.PROCESSADO);
                documento.setObservacoesProcessamento(decisaoInicial.getMotivo());
                marcarEtapa(documento, PipelineEtapa.CONCLUIDO, true);
                events.publishEvent(new PipelineEvents.PipelineConcluido(documento.getId(), 0));
                return documentoRepository.save(documento);
            }

            documento.setEstado(EstadoDocumento.PROCESSANDO);
            documentoRepository.save(documento);

            List<PipelineStage> stages = montarStages(desde, documento.getUltimaEtapaOk());

            for (PipelineStage stage : stages) {
                correrEtapa(ctx, stage);
                if (stage.etapa() == PipelineEtapa.EXTRAINDO_ESTRUTURA) {
                    IncrementalDecision apos = incrementalChangeDetector.avaliarAposExtracao(ctx);
                    ctx.setDecisaoIncremental(apos);
                    pipelineAuditService.registarSimples(
                            documento.getId(), PipelineEtapa.DETECAO_INCREMENTAL, PipelineResultado.SUCESSO,
                            apos.getMotivo(), hash, null, ctx.getArtigosExtraidos().size(), 0, null);
                }
            }

            if (ctx.getDecisaoIncremental() == null) {
                ctx.setDecisaoIncremental(decisaoInicial);
            }

            aplicarQualidade(ctx);

            documento = documentoRepository.save(documento);

            int n = ctx.getArtigosExtraidos() != null ? ctx.getArtigosExtraidos().size() : 0;
            pipelineAuditService.registarSimples(
                    documento.getId(), PipelineEtapa.CONCLUIDO, PipelineResultado.SUCESSO,
                    "artigos=" + n, hash, null, n, Boolean.TRUE.equals(documento.getRevisaoNecessaria()) ? 1 : 0, null);

            events.publishEvent(new PipelineEvents.PipelineConcluido(documento.getId(), n));
            pipelineMetricsCollector.registarExecucao(ctx, System.currentTimeMillis() - t0, 0, false);
            return documento;

        } catch (Exception e) {
            // Não gravar estado ERRO aqui: a TX actual será rollback e o save
            // seria desfeito. A persistência do erro é exclusiva de
            // DocumentoEstadoService.marcarErro (REQUIRES_NEW), invocado pelo job.
            log.error("Pipeline falhou documento {}: {}", documento.getId(), e.getMessage(), e);
            try {
                pipelineAuditService.registarSimples(
                        documento.getId(), PipelineEtapa.ERRO, PipelineResultado.ERRO,
                        null, hash, e.getMessage(), null, 0, e);
            } catch (Exception ignored) {
                // audit pode falhar se a TX já estiver rollback-only
            }
            events.publishEvent(new PipelineEvents.PipelineFalhou(
                    documento.getId(), PipelineEtapa.ERRO, e.getMessage()));
            try {
                pipelineMetricsCollector.registarExecucao(ctx, System.currentTimeMillis() - t0, 0, true);
            } catch (Exception ignored) {
            }
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }

    private List<PipelineStage> montarStages(PipelineEtapa desdeExplicito, String ultimaOk) {
        List<PipelineStage> todas = List.of(
                documentValidatorStage,
                pdfAnalysisStage,
                pdfTextExtractorStage,
                metadataExtractorStage,
                structureExtractorStage,
                articlePersistenceStage,
                knowledgeIndexerStage,
                ontologiaLigacaoStage,
                knowledgeGeneratorStage
        );

        PipelineEtapa inicio = desdeExplicito;
        if (inicio == null && ultimaOk != null) {
            try {
                PipelineEtapa ok = PipelineEtapa.valueOf(ultimaOk);
                int idx = ORDEM_PROCESSAMENTO.indexOf(ok);
                if (idx >= 0 && idx + 1 < ORDEM_PROCESSAMENTO.size()) {
                    inicio = ORDEM_PROCESSAMENTO.get(idx + 1);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (inicio == null) {
            return new ArrayList<>(todas);
        }

        List<PipelineStage> filtradas = new ArrayList<>();
        boolean incluir = false;
        for (PipelineStage s : todas) {
            if (s.etapa() == inicio) {
                incluir = true;
            }
            if (incluir) {
                filtradas.add(s);
            }
        }
        return filtradas.isEmpty() ? new ArrayList<>(todas) : filtradas;
    }

    private void correrEtapa(PipelineContexto ctx, PipelineStage stage) throws Exception {
        correrEtapa(ctx, stage.etapa(), () -> stage.executar(ctx), null, null, null);
    }

    private void correrEtapa(PipelineContexto ctx, PipelineEtapa etapa, StageAction action,
                             String detalheFixo, Integer artigos, Integer avisos) throws Exception {
        Documento doc = ctx.getDocumento();
        marcarEtapa(doc, etapa, false);
        UUID registoId = pipelineAuditService.iniciar(doc.getId(), etapa, doc.getHashFicheiro());
        try {
            action.run();
            String detalhe = detalheFixo != null ? detalheFixo : detalheEtapa(ctx, etapa);
            Integer art = artigos != null ? artigos : (
                    etapa == PipelineEtapa.EXTRAINDO_ESTRUTURA || etapa == PipelineEtapa.PERSISTINDO_ARTIGOS
                            ? ctx.getArtigosExtraidos().size() : null);
            int av = avisos != null ? avisos : 0;
            pipelineAuditService.concluir(registoId, PipelineResultado.SUCESSO, detalhe, null, art, av, null);
            if (ORDEM_PROCESSAMENTO.contains(etapa)) {
                doc.setUltimaEtapaOk(etapa.name());
                documentoRepository.save(doc);
            }
            events.publishEvent(new PipelineEvents.EtapaConcluida(doc.getId(), etapa, true, detalhe));
        } catch (Exception e) {
            pipelineAuditService.concluir(registoId, PipelineResultado.ERRO, null, e.getMessage(),
                    null, 0, e);
            events.publishEvent(new PipelineEvents.EtapaConcluida(doc.getId(), etapa, false, e.getMessage()));
            throw e;
        }
    }

    private String detalheEtapa(PipelineContexto ctx, PipelineEtapa etapa) {
        return switch (etapa) {
            case ANALISANDO_PDF -> {
                var a = ctx.getPdfAnalysis();
                yield a == null ? null : "tipo=" + a.getTipo() + " ocr=" + a.isOcrNecessario()
                        + " chars~" + a.getCharsTextoNativo();
            }
            case EXTRAINDO_PDF -> {
                var r = ctx.getResultadoProcessamento();
                yield r == null ? "paginas=" + ctx.getPaginas().size()
                        : "metodo=" + r.getMetodoExtracao() + " chars=" + r.getCaracteres();
            }
            case EXTRAINDO_ESTRUTURA -> "artigos=" + ctx.getArtigosExtraidos().size()
                    + " soltas=" + ctx.getOcorrenciasSoltas();
            case PERSISTINDO_ARTIGOS -> "persistidos=" + ctx.getArtigosExtraidos().size();
            case GERANDO_CONHECIMENTO -> {
                var cs = ctx.getKnowledgeChangeSet();
                yield ctx.isConhecimentoAutomaticoAtivo()
                        ? "changeSet novos=" + (cs != null ? cs.getArtigosNovosIds().size() : 0)
                        : "desativado";
            }
            default -> null;
        };
    }

    /**
     * Qualidade final: nunca marca PROCESSADO puro com 0 artigos.
     * Calcula confiança heurística e preenche relatório.
     */
    private void aplicarQualidade(PipelineContexto ctx) {
        Documento doc = ctx.getDocumento();
        int extraidos = ctx.getArtigosExtraidos() != null ? ctx.getArtigosExtraidos().size() : 0;
        int soltas = ctx.getOcorrenciasSoltas();

        DocumentoProcessamentoResultado rel = ctx.getResultadoProcessamento();
        if (rel == null) {
            rel = new DocumentoProcessamentoResultado();
            ctx.setResultadoProcessamento(rel);
        }
        rel.setArtigosEncontrados(extraidos);
        rel.setOcorrenciasSoltasArtigo(soltas);

        int confianca = calcularConfianca(rel, extraidos, soltas);
        rel.setConfianca(confianca);

        boolean zeroArtigos = extraidos == 0;
        boolean desvioContagem = soltas > 0 && extraidos < soltas * LIMIAR_REVISAO;
        boolean ocr = rel.isOcrUsado() || rel.getMetodoExtracao() == MetodoExtracao.OCR_TESSERACT
                || rel.getMetodoExtracao() == MetodoExtracao.HIBRIDO;
        boolean revisao = zeroArtigos || desvioContagem || confianca < 50;

        if (zeroArtigos) {
            rel.addAviso("Nenhum artigo estruturado — documento marcado para revisão (não é sucesso silencioso).");
        }
        if (desvioContagem) {
            rel.addAviso(String.format("Contagem baixa: %d artigos vs %d ocorrências soltas.", extraidos, soltas));
        }
        if (ocr) {
            rel.addAviso("Texto obtido via OCR — validar artigos críticos manualmente.");
        }

        doc.setRevisaoNecessaria(revisao);
        doc.setMetodoExtracao(rel.getMetodoExtracao() != null ? rel.getMetodoExtracao().name() : null);
        doc.setConfiancaExtracao(confianca);
        if (rel.getTipoPdf() != null) {
            doc.setTipoPdf(rel.getTipoPdf().name());
        }
        doc.setObservacoesProcessamento(rel.resumoObservacoes(PipelineVersion.ATUAL));

        if (zeroArtigos) {
            // Não é FALHA_EXTRACAO (houve texto) — é processamento com aviso grave
            doc.setEstado(EstadoDocumento.PROCESSADO_COM_AVISOS);
            doc.setMensagemProgresso("Processado, mas não foram encontrados artigos. Convém rever o documento.");
            log.warn("Documento {}: 0 artigos (ocorrências soltas={}) — PROCESSADO_COM_AVISOS",
                    doc.getId(), soltas);
        } else if (revisao || ocr || confianca < 70) {
            doc.setEstado(EstadoDocumento.PROCESSADO_COM_AVISOS);
            doc.setMensagemProgresso(extraidos + " artigos extraídos — recomenda-se uma revisão rápida.");
        } else {
            doc.setEstado(EstadoDocumento.PROCESSADO);
            doc.setMensagemProgresso(extraidos + " artigos extraídos com sucesso.");
        }
        doc.setProgressoPercentagem(100);
        if (doc.getProgressoPaginasTotal() != null) {
            doc.setProgressoPaginasOk(doc.getProgressoPaginasTotal());
        }

        marcarEtapa(doc, PipelineEtapa.CONCLUIDO, true);
    }

    private int calcularConfianca(DocumentoProcessamentoResultado rel, int extraidos, int soltas) {
        if (rel.getCaracteres() <= 0) {
            return 0;
        }
        int base = 40;
        if (extraidos > 0) {
            base += 30;
        }
        if (soltas > 0 && extraidos > 0) {
            double ratio = Math.min(1.0, (double) extraidos / soltas);
            base += (int) (20 * ratio);
        } else if (extraidos > 0) {
            base += 15;
        }
        if (rel.getMetodoExtracao() == MetodoExtracao.PDFBOX) {
            base += 10;
        } else if (rel.getMetodoExtracao() == MetodoExtracao.OCR_TESSERACT) {
            base -= 5;
        }
        return Math.max(0, Math.min(100, base));
    }

    /**
     * Persiste etapa. Sem TX pai, o {@code save} do Spring Data commit de imediato
     * (TX curta), libertando o lock da linha para o progresso OCR.
     */
    private void marcarEtapa(Documento documento, PipelineEtapa etapa, boolean sucessoFinal) {
        documento.setPipelineEtapa(etapa.name());
        documento.setPipelineVersao(PipelineVersion.ATUAL);
        if (sucessoFinal) {
            documento.setUltimaEtapaOk(etapa.name());
        }
        documentoRepository.save(documento);
    }

    @FunctionalInterface
    private interface StageAction {
        void run() throws Exception;
    }
}
