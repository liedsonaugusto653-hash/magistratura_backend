package ao.magistratura.ia;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.Resumo;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.StudyContextPolicy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Constrói os prompts enviados ao {@link AIProvider}, combinando os
 * templates fixos em {@code resources/prompts/*.txt} com o contexto
 * jurídico real (diploma, artigo, resumo) disponível no momento do pedido.
 * <p>
 * Isto garante que a IA nunca responde "às cegas": qualquer explicação,
 * resumo, flashcard ou questão gerada é sempre ancorada em conteúdo real
 * da biblioteca jurídica, e não apenas no conhecimento genérico do modelo.
 */
@Component
public class PromptBuilder {

    private static final int MAX_CHARS_ARTIGO = 3500;

    private static final String SEM_CONTEXTO =
            "Nenhum extrato legislativo da biblioteca foi associado a este pedido. "
          + "Podes apresentar-te como Tutor da plataforma e conversar normalmente. "
          + "Se a pergunta for jurídica, não inventes artigos nem diplomas: indica que "
          + "não há fundamento legal recuperado da biblioteca e sugere indicar o diploma/"
          + "artigo ou reformular a pergunta.";

    private final Map<String, String> cacheTemplates = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATA_PT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-PT"));

    private static final ZoneId ZONA_APP = ZoneId.of("Africa/Luanda");

    /** Data legível para o system prompt (sempre a data real do servidor). */
    public static String dataAtualFormatada() {
        return LocalDate.now(ZONA_APP).format(DATA_PT);
    }

    public String promptSistemaTutor(ContextoJuridico contexto) {
        return template("tutor")
                .replace("{{DATA_ATUAL}}", dataAtualFormatada())
                .replace("{{contexto_juridico}}", formatarContexto(contexto));
    }

    public String promptResumo(ContextoJuridico contexto) {
        return template("resumo").replace("{{contexto_juridico}}", formatarContexto(contexto));
    }

    public String promptFlashcards(ContextoJuridico contexto, int quantidade) {
        return template("flashcard")
                .replace("{{quantidade}}", String.valueOf(quantidade))
                .replace("{{contexto_juridico}}", formatarContexto(contexto));
    }

    public String promptQuestoes(ContextoJuridico contexto, int quantidade) {
        return template("questao")
                .replace("{{quantidade}}", String.valueOf(quantidade))
                .replace("{{contexto_juridico}}", formatarContexto(contexto));
    }

    /**
     * Ficha de Estudo de um conceito da ontologia (Mapa Jurídico): definição
     * curta + perguntas-guia de raciocínio jurídico, ancoradas nos artigos
     * ligados ao tópico.
     */
    public String promptFichaEstudo(ContextoJuridico contexto, String nomeTopico, String descricaoTopico) {
        return template("ficha_estudo")
                .replace("{{nome_topico}}", nomeTopico != null ? nomeTopico : "")
                .replace("{{descricao_topico}}", descricaoTopico != null && !descricaoTopico.isBlank()
                        ? descricaoTopico : "")
                .replace("{{contexto_juridico}}", formatarContexto(contexto));
    }

    private String formatarContexto(ContextoJuridico contexto) {
        if (contexto == null || contexto.isVazio()) {
            return SEM_CONTEXTO;
        }

        StringBuilder sb = new StringBuilder("Contexto jurídico (usa apenas isto como fonte de verdade):\n");

        if (contexto.diploma() != null) {
            Diploma d = contexto.diploma();
            sb.append("\nDiploma: ").append(d.getTitulo())
              .append(" (nº ").append(d.getNumero()).append(")");
            if (d.getResumo() != null && !d.getResumo().isBlank()) {
                sb.append("\nResumo do diploma: ").append(d.getResumo());
            }
        }

        if (contexto.artigo() != null) {
            Artigo a = contexto.artigo();
            sb.append("\n\nArtigo ").append(a.getNumero());
            if (a.getTitulo() != null && !a.getTitulo().isBlank()) {
                sb.append(" — ").append(a.getTitulo());
            }
            sb.append(":\n").append(truncarTexto(a.getTexto()));
            if (a.getResumo() != null && !a.getResumo().isBlank()) {
                sb.append("\nResumo do artigo: ").append(a.getResumo());
            }
        }

        if (contexto.resumo() != null) {
            Resumo r = contexto.resumo();
            sb.append("\n\nResumo \"").append(r.getTitulo()).append("\":\n").append(r.getConteudo());
        }

        if (contexto.trechoLivre() != null && !contexto.trechoLivre().isBlank()) {
            sb.append("\n\nTrecho selecionado pelo estudante:\n").append(contexto.trechoLivre());
        }

        // Preferir passagens da Knowledge Layer (sem re-fetch JPA), numeradas para citação [n]
        if (contexto.passagensRecuperadas() != null && !contexto.passagensRecuperadas().isEmpty()) {
            sb.append("\n\n--- Fontes recuperadas da biblioteca (cita com [n] no texto) ---");
            var passagensLimitadas = StudyContextPolicy.limitar(
                    contexto.passagensRecuperadas(), StudyContextPolicy.CHAT_MAX_PASSAGENS_NO_PROMPT);
            int n = 1;
            for (KnowledgePassage p : passagensLimitadas) {
                sb.append("\n\n[").append(n).append("] Artigo ")
                  .append(p.artigoNumero() != null ? p.artigoNumero() : "?");
                if (p.artigoTitulo() != null && !p.artigoTitulo().isBlank()) {
                    sb.append(" — ").append(p.artigoTitulo());
                }
                if (p.diplomaTitulo() != null) {
                    sb.append("\nDiploma: ").append(p.diplomaTitulo());
                    if (p.diplomaNumero() != null && !p.diplomaNumero().isBlank()) {
                        sb.append(" (nº ").append(p.diplomaNumero()).append(")");
                    }
                }
                if (p.capitulo() != null) {
                    sb.append("\n").append(p.capitulo());
                }
                if (p.seccao() != null) {
                    sb.append(" | ").append(p.seccao());
                }
                sb.append("\n");
                sb.append(truncarTexto(p.texto()));
                n++;
            }
        } else if (contexto.artigosRecuperados() != null && !contexto.artigosRecuperados().isEmpty()) {
            sb.append("\n\n--- Artigos recuperados da biblioteca (cita com [n] no texto) ---");
            int n = 1;
            for (Artigo a : contexto.artigosRecuperados()) {
                if (contexto.artigo() != null && a.getId() != null && a.getId().equals(contexto.artigo().getId())) {
                    continue;
                }
                sb.append("\n\n[").append(n).append("] Artigo ").append(a.getNumero());
                if (a.getTitulo() != null && !a.getTitulo().isBlank()) {
                    sb.append(" — ").append(a.getTitulo());
                }
                if (a.getDiploma() != null) {
                    sb.append("\nDiploma: ").append(a.getDiploma().getTitulo());
                }
                sb.append(":\n").append(truncarTexto(a.getTexto()));
                n++;
            }
        }

        return sb.toString();
    }

    private static String truncarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= MAX_CHARS_ARTIGO) {
            return texto;
        }
        return texto.substring(0, MAX_CHARS_ARTIGO)
                + "\n[… texto truncado para caber no contexto do modelo …]";
    }

    private String template(String nome) {

        return cacheTemplates.computeIfAbsent(nome, this::carregarTemplate);
    }

    private String carregarTemplate(String nome) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + nome + ".txt");
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Template de prompt em falta: prompts/" + nome + ".txt", e);
        }
    }

    /**
     * Contexto jurídico opcional que ancora um pedido à IA. Qualquer
     * combinação dos campos pode estar presente; todos são opcionais.
     */
    public record ContextoJuridico(
            Diploma diploma,
            Artigo artigo,
            Resumo resumo,
            String trechoLivre,
            java.util.List<Artigo> artigosRecuperados,
            java.util.List<KnowledgePassage> passagensRecuperadas
    ) {
        public ContextoJuridico(Diploma diploma, Artigo artigo, Resumo resumo, String trechoLivre) {
            this(diploma, artigo, resumo, trechoLivre, java.util.List.of(), java.util.List.of());
        }

        public ContextoJuridico(Diploma diploma, Artigo artigo, Resumo resumo, String trechoLivre,
                                java.util.List<Artigo> artigosRecuperados) {
            this(diploma, artigo, resumo, trechoLivre,
                    artigosRecuperados != null ? artigosRecuperados : java.util.List.of(),
                    java.util.List.of());
        }

        public static ContextoJuridico vazio() {
            return new ContextoJuridico(null, null, null, null, java.util.List.of(), java.util.List.of());
        }

        public static ContextoJuridico comPassagens(Diploma diploma, Artigo artigo, String trecho,
                                                     java.util.List<KnowledgePassage> passagens) {
            return new ContextoJuridico(diploma, artigo, null, trecho, java.util.List.of(),
                    passagens != null ? passagens : java.util.List.of());
        }

        public boolean isVazio() {
            boolean semArtigos = artigosRecuperados == null || artigosRecuperados.isEmpty();
            boolean semPassagens = passagensRecuperadas == null || passagensRecuperadas.isEmpty();
            return diploma == null && artigo == null && resumo == null
                    && (trechoLivre == null || trechoLivre.isBlank())
                    && semArtigos && semPassagens;
        }
    }

    /**
     * Utilitário para truncar o histórico de conversa a uma janela de
     * contexto razoável, evitando prompts demasiado longos.
     */
    public List<ChatMessage> limitarJanela(List<ChatMessage> mensagens, int maximo) {
        if (mensagens.size() <= maximo) {
            return mensagens;
        }
        return mensagens.subList(mensagens.size() - maximo, mensagens.size());
    }
}
