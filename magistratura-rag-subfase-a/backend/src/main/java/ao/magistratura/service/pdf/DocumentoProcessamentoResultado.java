package ao.magistratura.service.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Relatório interno de uma execução do pipeline de ingestão jurídica.
 * Persistido resumidamente em {@code documentos.observacoes_processamento}
 * e campos dedicados (método, confiança).
 */
public class DocumentoProcessamentoResultado {

    private int paginas;
    private MetodoExtracao metodoExtracao = MetodoExtracao.NENHUM;
    private long caracteres;
    private int artigosEncontrados;
    private int ocorrenciasSoltasArtigo;
    /** 0–100, heurística de qualidade da extracção. */
    private int confianca;
    private PdfTipo tipoPdf = PdfTipo.UNKNOWN;
    private boolean ocrUsado;
    private final List<String> avisos = new ArrayList<>();

    public int getPaginas() { return paginas; }
    public void setPaginas(int paginas) { this.paginas = paginas; }

    public MetodoExtracao getMetodoExtracao() { return metodoExtracao; }
    public void setMetodoExtracao(MetodoExtracao metodoExtracao) { this.metodoExtracao = metodoExtracao; }

    public long getCaracteres() { return caracteres; }
    public void setCaracteres(long caracteres) { this.caracteres = caracteres; }

    public int getArtigosEncontrados() { return artigosEncontrados; }
    public void setArtigosEncontrados(int artigosEncontrados) { this.artigosEncontrados = artigosEncontrados; }

    public int getOcorrenciasSoltasArtigo() { return ocorrenciasSoltasArtigo; }
    public void setOcorrenciasSoltasArtigo(int ocorrenciasSoltasArtigo) { this.ocorrenciasSoltasArtigo = ocorrenciasSoltasArtigo; }

    public int getConfianca() { return confianca; }
    public void setConfianca(int confianca) { this.confianca = confianca; }

    public PdfTipo getTipoPdf() { return tipoPdf; }
    public void setTipoPdf(PdfTipo tipoPdf) { this.tipoPdf = tipoPdf; }

    public boolean isOcrUsado() { return ocrUsado; }
    public void setOcrUsado(boolean ocrUsado) { this.ocrUsado = ocrUsado; }

    public List<String> getAvisos() { return avisos; }

    public void addAviso(String a) {
        if (a != null && !a.isBlank()) {
            avisos.add(a);
        }
    }

    public String resumoObservacoes(String pipelineVersao) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Pipeline %s | método=%s | tipo=%s | páginas=%d | chars=%d | artigos=%d | confiança=%d%%",
                pipelineVersao,
                metodoExtracao,
                tipoPdf,
                paginas,
                caracteres,
                artigosEncontrados,
                confianca));
        if (!avisos.isEmpty()) {
            sb.append(" | avisos: ").append(String.join("; ", avisos));
        }
        return sb.toString();
    }
}
