package ao.magistratura.knowledge.origin;

import ao.magistratura.entity.Artigo;
import ao.magistratura.pipeline.model.PipelineVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeOriginService {

    private final KnowledgeOriginRepository repository;

    /**
     * Obtém ou cria a origem lógica para um artigo persistido e atualiza o id JPA atual.
     */
    @Transactional
    public KnowledgeOrigin upsertForArtigo(Artigo artigo) {
        UUID documentoId = artigo.getDocumento() != null ? artigo.getDocumento().getId() : null;
        UUID diplomaId = artigo.getDiploma() != null ? artigo.getDiploma().getId() : null;
        String key = KnowledgeOrigin.keyFor(documentoId, artigo.getNumero());

        KnowledgeOrigin origin = repository.findByOriginKey(key).orElseGet(() ->
                KnowledgeOrigin.builder()
                        .originKey(key)
                        .documentoId(documentoId)
                        .diplomaId(diplomaId)
                        .artigoNumero(artigo.getNumero())
                        .build());

        origin.setArtigoIdAtual(artigo.getId());
        origin.setArtigoHash(artigo.getHashConteudo());
        origin.setDocumentoId(documentoId);
        origin.setDiplomaId(diplomaId);
        origin.setPipelineVersion(PipelineVersion.ATUAL);
        return repository.save(origin);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<KnowledgeOrigin> findByKey(String originKey) {
        return repository.findByOriginKey(originKey);
    }
}
