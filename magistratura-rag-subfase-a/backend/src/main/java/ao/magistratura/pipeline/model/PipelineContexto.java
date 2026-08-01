package ao.magistratura.pipeline.model;

import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.Documento;
import ao.magistratura.service.pdf.ArtigoExtraido;
import ao.magistratura.service.pdf.DocumentoProcessamentoResultado;
import ao.magistratura.service.pdf.PaginaTexto;
import ao.magistratura.service.pdf.PdfAnalysisResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado mutável partilhado entre as etapas de uma execução do pipeline.
 * Não é uma entidade JPA — vive apenas durante o processamento.
 */
public class PipelineContexto {

    private final UUID execucaoId = UUID.randomUUID();
    private Documento documento;
    private Diploma diploma;
    private File ficheiro;
    private List<PaginaTexto> paginas = List.of();
    private List<ArtigoExtraido> artigosExtraidos = new ArrayList<>();
    private int ocorrenciasSoltas;
    private IncrementalDecision decisaoIncremental;
    private String mensagemErro;
    private boolean conhecimentoAutomaticoAtivo;
    private KnowledgeChangeSet knowledgeChangeSet;
    private PdfAnalysisResult pdfAnalysis;
    private DocumentoProcessamentoResultado resultadoProcessamento;

    public UUID getExecucaoId() {
        return execucaoId;
    }

    public Documento getDocumento() {
        return documento;
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
    }

    public Diploma getDiploma() {
        return diploma;
    }

    public void setDiploma(Diploma diploma) {
        this.diploma = diploma;
    }

    public File getFicheiro() {
        return ficheiro;
    }

    public void setFicheiro(File ficheiro) {
        this.ficheiro = ficheiro;
    }

    public List<PaginaTexto> getPaginas() {
        return paginas;
    }

    public void setPaginas(List<PaginaTexto> paginas) {
        this.paginas = paginas != null ? paginas : List.of();
    }

    public List<ArtigoExtraido> getArtigosExtraidos() {
        return artigosExtraidos;
    }

    public void setArtigosExtraidos(List<ArtigoExtraido> artigosExtraidos) {
        this.artigosExtraidos = artigosExtraidos != null ? artigosExtraidos : new ArrayList<>();
    }

    public int getOcorrenciasSoltas() {
        return ocorrenciasSoltas;
    }

    public void setOcorrenciasSoltas(int ocorrenciasSoltas) {
        this.ocorrenciasSoltas = ocorrenciasSoltas;
    }

    public IncrementalDecision getDecisaoIncremental() {
        return decisaoIncremental;
    }

    public void setDecisaoIncremental(IncrementalDecision decisaoIncremental) {
        this.decisaoIncremental = decisaoIncremental;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public boolean isConhecimentoAutomaticoAtivo() {
        return conhecimentoAutomaticoAtivo;
    }

    public void setConhecimentoAutomaticoAtivo(boolean conhecimentoAutomaticoAtivo) {
        this.conhecimentoAutomaticoAtivo = conhecimentoAutomaticoAtivo;
    }

    public KnowledgeChangeSet getKnowledgeChangeSet() {
        return knowledgeChangeSet;
    }

    public void setKnowledgeChangeSet(KnowledgeChangeSet knowledgeChangeSet) {
        this.knowledgeChangeSet = knowledgeChangeSet;
    }

    public PdfAnalysisResult getPdfAnalysis() {
        return pdfAnalysis;
    }

    public void setPdfAnalysis(PdfAnalysisResult pdfAnalysis) {
        this.pdfAnalysis = pdfAnalysis;
    }

    public DocumentoProcessamentoResultado getResultadoProcessamento() {
        return resultadoProcessamento;
    }

    public void setResultadoProcessamento(DocumentoProcessamentoResultado resultadoProcessamento) {
        this.resultadoProcessamento = resultadoProcessamento;
    }
}
