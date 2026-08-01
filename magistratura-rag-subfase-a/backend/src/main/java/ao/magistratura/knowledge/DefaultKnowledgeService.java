package ao.magistratura.knowledge;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.RelacaoJuridica;
import ao.magistratura.entity.TopicoArtigo;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.knowledge.index.IndexingService;
import ao.magistratura.knowledge.retrieval.RetrievalEngine;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.RelacaoJuridicaRepository;
import ao.magistratura.repository.TopicoArtigoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * Knowledge Layer com âncora ontológica e expansão por relações PRESSUPOE.
 * <p>
 * Se a consulta tem {@code topicoId}, os artigos ligados ao tópico entram no
 * resultado; opcionalmente expandem-se tópicos que este <em>pressupõe</em>
 * (A PRESSUPOE B → ao estudar A, incluir B), com score atenuado.
 */
@Service
@RequiredArgsConstructor
public class DefaultKnowledgeService implements KnowledgeService {

    private static final String TIPO_PRESSUPOE = "PRESSUPOE";

    private final RetrievalEngine retrievalEngine;
    private final IndexingService indexingService;
    private final ArtigoRepository artigoRepository;
    private final TopicoArtigoRepository topicoArtigoRepository;
    private final RelacaoJuridicaRepository relacaoJuridicaRepository;

    /** Cache local com TTL — evita re-retrieval da mesma pergunta. */
    private final Cache<String, KnowledgeResult> queryCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .maximumSize(512)
            .recordStats()
            .build();

    @Value("${app.ontologia.rag-expandir-pressupoe:true}")
    private boolean expandirPressupoe;

    /** Profundidade máxima da cadeia PRESSUPOE (1 = só vizinhos directos). */
    @Value("${app.ontologia.rag-expansao-profundidade:1}")
    private int expansaoProfundidade;

    /** Multiplicador de score por cada nível de expansão (ex.: 0.65). */
    @Value("${app.ontologia.rag-expansao-factor-score:0.65}")
    private double expansaoFactorScore;

    /** Máximo de tópicos visitados na expansão (protecção). */
    @Value("${app.ontologia.rag-expansao-max-topicos:8}")
    private int expansaoMaxTopicos;

    @Override
    public KnowledgeResult search(KnowledgeQuery query) {
        if (query == null) {
            return KnowledgeResult.vazioResultado();
        }
        boolean emptyText = query.texto() == null || query.texto().isBlank();
        boolean hasAnchor = query.artigoId() != null
                || query.diplomaId() != null
                || query.topicoId() != null;
        if (emptyText && !hasAnchor) {
            return KnowledgeResult.vazioResultado();
        }
        if (query.artigoId() != null && emptyText && query.topicoId() == null) {
            return findArticle(query.artigoId())
                    .map(p -> new KnowledgeResult(List.of(p), "ID", 1, false))
                    .orElseGet(KnowledgeResult::vazioResultado);
        }
        if (query.topicoId() != null && emptyText && query.artigoId() == null && query.diplomaId() == null) {
            List<KnowledgePassage> ont = passagesOntologiaExpandida(
                    query.topicoId(), query.limite() > 0 ? query.limite() : 12);
            String est = expandirPressupoe ? "ONTOLOGIA+PRESSUPOE" : "ONTOLOGIA";
            return new KnowledgeResult(ont, est, ont.size(), ont.isEmpty());
        }

        String key = cacheKey(query);
        KnowledgeResult cached = queryCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        KnowledgeResult result = retrievalEngine.search(query);
        if (query.topicoId() != null) {
            result = fundirOntologia(result, query);
        }

        queryCache.put(key, result);
        return result;
    }

    @Override
    public Optional<KnowledgePassage> findArticle(UUID artigoId) {
        if (artigoId == null) {
            return Optional.empty();
        }
        return artigoRepository.findById(artigoId).map(this::toPassage);
    }

    @Override
    public Optional<KnowledgePassage> findByReference(UUID diplomaId, String artigoNumero) {
        if (diplomaId == null || artigoNumero == null || artigoNumero.isBlank()) {
            return Optional.empty();
        }
        String num = artigoNumero.trim();
        // Query indexada por diploma + número (evita carregar todos os artigos do diploma)
        String numLimpo = num.replaceFirst("(?i)^(art\\.?|artigo)\\s*", "").trim();
        return artigoRepository.buscarPorNumeroEDiplomaId(numLimpo, diplomaId).stream()
                .findFirst()
                .map(this::toPassage);
    }

    @Override
    public void indexArtigo(UUID artigoId) {
        indexingService.indexArtigo(artigoId);
        queryCache.invalidateAll();
    }

    @Override
    public void removeByDocumento(UUID documentoId) {
        indexingService.removeByDocumento(documentoId);
        queryCache.invalidateAll();
    }

    private KnowledgeResult fundirOntologia(KnowledgeResult base, KnowledgeQuery query) {
        int limit = query.limite() > 0 ? query.limite() : 8;
        List<KnowledgePassage> ont = passagesOntologiaExpandida(query.topicoId(), limit * 2);
        if (ont.isEmpty()) {
            return base;
        }

        Map<UUID, KnowledgePassage> porArtigo = new LinkedHashMap<>();
        for (KnowledgePassage p : ont) {
            if (p.artigoId() != null) {
                porArtigo.put(p.artigoId(), p);
            }
        }
        List<KnowledgePassage> basePassagens = base != null && base.passagens() != null
                ? base.passagens() : List.of();
        for (KnowledgePassage p : basePassagens) {
            if (p.artigoId() == null) {
                continue;
            }
            if (porArtigo.containsKey(p.artigoId())) {
                KnowledgePassage o = porArtigo.get(p.artigoId());
                double score = Math.max(o.score(), p.score()) + 0.15;
                porArtigo.put(p.artigoId(), new KnowledgePassage(
                        p.id(), p.artigoId(), p.diplomaId(), p.documentoId(), p.kind(),
                        p.diplomaTitulo(), p.diplomaNumero(), p.artigoNumero(), p.artigoTitulo(),
                        p.capitulo(), p.seccao(), p.texto(),
                        mergeMetodo(o.metodo(), p.metodo()),
                        Math.min(score, 1.5)));
            } else {
                porArtigo.putIfAbsent(p.artigoId(), p);
            }
        }

        List<KnowledgePassage> merged = new ArrayList<>(porArtigo.values());
        merged.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (merged.size() > limit) {
            merged = new ArrayList<>(merged.subList(0, limit));
        }
        String estrategia = (base != null ? base.estrategia() : "NENHUMA")
                + (expandirPressupoe ? "+ONTOLOGIA+PRESSUPOE" : "+ONTOLOGIA");
        int candidatos = (base != null ? base.totalCandidatos() : 0) + ont.size();
        return new KnowledgeResult(merged, estrategia, candidatos, merged.isEmpty());
    }

    /**
     * Artigos do tópico + artigos de tópicos alcançados por cadeias PRESSUPOE
     * (A PRESSUPOE B → B é pré-requisito de A).
     */
    private List<KnowledgePassage> passagesOntologiaExpandida(UUID topicoRaiz, int limite) {
        if (topicoRaiz == null || limite <= 0) {
            return List.of();
        }

        // topicoId → factor de score (1.0 = tópico pedido)
        Map<UUID, Double> topicosComFactor = new LinkedHashMap<>();
        topicosComFactor.put(topicoRaiz, 1.0);

        if (expandirPressupoe) {
            expandirPressupoe(topicoRaiz, topicosComFactor);
        }

        Map<UUID, KnowledgePassage> porArtigo = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> e : topicosComFactor.entrySet()) {
            UUID topicoId = e.getKey();
            double factor = e.getValue();
            boolean primario = topicoId.equals(topicoRaiz);
            List<TopicoArtigo> ligacoes = topicoArtigoRepository.findByTopicoIdComArtigo(topicoId);
            for (TopicoArtigo ta : ligacoes) {
                Artigo a = ta.getArtigo();
                if (a == null || a.getId() == null) {
                    continue;
                }
                float rel = ta.getRelevancia() != null ? ta.getRelevancia() : 1f;
                double score = Math.min(1.0, (0.75 + rel * 0.25) * factor);
                String metodo = primario ? "ONTOLOGIA" : "ONTOLOGIA_PRESSUPOE";
                KnowledgePassage p = toPassageOntologia(a, score, metodo);
                KnowledgePassage existente = porArtigo.get(a.getId());
                if (existente == null || p.score() > existente.score()) {
                    porArtigo.put(a.getId(), p);
                }
            }
        }

        List<KnowledgePassage> out = new ArrayList<>(porArtigo.values());
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (out.size() > limite) {
            out = new ArrayList<>(out.subList(0, limite));
        }
        return out;
    }

    /**
     * BFS sobre arestas PRESSUPOE a partir da origem (tópico consultado).
     * Segue origem → destino (A pressupõe B → visita B).
     */
    private void expandirPressupoe(UUID topicoRaiz, Map<UUID, Double> topicosComFactor) {
        int profundidadeMax = Math.max(1, expansaoProfundidade);
        double factorBase = expansaoFactorScore <= 0 || expansaoFactorScore > 1
                ? 0.65 : expansaoFactorScore;
        int maxTopicos = Math.max(2, expansaoMaxTopicos);

        Queue<UUID> fila = new ArrayDeque<>();
        Map<UUID, Integer> profundidade = new LinkedHashMap<>();
        fila.add(topicoRaiz);
        profundidade.put(topicoRaiz, 0);

        while (!fila.isEmpty() && topicosComFactor.size() < maxTopicos) {
            UUID actual = fila.poll();
            int prof = profundidade.getOrDefault(actual, 0);
            if (prof >= profundidadeMax) {
                continue;
            }
            List<RelacaoJuridica> saidas = relacaoJuridicaRepository.findByOrigemId(actual);
            for (RelacaoJuridica r : saidas) {
                if (r.getTipoRelacao() == null
                        || !TIPO_PRESSUPOE.equalsIgnoreCase(r.getTipoRelacao().trim())) {
                    continue;
                }
                if (r.getDestino() == null || r.getDestino().getId() == null) {
                    continue;
                }
                UUID destId = r.getDestino().getId();
                if (topicosComFactor.containsKey(destId)) {
                    continue;
                }
                float pesoRel = r.getPeso() != null ? r.getPeso() : 1f;
                double factorNivel = Math.pow(factorBase, prof + 1) * Math.min(1.0, pesoRel);
                topicosComFactor.put(destId, factorNivel);
                profundidade.put(destId, prof + 1);
                fila.add(destId);
                if (topicosComFactor.size() >= maxTopicos) {
                    break;
                }
            }
        }
    }

    private static String mergeMetodo(String ont, String rag) {
        String o = ont != null ? ont : "ONTOLOGIA";
        String r = rag != null ? rag : "RAG";
        return o + "+" + r;
    }

    private KnowledgePassage toPassageOntologia(Artigo a, double score, String metodo) {
        return new KnowledgePassage(
                a.getId(),
                a.getId(),
                a.getDiploma() != null ? a.getDiploma().getId() : null,
                a.getDocumento() != null ? a.getDocumento().getId() : null,
                KnowledgeContentKind.LEGISLACAO,
                a.getDiploma() != null ? a.getDiploma().getTitulo() : null,
                a.getDiploma() != null ? a.getDiploma().getNumero() : null,
                a.getNumero(),
                a.getTitulo(),
                a.getCapitulo(),
                a.getSeccao(),
                a.getTexto(),
                metodo,
                score);
    }

    private KnowledgePassage toPassage(Artigo a) {
        return new KnowledgePassage(
                a.getId(),
                a.getId(),
                a.getDiploma() != null ? a.getDiploma().getId() : null,
                a.getDocumento() != null ? a.getDocumento().getId() : null,
                KnowledgeContentKind.LEGISLACAO,
                a.getDiploma() != null ? a.getDiploma().getTitulo() : null,
                a.getDiploma() != null ? a.getDiploma().getNumero() : null,
                a.getNumero(),
                a.getTitulo(),
                a.getCapitulo(),
                a.getSeccao(),
                a.getTexto(),
                "ID",
                1.0);
    }

    private static String cacheKey(KnowledgeQuery q) {
        return String.valueOf(q.texto() != null ? q.texto().trim().toLowerCase(Locale.ROOT) : "")
                + "|" + q.diplomaId() + "|" + q.artigoId() + "|" + q.topicoId()
                + "|" + q.limite() + "|" + q.hibrido();
    }
}
