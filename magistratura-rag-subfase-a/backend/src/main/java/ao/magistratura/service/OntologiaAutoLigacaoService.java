package ao.magistratura.service;

import ao.magistratura.dto.ontologia.SugestaoLigacaoResponse;
import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.TopicoArtigo;
import ao.magistratura.entity.TopicoJuridico;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.TopicoArtigoRepository;
import ao.magistratura.repository.TopicoJuridicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sugere e persiste ligações tópico↔artigo (lexical + sinónimos jurídicos).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OntologiaAutoLigacaoService {

    private static final Pattern NAO_ALNUM = Pattern.compile("[^a-z0-9\\s]+");
    private static final Set<String> STOP = Set.of(
            "de", "da", "do", "das", "dos", "e", "ou", "a", "o", "as", "os",
            "em", "no", "na", "nos", "nas", "por", "para", "com", "sem", "ao", "à",
            "um", "uma", "uns", "umas", "que", "se", "como", "sobre", "entre",
            "artigo", "artigos", "lei", "codigo", "código", "n", "nr", "nº",
            "republica", "angola", "geral", "conforme", "termos"
    );

    /** Sinónimos / termos-chave por código de tópico (melhoram match em PDFs reais). */
    private static final Map<String, List<String>> SINONIMOS = Map.ofEntries(
            Map.entry("TRABALHO.EMPREGADOR", List.of("empregador", "patronato", "entidade empregadora", "empresa")),
            Map.entry("TRABALHO.TRABALHADOR", List.of("trabalhador", "trabalhadora", "assalariado", "colaborador")),
            Map.entry("TRABALHO.CONTRATO_TRABALHO", List.of("contrato de trabalho", "vinculo laboral", "relação de trabalho", "laboral")),
            Map.entry("PROCESSO_PENAL.PRISAO_PREVENTIVA", List.of("prisão preventiva", "prision preventiva", "medida de coacção", "coacao")),
            Map.entry("PROCESSO_PENAL.PRESUNCAO_INOCENCIA", List.of("presunção de inocência", "presuncao de inocencia", "inocente até")),
            Map.entry("PESSOA.DIREITOS_FUNDAMENTAIS", List.of("direitos fundamentais", "liberdades", "garantias", "direitos humanos")),
            Map.entry("PESSOA.NACIONALIDADE", List.of("nacionalidade", "cidadão angolano", "cidadao")),
            Map.entry("PESSOA.PERSONALIDADE", List.of("personalidade jurídica", "personalidade juridica", "capacidade de gozo")),
            Map.entry("PESSOA.CAPACIDADE", List.of("capacidade jurídica", "incapaz", "maioridade")),
            Map.entry("ESTADO.PODER_JUDICIAL", List.of("poder judicial", "tribunais", "magistratura", "juízes", "juizes")),
            Map.entry("ESTADO.PODERES", List.of("poder legislativo", "poder executivo", "separação de poderes", "orgaos de soberania")),
            Map.entry("ESTADO.RESPONSABILIDADE", List.of("responsabilidade do estado", "responsabilidade civil do estado", "administração pública")),
            Map.entry("CONTRATO.COMPRA_VENDA", List.of("compra e venda", "comprador", "vendedor", "preço")),
            Map.entry("CONTRATO.ARRENDAMENTO", List.of("arrendamento", "senhorio", "inquilino", "renda")),
            Map.entry("CONTRATO.FORMACAO", List.of("proposta", "aceitação", "aceitacao", "formação do contrato")),
            Map.entry("RESPONSABILIDADE.PRESSUPOSTOS", List.of("nexo causal", "culpa", "ilicitude", "dano", "responsabilidade civil")),
            Map.entry("RESPONSABILIDADE.INDEMNIZACAO", List.of("indemnização", "indemnizacao", "reparação", "reparacao")),
            Map.entry("PROCESSO_CIVIL.COMPETENCIA", List.of("competência", "competencia", "juízo competente", "foro")),
            Map.entry("PROCESSO_CIVIL.RECURSOS", List.of("apelação", "apelacao", "revista", "recurso", "agravo")),
            Map.entry("TRIBUNAL.ORGANIZACAO", List.of("tribunal supremo", "organização judiciária", "organizacao judiciaria", "comarca")),
            Map.entry("FAMILIA.CASAMENTO", List.of("casamento", "cônjuges", "conjuges", "matrimónio", "matrimonio")),
            Map.entry("FAMILIA.DIVORCIO", List.of("divórcio", "divorcio", "dissolução do casamento")),
            Map.entry("FAMILIA.FILIACAO", List.of("filiação", "filiacao", "poder paternal", "filhos"))
    );

    /** Se o diploma mencionar estes termos, favorece tópicos da entidade. */
    private static final Map<String, String> DIPLOMA_PARA_ENTIDADE = Map.ofEntries(
            Map.entry("trabalho", "TRABALHO"),
            Map.entry("laboral", "TRABALHO"),
            Map.entry("constitui", "ESTADO"),
            Map.entry("constitucional", "ESTADO"),
            Map.entry("processo civil", "PROCESSO_CIVIL"),
            Map.entry("processual civil", "PROCESSO_CIVIL"),
            Map.entry("processo penal", "PROCESSO_PENAL"),
            Map.entry("processual penal", "PROCESSO_PENAL"),
            Map.entry("código civil", "CONTRATO"),
            Map.entry("codigo civil", "CONTRATO"),
            Map.entry("código penal", "CRIME"),
            Map.entry("codigo penal", "CRIME"),
            Map.entry("família", "FAMILIA"),
            Map.entry("familia", "FAMILIA")
    );

    private final ArtigoRepository artigoRepository;
    private final TopicoJuridicoRepository topicoRepository;
    private final TopicoArtigoRepository topicoArtigoRepository;
    private final DiplomaRepository diplomaRepository;

    @Value("${app.ontologia.auto-ligar:true}")
    private boolean autoLigarEnabled;

    @Value("${app.ontologia.min-score:0.28}")
    private float minScore;

    @Value("${app.ontologia.max-ligacoes-por-artigo:5}")
    private int maxPorArtigo;

    public boolean isEnabled() {
        return autoLigarEnabled;
    }

    @Transactional
    public int ligarAposDocumento(UUID documentoId) {
        if (!autoLigarEnabled || documentoId == null) {
            return 0;
        }
        try {
            List<Artigo> artigos = artigoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId);
            List<SugestaoLigacaoResponse> criadas = aplicar(artigos, false).stream()
                    .filter(s -> "CRIADA".equals(s.estado()))
                    .toList();
            log.info("Ontologia auto-ligação documento={}: {} criada(s) de {} artigo(s)",
                    documentoId, criadas.size(), artigos.size());
            return criadas.size();
        } catch (Exception e) {
            log.warn("Ontologia auto-ligação falhou (documento={}): {}", documentoId, e.getMessage());
            return 0;
        }
    }

    @Transactional
    public List<SugestaoLigacaoResponse> sugerirPorDocumento(UUID documentoId, boolean dryRun) {
        List<Artigo> artigos = artigoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId);
        if (artigos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum artigo neste documento");
        }
        return aplicar(artigos, dryRun);
    }

    @Transactional
    public List<SugestaoLigacaoResponse> sugerirPorDiploma(UUID diplomaId, boolean dryRun) {
        List<Artigo> artigos = artigoRepository.findByDiplomaIdOrderByOrdemAsc(diplomaId);
        if (artigos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum artigo neste diploma");
        }
        return aplicar(artigos, dryRun);
    }

    /**
     * Percorre todos os diplomas com artigos e cria ligações em falta.
     * Útil para biblioteca já processada antes da ontologia.
     */
    @Transactional
    public List<SugestaoLigacaoResponse> ligarTodaBiblioteca(boolean dryRun) {
        List<Diploma> diplomas = diplomaRepository.findAll();
        List<SugestaoLigacaoResponse> all = new ArrayList<>();
        for (Diploma d : diplomas) {
            List<Artigo> arts = artigoRepository.findByDiplomaIdOrderByOrdemAsc(d.getId());
            if (arts.isEmpty()) {
                continue;
            }
            all.addAll(aplicar(arts, dryRun));
        }
        log.info("Ontologia ligarTodaBiblioteca dryRun={} → {} resultados", dryRun, all.size());
        return all;
    }

    private List<SugestaoLigacaoResponse> aplicar(List<Artigo> artigos, boolean dryRun) {
        List<TopicoJuridico> topicos = topicoRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .toList();
        if (topicos.isEmpty() || artigos.isEmpty()) {
            return List.of();
        }

        List<TopicIndex> indices = topicos.stream().map(TopicIndex::of).toList();
        String diplomaBoostEntidade = detectarEntidadeDiploma(artigos.get(0));

        List<SugestaoLigacaoResponse> resultado = new ArrayList<>();

        for (Artigo artigo : artigos) {
            String tituloN = normalizar(safe(artigo.getTitulo()));
            String textoN = normalizar(safe(artigo.getTexto()));
            String metaN = normalizar(safe(artigo.getCapitulo()) + " " + safe(artigo.getSeccao())
                    + " " + safe(artigo.getNumero()));
            if (textoN.length() > 6000) {
                textoN = textoN.substring(0, 6000);
            }
            String blob = tituloN + " " + metaN + " " + textoN;

            List<Scored> scores = new ArrayList<>();
            for (TopicIndex ti : indices) {
                float s = pontuar(ti, tituloN, textoN, metaN, blob);
                if (diplomaBoostEntidade != null
                        && ti.topico().getCodigo() != null
                        && ti.topico().getCodigo().startsWith(diplomaBoostEntidade + ".")) {
                    s += 0.12f;
                }
                if (s >= minScore) {
                    scores.add(new Scored(ti.topico(), Math.min(1f, s)));
                }
            }
            scores.sort(Comparator.comparingDouble(Scored::score).reversed());
            if (scores.size() > maxPorArtigo) {
                scores = scores.subList(0, maxPorArtigo);
            }

            for (Scored sc : scores) {
                boolean jaExiste = topicoArtigoRepository
                        .findByTopicoIdAndArtigoId(sc.topico().getId(), artigo.getId())
                        .isPresent();
                if (jaExiste) {
                    resultado.add(toResponse(sc, artigo, true, dryRun));
                    continue;
                }
                if (!dryRun) {
                    TopicoArtigo lig = TopicoArtigo.builder()
                            .topico(sc.topico())
                            .artigo(artigo)
                            .relevancia(clamp(sc.score()))
                            .origemLigacao("AUTO")
                            .build();
                    topicoArtigoRepository.save(lig);
                }
                resultado.add(toResponse(sc, artigo, false, dryRun));
            }
        }
        return resultado;
    }

    private String detectarEntidadeDiploma(Artigo amostra) {
        if (amostra.getDiploma() == null) {
            return null;
        }
        String t = normalizar(safe(amostra.getDiploma().getTitulo()) + " " + safe(amostra.getDiploma().getNumero()));
        for (Map.Entry<String, String> e : DIPLOMA_PARA_ENTIDADE.entrySet()) {
            if (t.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static SugestaoLigacaoResponse toResponse(Scored sc, Artigo a, boolean jaExistia, boolean dryRun) {
        return new SugestaoLigacaoResponse(
                a.getId(),
                a.getNumero(),
                a.getTitulo(),
                sc.topico().getId(),
                sc.topico().getCodigo(),
                sc.topico().getNome(),
                sc.score(),
                jaExistia,
                dryRun ? "SUGESTAO" : (jaExistia ? "JA_EXISTIA" : "CRIADA")
        );
    }

    private float pontuar(TopicIndex ti, String titulo, String texto, String meta, String blob) {
        if (ti.tokens().isEmpty() && ti.codigoTails().isEmpty() && ti.sinonimos().isEmpty()) {
            return 0f;
        }
        float score = 0f;
        int hitsTitulo = 0;
        int hitsTexto = 0;
        for (String tok : ti.tokens()) {
            if (titulo.contains(tok)) {
                hitsTitulo++;
                score += 0.32f;
            } else if (meta.contains(tok)) {
                score += 0.14f;
            }
            if (texto.contains(tok)) {
                hitsTexto++;
                score += 0.09f;
            }
        }
        for (String c : ti.codigoTails()) {
            if (c.length() < 4) continue;
            if (titulo.contains(c) || meta.contains(c)) {
                score += 0.38f;
            } else if (texto.contains(c)) {
                score += 0.14f;
            }
        }
        // Sinónimos: frases longas no blob
        for (String syn : ti.sinonimos()) {
            if (syn.length() < 4) continue;
            if (titulo.contains(syn)) {
                score += 0.45f;
            } else if (blob.contains(syn)) {
                score += 0.22f;
            }
        }
        if (hitsTitulo >= 2) score += 0.12f;
        if (hitsTexto >= 3) score += 0.08f;
        return Math.min(1f, score);
    }

    private static String normalizar(String s) {
        if (s == null || s.isBlank()) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT);
        n = NAO_ALNUM.matcher(n).replaceAll(" ");
        return n.replaceAll("\\s+", " ").trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private record Scored(TopicoJuridico topico, float score) {}

    private record TopicIndex(
            TopicoJuridico topico,
            Set<String> tokens,
            List<String> codigoTails,
            List<String> sinonimos
    ) {
        static TopicIndex of(TopicoJuridico t) {
            Set<String> tokens = new HashSet<>();
            tokens.addAll(tokensDe(t.getNome()));
            tokens.addAll(tokensDe(t.getDescricao()));
            List<String> tails = new ArrayList<>();
            if (t.getCodigo() != null) {
                for (String p : t.getCodigo().split("\\.")) {
                    String n = normalizar(p.replace('_', ' '));
                    if (!n.isBlank()) {
                        tails.add(n);
                        tokens.addAll(Arrays.asList(n.split("\\s+")));
                    }
                }
            }
            tokens.removeIf(x -> x.length() < 4 || STOP.contains(x));
            List<String> syn = SINONIMOS.getOrDefault(t.getCodigo(), List.of()).stream()
                    .map(OntologiaAutoLigacaoService::normalizar)
                    .filter(s -> !s.isBlank())
                    .toList();
            return new TopicIndex(t, tokens, tails, syn);
        }

        private static Set<String> tokensDe(String raw) {
            if (raw == null || raw.isBlank()) return Set.of();
            return Arrays.stream(normalizar(raw).split("\\s+"))
                    .filter(x -> x.length() >= 4 && !STOP.contains(x))
                    .collect(Collectors.toSet());
        }
    }
}
