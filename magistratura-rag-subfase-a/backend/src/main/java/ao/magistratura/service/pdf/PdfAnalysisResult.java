package ao.magistratura.service.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado da análise prévia de um PDF (antes da extracção completa / OCR).
 */
public class PdfAnalysisResult {

    private PdfTipo tipo = PdfTipo.UNKNOWN;
    private int paginas;
    private long tamanhoBytes;
    private long charsTextoNativo;
    private int paginasComTexto;
    private int paginasComImagem;
    private boolean encriptado;
    private boolean ocrNecessario;
    private String motivo;
    /** Código estável p.ex. PDF_PROTECTED_BLOCKED — para UI sem parsear texto. */
    private String codigoErro;
    private final List<String> avisos = new ArrayList<>();

    public PdfTipo getTipo() { return tipo; }
    public void setTipo(PdfTipo tipo) { this.tipo = tipo; }

    public int getPaginas() { return paginas; }
    public void setPaginas(int paginas) { this.paginas = paginas; }

    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public long getCharsTextoNativo() { return charsTextoNativo; }
    public void setCharsTextoNativo(long charsTextoNativo) { this.charsTextoNativo = charsTextoNativo; }

    public int getPaginasComTexto() { return paginasComTexto; }
    public void setPaginasComTexto(int paginasComTexto) { this.paginasComTexto = paginasComTexto; }

    public int getPaginasComImagem() { return paginasComImagem; }
    public void setPaginasComImagem(int paginasComImagem) { this.paginasComImagem = paginasComImagem; }

    public boolean isEncriptado() { return encriptado; }
    public void setEncriptado(boolean encriptado) { this.encriptado = encriptado; }

    public boolean isOcrNecessario() { return ocrNecessario; }
    public void setOcrNecessario(boolean ocrNecessario) { this.ocrNecessario = ocrNecessario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getCodigoErro() { return codigoErro; }
    public void setCodigoErro(String codigoErro) { this.codigoErro = codigoErro; }

    public List<String> getAvisos() { return avisos; }

    public void addAviso(String a) {
        if (a != null && !a.isBlank()) {
            avisos.add(a);
        }
    }
}
