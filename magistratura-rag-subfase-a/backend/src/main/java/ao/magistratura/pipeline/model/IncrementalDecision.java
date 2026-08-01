package ao.magistratura.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resultado do {@code IncrementalChangeDetector}: o que precisa de ser
 * processado ou regenerado. Não gera conhecimento — apenas decide.
 */
public class IncrementalDecision {

    public enum TipoDocumento {
        NOVO,
        DUPLICADO_IDENTICO,
        NOVA_VERSAO,
        REPROCESSAMENTO
    }

    private TipoDocumento tipo = TipoDocumento.NOVO;
    private boolean processarExtracao = true;
    private boolean regenerarIndexacao = true;
    /** Sempre false até a Fase 2 estar validada e a feature flag ativada. */
    private boolean regenerarConhecimento = false;
    private UUID documentoAnteriorId;
    private String motivo;
    private final List<String> artigosNovos = new ArrayList<>();
    private final List<String> artigosAlterados = new ArrayList<>();
    private final List<String> artigosRemovidos = new ArrayList<>();

    public TipoDocumento getTipo() {
        return tipo;
    }

    public void setTipo(TipoDocumento tipo) {
        this.tipo = tipo;
    }

    public boolean isProcessarExtracao() {
        return processarExtracao;
    }

    public void setProcessarExtracao(boolean processarExtracao) {
        this.processarExtracao = processarExtracao;
    }

    public boolean isRegenerarIndexacao() {
        return regenerarIndexacao;
    }

    public void setRegenerarIndexacao(boolean regenerarIndexacao) {
        this.regenerarIndexacao = regenerarIndexacao;
    }

    public boolean isRegenerarConhecimento() {
        return regenerarConhecimento;
    }

    public void setRegenerarConhecimento(boolean regenerarConhecimento) {
        this.regenerarConhecimento = regenerarConhecimento;
    }

    public UUID getDocumentoAnteriorId() {
        return documentoAnteriorId;
    }

    public void setDocumentoAnteriorId(UUID documentoAnteriorId) {
        this.documentoAnteriorId = documentoAnteriorId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public List<String> getArtigosNovos() {
        return artigosNovos;
    }

    public List<String> getArtigosAlterados() {
        return artigosAlterados;
    }

    public List<String> getArtigosRemovidos() {
        return artigosRemovidos;
    }
}
