package ao.magistratura.util;

import ao.magistratura.dto.ia.FlashcardGeradoResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.entity.OpcaoResposta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação rigorosa de questões e flashcards gerados por IA.
 * Só passam itens estruturalmente correctos e minimamente ancorados no contexto jurídico.
 * Não substitui revisão humana, mas evita gabaritos inventados ou opções vazias.
 */
public final class GeneratedStudyValidator {

    private static final Pattern SO_LETRAS = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final int MIN_ENUNCIADO = 20;
    private static final int MIN_OPCAO = 2;
    private static final int MIN_JUSTIFICACAO = 15;
    private static final int MIN_PERGUNTA = 8;
    private static final int MIN_RESPOSTA = 4;
    /** Fração mínima de tokens da resposta/justificação que devem aparecer no contexto. */
    private static final double MIN_OVERLAP = 0.12;

    private GeneratedStudyValidator() {
    }

    public static List<QuestaoGeradaResponse> filtrarQuestoesValidas(
            List<QuestaoGeradaResponse> geradas, String contextoJuridico) {
        List<QuestaoGeradaResponse> ok = new ArrayList<>();
        if (geradas == null) {
            return ok;
        }
        String ctxNorm = normalizar(contextoJuridico != null ? contextoJuridico : "");
        for (QuestaoGeradaResponse q : geradas) {
            if (q == null) continue;
            if (!estruturaQuestaoOk(q)) continue;
            OpcaoResposta letra = normalizarOpcao(q.respostaCorreta());
            if (letra == null) continue;
            String textoCorrecto = textoOpcao(q, letra);
            if (textoCorrecto == null || textoCorrecto.isBlank()) continue;
            // Justificação ou texto da opção correcta deve ter overlap mínimo com o contexto
            String ancora = (q.justificacao() != null ? q.justificacao() : "") + " " + textoCorrecto;
            if (!ctxNorm.isBlank() && overlapTokens(ancora, ctxNorm) < MIN_OVERLAP) {
                continue;
            }
            ok.add(new QuestaoGeradaResponse(
                    q.id(),
                    q.enunciado().trim(),
                    q.opcaoA().trim(),
                    q.opcaoB().trim(),
                    q.opcaoC().trim(),
                    q.opcaoD().trim(),
                    letra.name(),
                    q.justificacao() != null ? q.justificacao().trim() : ""
            ));
        }
        return ok;
    }

    public static List<FlashcardGeradoResponse> filtrarFlashcardsValidos(
            List<FlashcardGeradoResponse> gerados, String contextoJuridico) {
        List<FlashcardGeradoResponse> ok = new ArrayList<>();
        if (gerados == null) {
            return ok;
        }
        String ctxNorm = normalizar(contextoJuridico != null ? contextoJuridico : "");
        for (FlashcardGeradoResponse f : gerados) {
            if (f == null) continue;
            String p = f.pergunta() != null ? f.pergunta().trim() : "";
            String r = f.resposta() != null ? f.resposta().trim() : "";
            if (p.length() < MIN_PERGUNTA || r.length() < MIN_RESPOSTA) continue;
            if (p.equalsIgnoreCase(r)) continue;
            if (!ctxNorm.isBlank() && overlapTokens(r, ctxNorm) < MIN_OVERLAP) {
                continue;
            }
            ok.add(new FlashcardGeradoResponse(f.id(), p, r));
        }
        return ok;
    }

    private static boolean estruturaQuestaoOk(QuestaoGeradaResponse q) {
        if (q.enunciado() == null || q.enunciado().trim().length() < MIN_ENUNCIADO) return false;
        String a = safe(q.opcaoA());
        String b = safe(q.opcaoB());
        String c = safe(q.opcaoC());
        String d = safe(q.opcaoD());
        if (a.length() < MIN_OPCAO || b.length() < MIN_OPCAO
                || c.length() < MIN_OPCAO || d.length() < MIN_OPCAO) {
            return false;
        }
        Set<String> uniq = new HashSet<>();
        uniq.add(normalizar(a));
        uniq.add(normalizar(b));
        uniq.add(normalizar(c));
        uniq.add(normalizar(d));
        if (uniq.size() < 4) return false;
        if (q.justificacao() == null || q.justificacao().trim().length() < MIN_JUSTIFICACAO) {
            return false;
        }
        return normalizarOpcao(q.respostaCorreta()) != null;
    }

    private static String textoOpcao(QuestaoGeradaResponse q, OpcaoResposta letra) {
        return switch (letra) {
            case A -> q.opcaoA();
            case B -> q.opcaoB();
            case C -> q.opcaoC();
            case D -> q.opcaoD();
        };
    }

    private static OpcaoResposta normalizarOpcao(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        char c = s.charAt(0);
        if (c >= 'A' && c <= 'D') {
            try {
                return OpcaoResposta.valueOf(String.valueOf(c));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static double overlapTokens(String texto, String contextoNorm) {
        Set<String> tokens = tokens(texto);
        if (tokens.isEmpty()) return 0;
        int hit = 0;
        for (String t : tokens) {
            if (contextoNorm.contains(t)) hit++;
        }
        return (double) hit / tokens.size();
    }

    private static Set<String> tokens(String s) {
        Set<String> set = new HashSet<>();
        for (String w : normalizar(s).split("\\s+")) {
            if (w.length() >= 4) set.add(w);
        }
        return set;
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = SO_LETRAS.matcher(n.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return n.replaceAll("\\s+", " ").trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
