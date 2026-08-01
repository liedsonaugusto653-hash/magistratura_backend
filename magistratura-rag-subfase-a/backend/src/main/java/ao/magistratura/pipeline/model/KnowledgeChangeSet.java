package ao.magistratura.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conjunto mínimo de alterações a passar ao KnowledgeGenerator.
 * Nunca o diploma completo — apenas o que mudou.
 */
public class KnowledgeChangeSet {

    private UUID documentoId;
    private UUID diplomaId;
    private final List<UUID> artigosNovosIds = new ArrayList<>();
    private final List<UUID> artigosAlteradosIds = new ArrayList<>();
    private final List<UUID> artigosRemovidosIds = new ArrayList<>();
    private final List<String> artigosRemovidosNumeros = new ArrayList<>();

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getDiplomaId() {
        return diplomaId;
    }

    public void setDiplomaId(UUID diplomaId) {
        this.diplomaId = diplomaId;
    }

    public List<UUID> getArtigosNovosIds() {
        return artigosNovosIds;
    }

    public List<UUID> getArtigosAlteradosIds() {
        return artigosAlteradosIds;
    }

    public List<UUID> getArtigosRemovidosIds() {
        return artigosRemovidosIds;
    }

    public List<String> getArtigosRemovidosNumeros() {
        return artigosRemovidosNumeros;
    }

    public boolean isEmpty() {
        return artigosNovosIds.isEmpty()
                && artigosAlteradosIds.isEmpty()
                && artigosRemovidosIds.isEmpty()
                && artigosRemovidosNumeros.isEmpty();
    }
}
