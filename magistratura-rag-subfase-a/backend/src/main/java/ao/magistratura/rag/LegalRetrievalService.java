package ao.magistratura.rag;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DiplomaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recuperação jurídica conservadora (Subfase A).
 * <p>
 * Não gera embeddings. Prioridade:
 * <ol>
 *   <li>artigoId / diplomaId explícitos (chamador)</li>
 *   <li>número de artigo + nome de diploma na pergunta</li>
 *   <li>número de artigo isolado</li>
 *   <li>pesquisa textual (LIKE)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class LegalRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(LegalRetrievalService.class);
    private static final int MAX_ARTIGOS = 5;

    /** "artigo 1", "Art. 12", "art. 1.º", "Art 23º" */
    private static final Pattern ARTIGO_NUM =
            Pattern.compile("(?i)\\barts?\\.?\\s*(\\d{1,4})(?:\\.º|º|ª)?\\b"
                    + "|\\bartigo\\s+(\\d{1,4})(?:\\.º|º|ª)?\\b");

    /** Linhas típicas de índice/sumário: "Artigo 1.º .......... 12". */
    private static final Pattern LINHA_INDICE =
            Pattern.compile("(?is)^\\s*art(?:igo|\\.)?\\s*\\d{1,4}.*?\\.{3,}\\s*\\d{1,4}\\s*$");

    private final ArtigoRepository artigoRepository;
    private final DiplomaRepository diplomaRepository;

    /**
     * Resolve contexto a partir de IDs explícitos e/ou do texto da pergunta.
     *
     * @param pergunta   mensagem do estudante
     * @param diplomaId  opcional — se presente, restringe / prioriza
     * @param artigoId   opcional — se presente, esse artigo é a fonte principal
     */
    @Transactional(readOnly = true)
    public RetrievalResult recuperar(String pergunta, UUID diplomaId, UUID artigoId) {
        // Sem IDs e mensagem social/meta → não forçar pesquisa jurídica (Teste 4)
        if (artigoId == null && diplomaId == null && isMensagemNaoJuridica(pergunta)) {
            log.debug("Retrieval ignorado (mensagem não jurídica): {}", truncar(pergunta));
return RetrievalResult.semContexto();
        }

        // 1) ID de artigo explícito
        if (artigoId != null) {
            Optional<Artigo> opt = artigoRepository.findById(artigoId);
            if (opt.isPresent()) {
                Artigo a = opt.get();
                Diploma d = a.getDiploma();
                if (diplomaId != null && d != null && !diplomaId.equals(d.getId())) {
                    log.debug("artigoId {} não pertence ao diplomaId {}; usa-se o diploma do artigo", artigoId, diplomaId);
                }
                // Se o registo for entrada de índice e existir gémeo substantivo, preferir o corpo
                Artigo preferido = preferirArtigoSubstantivo(a);
                Diploma dPref = preferido.getDiploma() != null ? preferido.getDiploma() : d;
                return RetrievalResult.de(
                        dPref,
                        List.of(preferido),
                        List.of(RetrievedPassage.fromArtigo(preferido, "ID_EXPLICITO", 1.0)),
                        "ID_ARTIGO"
                );
            }
        }

        Diploma diplomaFiltro = null;
        if (diplomaId != null) {
            diplomaFiltro = diplomaRepository.findById(diplomaId).orElse(null);
        }

        String q = pergunta != null ? pergunta.trim() : "";
        String numero = extrairNumeroArtigo(q);
        String termoDiploma = diplomaFiltro == null ? extrairTermoDiploma(q) : null;

        // 2) Número + diploma (explícito ou detectado)
        if (numero != null) {
            List<Artigo> encontrados;
            if (diplomaFiltro != null) {
                encontrados = artigoRepository.buscarPorNumeroEDiplomaId(numero, diplomaFiltro.getId());
            } else if (termoDiploma != null) {
                encontrados = artigoRepository.buscarPorNumeroEDiplomaTermo(numero, termoDiploma);
            } else {
                encontrados = artigoRepository.buscarPorNumeroFlexivel(numero);
            }
            encontrados = limitar(encontrados);
            if (!encontrados.isEmpty()) {
                Diploma d = diplomaFiltro != null ? diplomaFiltro : primeiroDiploma(encontrados);
                List<RetrievedPassage> passagens = new ArrayList<>();
                for (Artigo a : encontrados) {
                    passagens.add(RetrievedPassage.fromArtigo(a,
                            termoDiploma != null || diplomaFiltro != null ? "NUMERO_DIPLOMA" : "NUMERO",
                            diplomaFiltro != null || termoDiploma != null ? 0.95 : 0.75));
                }
                return RetrievalResult.de(d, encontrados, passagens,
                        diplomaFiltro != null || termoDiploma != null ? "NUMERO_DIPLOMA" : "NUMERO");
            }
        }

        // 3) Só diploma explícito — sem número: não devolve todos os artigos (demasiado grande)
        //    Cai em pesquisa textual limitada a esse diploma se houver pergunta.
        if (diplomaFiltro != null && !q.isBlank()) {
            List<Artigo> textual = artigoRepository.pesquisarNoDiploma(diplomaFiltro.getId(), q, PageRequest.of(0, MAX_ARTIGOS)).getContent();
            textual = limitar(textual);
            if (!textual.isEmpty()) {
                List<RetrievedPassage> passagens = textual.stream()
                        .map(a -> RetrievedPassage.fromArtigo(a, "TEXTO", 0.55))
                        .toList();
                return RetrievalResult.de(diplomaFiltro, textual, passagens, "TEXTO_NO_DIPLOMA");
            }
            return RetrievalResult.de(diplomaFiltro, List.of(), List.of(), "DIPLOMA_SEM_ARTIGOS");
        }

        // 4) Pesquisa textual global
        if (!q.isBlank()) {
            String termo = prepararTermoPesquisa(q);
            if (termo.length() >= 3) {
                List<Artigo> textual = artigoRepository.pesquisar(termo, PageRequest.of(0, MAX_ARTIGOS * 2)).getContent();
                textual = limitar(textual);
                if (!textual.isEmpty()) {
                    List<RetrievedPassage> passagens = textual.stream()
                            .map(a -> RetrievedPassage.fromArtigo(a, "TEXTO", 0.5))
                            .toList();
                    return RetrievalResult.de(primeiroDiploma(textual), textual, passagens, "TEXTO");
                }
            }
        }

        // Fallback: diploma seleccionado sem match textual → artigos representativos
        if (diplomaFiltro != null) {
            List<Artigo> doDiploma = artigoRepository
                    .findByDiplomaIdOrderByOrdemAsc(diplomaFiltro.getId())
                    .stream()
                    .filter(a -> a.getTexto() != null && !a.getTexto().isBlank() && !isConteudoIndice(a))
                    .limit(MAX_ARTIGOS)
                    .toList();
            if (!doDiploma.isEmpty()) {
                List<RetrievedPassage> passagens = doDiploma.stream()
                        .map(a -> RetrievedPassage.fromArtigo(a, "DIPLOMA_FALLBACK", 0.7))
                        .toList();
                log.info("Retrieval fallback diploma {} → {} artigos da BD", diplomaFiltro.getId(), doDiploma.size());
                return RetrievalResult.de(diplomaFiltro, doDiploma, passagens, "DIPLOMA_FALLBACK");
            }
        }

        log.info("Retrieval vazio para pergunta='{}' diplomaId={} artigoId={}", truncar(q), diplomaId, artigoId);
        return RetrievalResult.semContexto();
    }


    /**
     * Se o artigo pedido por ID for claramente índice e existir outro com o mesmo
     * número no mesmo diploma com texto substantivo, devolve o substantivo.
     */
    private Artigo preferirArtigoSubstantivo(Artigo a) {
        if (a == null || !isConteudoIndice(a)) {
            return a;
        }
        if (a.getNumero() == null || a.getDiploma() == null || a.getDiploma().getId() == null) {
            return a;
        }
        List<Artigo> irmaos = artigoRepository.buscarPorNumeroEDiplomaId(a.getNumero(), a.getDiploma().getId());
        Artigo melhor = a;
        int melhorScore = scoreRelevancia(a);
        for (Artigo c : irmaos) {
            if (c == null || c.getId() == null) {
                continue;
            }
            int s = scoreRelevancia(c);
            if (s > melhorScore) {
                melhor = c;
                melhorScore = s;
            }
        }
        if (melhor != a) {
            log.info("Retrieval: artigo ID {} parece índice; preferido gémeo substantivo {}",
                    a.getId(), melhor.getId());
        }
        return melhor;
    }

    /**
     * Saudações e perguntas meta sobre a IA — não disparam pesquisa na biblioteca.
     */
    static boolean isMensagemNaoJuridica(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            return true;
        }
        String t = pergunta.trim().toLowerCase(Locale.ROOT);
        if (t.length() <= 2) {
            return true;
        }
        if (extrairNumeroArtigo(pergunta) != null || extrairTermoDiploma(pergunta) != null) {
            return false;
        }
        if (t.contains("artigo") || t.contains("constitui") || t.contains("diploma")
                || t.contains("lei ") || t.contains("código") || t.contains("codigo")
                || t.contains("soberania") || t.contains("magistratura")) {
            return false;
        }
        if (t.startsWith("olá") || t.startsWith("ola") || t.startsWith("oi ")
                || t.equals("oi") || t.startsWith("hey")
                || t.startsWith("bom dia") || t.startsWith("boa tarde") || t.startsWith("boa noite")) {
            return true;
        }
        if (t.contains("quem és") || t.contains("quem é você") || t.contains("quem e voce")
                || t.contains("quem é o tutor") || t.contains("quem eres")) {
            return true;
        }
        return t.matches("^(obrigad[oa]|ok|sim|não|nao)([!?.\\s].*)?$")
                || t.matches("^como (estás|estas|funciona)([!?.\\s].*)?$");
    }

    static String extrairNumeroArtigo(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            return null;
        }
        Matcher m = ARTIGO_NUM.matcher(pergunta);
        if (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            return g1 != null ? g1 : g2;
        }
        return null;
    }

    /**
     * Detecta diplomas comuns por palavras-chave na pergunta.
     */
    static String extrairTermoDiploma(String pergunta) {
        if (pergunta == null) {
            return null;
        }
        String p = pergunta.toLowerCase(Locale.ROOT);
        if (p.contains("constitui")) {
            return "constitui";
        }
        if (p.contains("código penal") || p.contains("codigo penal")) {
            return "penal";
        }
        if (p.contains("processo penal")) {
            return "processo penal";
        }
        if (p.contains("código civil") || p.contains("codigo civil")) {
            return "civil";
        }
        if (p.contains("trabalho")) {
            return "trabalho";
        }
        return null;
    }

    private static String prepararTermoPesquisa(String pergunta) {
        // Remove ruído comum de perguntas pedagógicas
        return pergunta
                .replaceAll("(?i)\\b(explique|explica|o que (é|e)|qual (é|e)|fale sobre|defina)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Ordena candidatos por relevância normativa e limita a {@link #MAX_ARTIGOS}.
     * Em caso de conflito de número (ex. Art. 1 «Alterações» vs «República de Angola»),
     * penaliza títulos/textos de revisão/meta e favorece texto substantivo.
     */
    private static List<Artigo> limitar(List<Artigo> lista) {
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        Map<UUID, Artigo> map = new LinkedHashMap<>();
        for (Artigo a : lista) {
            if (a != null && a.getId() != null) {
                map.putIfAbsent(a.getId(), a);
            }
        }
        List<Artigo> candidatos = new ArrayList<>(map.values());

        // Por (diploma, número): se índice e corpo coexistirem, fica o de maior score (corpo)
        Map<String, Artigo> melhorPorChave = new LinkedHashMap<>();
        for (Artigo a : candidatos) {
            String chave = chaveNumeroDiploma(a);
            Artigo atual = melhorPorChave.get(chave);
            if (atual == null || scoreRelevancia(a) > scoreRelevancia(atual)) {
                melhorPorChave.put(chave, a);
            }
        }
        List<Artigo> ordenados = new ArrayList<>(melhorPorChave.values());
        ordenados.sort(Comparator
                .comparingInt((Artigo a) -> scoreRelevancia(a)).reversed()
                .thenComparing(a -> a.getOrdem() != null ? a.getOrdem() : Integer.MAX_VALUE)
                .thenComparing(a -> a.getId() != null ? a.getId().toString() : ""));
        if (ordenados.size() > MAX_ARTIGOS) {
            return new ArrayList<>(ordenados.subList(0, MAX_ARTIGOS));
        }
        return ordenados;
    }

    private static String chaveNumeroDiploma(Artigo a) {
        String num = a.getNumero() != null ? a.getNumero().trim() : "";
        String dip = a.getDiploma() != null && a.getDiploma().getId() != null
                ? a.getDiploma().getId().toString()
                : "sem-diploma";
        return dip + "|" + num;
    }

    /**
     * Pontuação de relevância (maior = preferido). Pacote-visível para testes unitários.
     */
    static int scoreRelevancia(Artigo a) {
        if (a == null) {
            return Integer.MIN_VALUE;
        }
        int score = 100;
        String titulo = a.getTitulo() != null ? a.getTitulo().toLowerCase(Locale.ROOT) : "";
        String texto = a.getTexto() != null ? a.getTexto().toLowerCase(Locale.ROOT) : "";
        String capitulo = a.getCapitulo() != null ? a.getCapitulo().toLowerCase(Locale.ROOT) : "";

        if (isConteudoIndice(a)) {
            // Forte penalização: a "primeira" ocorrência no PDF costuma ser o índice
            score -= 120;
        }
        if (isMetaLegislativo(titulo) || isMetaLegislativo(texto) || isMetaLegislativo(capitulo)) {
            score -= 80;
        }
        if (texto.startsWith("são alterados") || texto.contains("são alterados os artigos")) {
            score -= 40;
        }
        if (titulo.contains("república") || titulo.contains("republica")) {
            score += 25;
        }
        if (texto.length() >= 120) {
            score += 20;
        } else if (texto.length() >= 50) {
            score += 10;
        } else if (texto.isBlank()) {
            score -= 50;
        }
        if (capitulo.contains("provedor")) {
            score -= 15;
        }
        return score;
    }

    /**
     * Heurística: entrada de índice/sumário vs corpo do artigo.
     * O PDF costuma listar "Artigo N … página" no índice antes do texto normativo —
     * essa primeira "chamada" deve ser ignorada em favor do artigo real.
     */
    static boolean isConteudoIndice(Artigo a) {
        if (a == null) {
            return false;
        }
        String titulo = a.getTitulo() != null ? a.getTitulo() : "";
        String texto = a.getTexto() != null ? a.getTexto() : "";
        String capitulo = a.getCapitulo() != null ? a.getCapitulo() : "";
        String seccao = a.getSeccao() != null ? a.getSeccao() : "";
        String blob = (titulo + "\n" + texto + "\n" + capitulo + "\n" + seccao).toLowerCase(Locale.ROOT);

        if (blob.contains("índice") || blob.contains("indice")
                || blob.contains("sumário") || blob.contains("sumario")
                || blob.contains("tabela de conteúdos") || blob.contains("table of contents")) {
            return true;
        }
        String capL = capitulo.toLowerCase(Locale.ROOT);
        if (capL.contains("índice") || capL.contains("indice")
                || capL.contains("sumário") || capL.contains("sumario")) {
            return true;
        }

        String textoTrim = texto.trim();
        if (textoTrim.isEmpty()) {
            return false;
        }
        if (LINHA_INDICE.matcher(textoTrim).matches()) {
            return true;
        }
        long dots = textoTrim.chars().filter(ch -> ch == '.').count();
        if (textoTrim.length() > 10 && dots * 2 >= textoTrim.length()) {
            return true;
        }
        if (textoTrim.length() < 100
                && textoTrim.matches("(?is).*\\d{1,4}\\s*$")
                && !textoTrim.matches("(?is).*(republica|república|direito|dever|principio|princípio|soberania|liberdade).*")
                && (dots >= 3 || textoTrim.contains("\t"))) {
            return true;
        }
        return false;
    }

    static boolean isMetaLegislativo(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String t = s.toLowerCase(Locale.ROOT);
        return t.contains("alterações") || t.contains("alteracoes")
                || t.contains("disposições transitórias") || t.contains("disposicoes transitorias")
                || t.contains("revisão") || t.contains("revisao")
                || t.contains("lei de revisão") || t.contains("lei de revisao");
    }

    private static Diploma primeiroDiploma(List<Artigo> artigos) {
        for (Artigo a : artigos) {
            if (a.getDiploma() != null) {
                return a.getDiploma();
            }
        }
        return null;
    }

    private static String truncar(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }
}
