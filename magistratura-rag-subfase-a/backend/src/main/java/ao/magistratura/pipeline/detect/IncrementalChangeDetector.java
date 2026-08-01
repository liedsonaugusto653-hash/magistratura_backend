package ao.magistratura.pipeline.detect;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Documento;
import ao.magistratura.pipeline.model.IncrementalDecision;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DocumentoRepository;
import ao.magistratura.service.pdf.ArtigoExtraido;
import ao.magistratura.util.ContentHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IncrementalChangeDetector {

    private final DocumentoRepository documentoRepository;
    private final ArtigoRepository artigoRepository;

    public IncrementalDecision avaliarDocumentoExistente(Documento documento) {
        IncrementalDecision d = new IncrementalDecision();
        Optional<Documento> mesmoHash = documentoRepository.findByHashFicheiro(documento.getHashFicheiro());
        if (mesmoHash.isPresent() && !mesmoHash.get().getId().equals(documento.getId())) {
            d.setTipo(IncrementalDecision.TipoDocumento.DUPLICADO_IDENTICO);
            d.setProcessarExtracao(false);
            d.setRegenerarIndexacao(false);
            d.setRegenerarConhecimento(false);
            d.setDocumentoAnteriorId(mesmoHash.get().getId());
            d.setMotivo("Hash SHA-256 idêntico a documento já existente.");
            return d;
        }
        d.setTipo(IncrementalDecision.TipoDocumento.REPROCESSAMENTO);
        d.setMotivo("Reprocessamento do documento (mesmo id / novo pipeline).");
        d.setProcessarExtracao(true);
        d.setRegenerarIndexacao(true);
        d.setRegenerarConhecimento(false);
        return d;
    }

    /**
     * Diff por hash de conteúdo do artigo quando disponível; fallback para texto normalizado.
     */
    public IncrementalDecision avaliarAposExtracao(PipelineContexto ctx) {
        IncrementalDecision d = ctx.getDecisaoIncremental() != null
                ? ctx.getDecisaoIncremental()
                : new IncrementalDecision();

        // Artigos ainda na BD antes da persistência (reprocessamento)
        List<Artigo> existentes = artigoRepository.findByDocumentoIdOrderByOrdemAsc(ctx.getDocumento().getId());
        Map<String, Artigo> porNumero = existentes.stream()
                .filter(a -> a.getNumero() != null)
                .collect(Collectors.toMap(Artigo::getNumero, Function.identity(), (a, b) -> a));

        for (ArtigoExtraido extraido : ctx.getArtigosExtraidos()) {
            Artigo antigo = porNumero.get(extraido.numero());
            String hashNovo = ContentHashUtil.hashArtigo(extraido.numero(), extraido.titulo(), extraido.texto());
            if (antigo == null) {
                d.getArtigosNovos().add(extraido.numero());
            } else {
                String hashAntigo = antigo.getHashConteudo();
                if (hashAntigo == null) {
                    hashAntigo = ContentHashUtil.hashArtigo(antigo.getNumero(), antigo.getTitulo(), antigo.getTexto());
                }
                if (!Objects.equals(hashAntigo, hashNovo)) {
                    d.getArtigosAlterados().add(extraido.numero());
                }
            }
        }

        for (Artigo antigo : existentes) {
            boolean ainda = ctx.getArtigosExtraidos().stream()
                    .anyMatch(e -> Objects.equals(e.numero(), antigo.getNumero()));
            if (!ainda && antigo.getNumero() != null) {
                d.getArtigosRemovidos().add(antigo.getNumero());
            }
        }

        if (existentes.isEmpty()) {
            d.setTipo(IncrementalDecision.TipoDocumento.NOVO);
            d.setMotivo("Primeiro processamento deste documento.");
        } else if (d.getArtigosNovos().isEmpty() && d.getArtigosAlterados().isEmpty()
                && d.getArtigosRemovidos().isEmpty()) {
            d.setMotivo("Hashes de artigos idênticos ao processamento anterior.");
        } else {
            d.setTipo(IncrementalDecision.TipoDocumento.NOVA_VERSAO);
            d.setMotivo(String.format(
                    "Alterações por hash: +%d novos, ~%d alterados, -%d removidos",
                    d.getArtigosNovos().size(),
                    d.getArtigosAlterados().size(),
                    d.getArtigosRemovidos().size()));
        }

        d.setRegenerarConhecimento(false);
        return d;
    }
}
