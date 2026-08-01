package ao.magistratura.service;

import ao.magistratura.dto.ontologia.*;
import ao.magistratura.entity.*;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.ia.IaJsonParser;
import ao.magistratura.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada conceptual do conhecimento jurídico (ontologia).
 * Não substitui diplomas/artigos — liga conceitos a artigos existentes.
 */
@Service
@RequiredArgsConstructor
public class OntologiaService {

    private static final Logger log = LoggerFactory.getLogger(OntologiaService.class);
    private final EntidadeJuridicaRepository entidadeRepo;
    private final TopicoJuridicoRepository topicoRepo;
    private final RelacaoJuridicaRepository relacaoRepo;
    private final TopicoArtigoRepository topicoArtigoRepo;
    private final ArtigoRepository artigoRepo;
    private final OntologiaFichaService ontologiaFichaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<EntidadeJuridicaResponse> listarEntidades() {
        return entidadeRepo.findByActivoTrueOrderByOrdemAscNomeAsc().stream()
                .map(e -> {
                    int n = topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(e.getId()).size();
                    return toEntidade(e, n);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EntidadeJuridicaResponse obterEntidade(UUID id) {
        EntidadeJuridica e = entidadeRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entidade jurídica não encontrada"));
        int n = topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(e.getId()).size();
        return toEntidade(e, n);
    }

    @Transactional(readOnly = true)
    public EntidadeJuridicaResponse obterEntidadePorCodigo(String codigo) {
        EntidadeJuridica e = entidadeRepo.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entidade não encontrada: " + codigo));
        int n = topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(e.getId()).size();
        return toEntidade(e, n);
    }

    @Transactional(readOnly = true)
    public List<TopicoJuridicoResponse> listarTopicosPorEntidade(UUID entidadeId) {
        if (!entidadeRepo.existsById(entidadeId)) {
            throw new RecursoNaoEncontradoException("Entidade jurídica não encontrada");
        }
        return topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(entidadeId).stream()
                .map(t -> toTopico(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicoJuridicoResponse> pesquisarTopicos(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        return topicoRepo.pesquisar(termo.trim()).stream()
                .map(t -> toTopico(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicoJuridicoResponse obterTopico(UUID id, boolean comRelacoes) {
        TopicoJuridico t = topicoRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tópico jurídico não encontrado"));
        return toTopico(t, comRelacoes);
    }

    /**
     * Mapa de estudo por conceito: entidade + tópicos + artigos ligados (todos os tópicos da entidade).
     */
    @Transactional(readOnly = true)
    public MapaConceitoResponse mapaPorEntidade(UUID entidadeId) {
        EntidadeJuridica e = entidadeRepo.findById(entidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entidade jurídica não encontrada"));
        List<TopicoJuridico> topicos = topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(entidadeId);
        List<TopicoJuridicoResponse> topicoDtos = topicos.stream()
                .map(t -> toTopico(t, true))
                .toList();
        List<TopicoArtigoResponse> artigos = new ArrayList<>();
        for (TopicoJuridico t : topicos) {
            artigos.addAll(listarArtigosDoTopico(t.getId()));
        }
        return new MapaConceitoResponse(toEntidade(e, topicos.size()), topicoDtos, artigos);
    }

    @Transactional(readOnly = true)
    public List<TopicoArtigoResponse> listarArtigosDoTopico(UUID topicoId) {
        if (!topicoRepo.existsById(topicoId)) {
            throw new RecursoNaoEncontradoException("Tópico jurídico não encontrado");
        }
        return topicoArtigoRepo.findByTopicoIdComArtigo(topicoId).stream()
                .map(this::toTopicoArtigo)
                .toList();
    }

    @Transactional
    public TopicoArtigoResponse ligarArtigo(UUID topicoId, LigarArtigoRequest req) {
        TopicoJuridico topico = topicoRepo.findById(topicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tópico jurídico não encontrado"));
        Artigo artigo = artigoRepo.findById(req.artigoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));

        topicoArtigoRepo.findByTopicoIdAndArtigoId(topicoId, req.artigoId()).ifPresent(x -> {
            throw new RegraNegocioException("Este artigo já está ligado a este tópico");
        });

        float rel = req.relevancia() != null ? clamp(req.relevancia()) : 1.0f;
        String origem = (req.origemLigacao() == null || req.origemLigacao().isBlank())
                ? "MANUAL" : req.origemLigacao().trim().toUpperCase();

        TopicoArtigo ligacao = TopicoArtigo.builder()
                .topico(topico)
                .artigo(artigo)
                .relevancia(rel)
                .origemLigacao(origem)
                .build();
        ligacao = topicoArtigoRepo.save(ligacao);
        return toTopicoArtigo(ligacao);
    }

    @Transactional
    public void desligarArtigo(UUID topicoId, UUID artigoId) {
        topicoArtigoRepo.findByTopicoIdAndArtigoId(topicoId, artigoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ligação tópico–artigo não encontrada"));
        topicoArtigoRepo.deleteByTopicoIdAndArtigoId(topicoId, artigoId);
    }

    private EntidadeJuridicaResponse toEntidade(EntidadeJuridica e, int totalTopicos) {
        return new EntidadeJuridicaResponse(
                e.getId(),
                e.getCodigo(),
                e.getNome(),
                e.getDescricao(),
                e.getIcone(),
                e.getOrdem() != null ? e.getOrdem() : 0,
                totalTopicos
        );
    }

    private TopicoJuridicoResponse toTopico(TopicoJuridico t, boolean comRelacoes) {
        EntidadeJuridica ent = t.getEntidade();
        TopicoJuridico parent = t.getParent();
        int totalArtigos = topicoArtigoRepo.findByTopicoIdComArtigo(t.getId()).size();

        List<RelacaoJuridicaResponse> relacoes = List.of();
        if (comRelacoes) {
            List<RelacaoJuridicaResponse> out = new ArrayList<>();
            for (RelacaoJuridica r : relacaoRepo.findByOrigemId(t.getId())) {
                TopicoJuridico d = r.getDestino();
                out.add(new RelacaoJuridicaResponse(
                        r.getId(), r.getTipoRelacao(),
                        r.getPeso() != null ? r.getPeso() : 1f,
                        r.getNotas(),
                        d.getId(), d.getCodigo(), d.getNome(), true));
            }
            for (RelacaoJuridica r : relacaoRepo.findByDestinoId(t.getId())) {
                TopicoJuridico o = r.getOrigem();
                out.add(new RelacaoJuridicaResponse(
                        r.getId(), r.getTipoRelacao(),
                        r.getPeso() != null ? r.getPeso() : 1f,
                        r.getNotas(),
                        o.getId(), o.getCodigo(), o.getNome(), false));
            }
            relacoes = out;
        }

        SequenciaLocal sequencia = calcularSequencia(t);

        return new TopicoJuridicoResponse(
                t.getId(),
                t.getCodigo(),
                t.getNome(),
                t.getDescricao(),
                ent != null ? ent.getId() : null,
                ent != null ? ent.getCodigo() : null,
                ent != null ? ent.getNome() : null,
                parent != null ? parent.getId() : null,
                parent != null ? parent.getNome() : null,
                t.getOrdem() != null ? t.getOrdem() : 0,
                totalArtigos,
                relacoes,
                t.getDefinicaoEstudo(),
                parsePerguntasGuia(t.getPerguntasGuia()),
                t.getPerguntasGuiaGeradoEm(),
                t.getPorqueExiste(),
                parseListaTexto(t.getOndeApareceVida()),
                parseListaTexto(t.getErrosComuns()),
                parseCasoPratico(t.getCasoPratico()),
                sequencia.anteriorId(),
                sequencia.anteriorNome(),
                sequencia.seguinteId(),
                sequencia.seguinteNome(),
                sequencia.posicao(),
                sequencia.total()
        );
    }

    /** Posição de um tópico na trilha de estudo sugerida da sua entidade (vazia se não pertencer a nenhuma). */
    private record SequenciaLocal(
            UUID anteriorId, String anteriorNome,
            UUID seguinteId, String seguinteNome,
            int posicao, int total
    ) {
        static SequenciaLocal vazia() {
            return new SequenciaLocal(null, null, null, null, 0, 0);
        }
    }

    private SequenciaLocal calcularSequencia(TopicoJuridico t) {
        if (t.getEntidade() == null) {
            return SequenciaLocal.vazia();
        }
        List<TopicoJuridico> topicos =
                topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(t.getEntidade().getId());
        List<UUID> trilha = calcularTrilhaSugerida(t.getEntidade().getId(), topicos);
        int idx = trilha.indexOf(t.getId());
        if (idx < 0) {
            return SequenciaLocal.vazia();
        }
        Map<UUID, TopicoJuridico> porId = topicos.stream()
                .collect(Collectors.toMap(TopicoJuridico::getId, x -> x));
        UUID anteriorId = idx > 0 ? trilha.get(idx - 1) : null;
        UUID seguinteId = idx < trilha.size() - 1 ? trilha.get(idx + 1) : null;
        return new SequenciaLocal(
                anteriorId, anteriorId != null ? porId.get(anteriorId).getNome() : null,
                seguinteId, seguinteId != null ? porId.get(seguinteId).getNome() : null,
                idx + 1, trilha.size()
        );
    }

    /**
     * Calcula a trilha de estudo sugerida para os tópicos de uma entidade:
     * ordenação topológica das relações PRESSUPOE (o pré-requisito vem sempre
     * antes de quem o pressupõe), usando {@code ordem}/nome/id como
     * desempate estável. Se houver um ciclo de dependências, os tópicos
     * envolvidos são acrescentados no fim pela mesma ordem de desempate — a
     * trilha nunca falha, apenas fica parcialmente sem garantia de
     * pré-requisitos nesse trecho.
     */
    private List<UUID> calcularTrilhaSugerida(UUID entidadeId, List<TopicoJuridico> topicos) {
        if (topicos.isEmpty()) {
            return List.of();
        }
        Map<UUID, TopicoJuridico> porId = topicos.stream()
                .collect(Collectors.toMap(TopicoJuridico::getId, x -> x));
        Set<UUID> ids = porId.keySet();

        Comparator<UUID> desempate = Comparator
                .<UUID>comparingInt(id -> {
                    Integer ordem = porId.get(id).getOrdem();
                    return ordem != null ? ordem : 0;
                })
                .thenComparing(id -> {
                    String nome = porId.get(id).getNome();
                    return nome != null ? nome : "";
                })
                .thenComparing(UUID::toString);

        Map<UUID, Set<UUID>> predecessores = new HashMap<>();
        for (UUID id : ids) {
            predecessores.put(id, new HashSet<>());
        }
        for (TopicoJuridico t : topicos) {
            for (RelacaoJuridica r : relacaoRepo.findByOrigemId(t.getId())) {
                if ("PRESSUPOE".equals(r.getTipoRelacao()) && ids.contains(r.getDestino().getId())) {
                    // t PRESSUPOE destino → destino é pré-requisito de t → deve vir antes
                    predecessores.get(t.getId()).add(r.getDestino().getId());
                }
            }
        }

        Map<UUID, List<UUID>> sucessores = new HashMap<>();
        for (UUID id : ids) {
            sucessores.put(id, new ArrayList<>());
        }
        Map<UUID, Integer> grauEntrada = new HashMap<>();
        for (UUID id : ids) {
            grauEntrada.put(id, predecessores.get(id).size());
            for (UUID pred : predecessores.get(id)) {
                sucessores.get(pred).add(id);
            }
        }

        TreeSet<UUID> prontos = new TreeSet<>(desempate);
        for (UUID id : ids) {
            if (grauEntrada.get(id) == 0) {
                prontos.add(id);
            }
        }

        List<UUID> resultado = new ArrayList<>();
        while (!prontos.isEmpty()) {
            UUID atual = prontos.pollFirst();
            resultado.add(atual);
            for (UUID suc : sucessores.get(atual)) {
                int novoGrau = grauEntrada.get(suc) - 1;
                grauEntrada.put(suc, novoGrau);
                if (novoGrau == 0) {
                    prontos.add(suc);
                }
            }
        }

        if (resultado.size() < ids.size()) {
            log.warn("Ciclo de dependências (PRESSUPOE) detectado na entidade {} — trilha sugerida parcial "
                    + "para {} de {} tópicos.", entidadeId, resultado.size(), ids.size());
            List<UUID> restantes = ids.stream()
                    .filter(id -> !resultado.contains(id))
                    .sorted(desempate)
                    .toList();
            resultado.addAll(restantes);
        }
        return resultado;
    }

    /** Trilha de estudo sugerida completa de uma entidade, para navegação/sidebar. */
    @Transactional(readOnly = true)
    public List<TrilhaItemResponse> obterTrilhaSugerida(UUID entidadeId) {
        if (!entidadeRepo.existsById(entidadeId)) {
            throw new RecursoNaoEncontradoException("Entidade jurídica não encontrada");
        }
        List<TopicoJuridico> topicos = topicoRepo.findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(entidadeId);
        List<UUID> trilha = calcularTrilhaSugerida(entidadeId, topicos);
        Map<UUID, TopicoJuridico> porId = topicos.stream()
                .collect(Collectors.toMap(TopicoJuridico::getId, x -> x));

        List<TrilhaItemResponse> out = new ArrayList<>();
        for (int i = 0; i < trilha.size(); i++) {
            TopicoJuridico t = porId.get(trilha.get(i));
            out.add(new TrilhaItemResponse(t.getId(), t.getNome(), i + 1));
        }
        return out;
    }

    private List<String> parseListaTexto(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            List<String> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    String texto = item.asText("").trim();
                    if (!texto.isEmpty()) {
                        out.add(texto);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("Falha ao interpretar lista em cache do tópico: {}", e.getMessage());
            return List.of();
        }
    }

    private CasoPraticoResponse parseCasoPratico(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode raiz = objectMapper.readTree(json);
            String enunciado = raiz.path("enunciado").asText("").trim();
            String explicacao = raiz.path("explicacao").asText("").trim();
            List<String> perguntas = new ArrayList<>();
            for (JsonNode item : raiz.path("perguntas")) {
                String texto = item.asText("").trim();
                if (!texto.isEmpty()) {
                    perguntas.add(texto);
                }
            }
            if (enunciado.isEmpty()) {
                return null;
            }
            return new CasoPraticoResponse(enunciado, perguntas, explicacao);
        } catch (Exception e) {
            log.warn("Falha ao interpretar caso prático em cache do tópico: {}", e.getMessage());
            return null;
        }
    }

    private List<PerguntaGuiaResponse> parsePerguntasGuia(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            List<PerguntaGuiaResponse> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    String pergunta = item.path("pergunta").asText("").trim();
                    String resposta = item.path("resposta").asText("").trim();
                    if (!pergunta.isEmpty() && !resposta.isEmpty()) {
                        out.add(new PerguntaGuiaResponse(pergunta, resposta));
                    }
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("Falha ao interpretar perguntas-guia em cache do tópico: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Gera (ou regenera, se {@code forcar}) a Ficha de Estudo de um tópico.
     * A geração por IA está em {@link OntologiaFichaService}.
     */
    @Transactional
    public TopicoJuridicoResponse gerarFichaEstudo(UUID topicoId, boolean forcar) {
        TopicoJuridico topico = ontologiaFichaService.gerarOuObter(topicoId, forcar);
        return toTopico(topico, true);
    }

    private TopicoArtigoResponse toTopicoArtigo(TopicoArtigo ta) {
        Artigo a = ta.getArtigo();
        Diploma d = a.getDiploma();
        return new TopicoArtigoResponse(
                ta.getId(),
                a.getId(),
                a.getNumero(),
                a.getTitulo(),
                d != null ? d.getId() : null,
                d != null ? d.getNumero() : null,
                d != null ? d.getTitulo() : null,
                ta.getRelevancia() != null ? ta.getRelevancia() : 1f,
                ta.getOrigemLigacao()
        );
    }

    private static float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
