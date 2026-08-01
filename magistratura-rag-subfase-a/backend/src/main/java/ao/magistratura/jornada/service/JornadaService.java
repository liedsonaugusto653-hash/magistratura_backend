package ao.magistratura.jornada.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class JornadaService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JornadaService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public Map<String, Object> obterProgresso(UUID utilizadorId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT momento_id, cena_id, concluidos_json
                FROM utilizador_jornada_progresso
                WHERE utilizador_id = ?
                """,
                utilizadorId
        );
        if (rows.isEmpty()) {
            return Map.of(
                    "momentoId", null,
                    "cenaId", null,
                    "concluidos", List.of()
            );
        }
        Map<String, Object> r = rows.get(0);
        List<String> concluidos = parseConcluidos((String) r.get("concluidos_json"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("momentoId", r.get("momento_id"));
        out.put("cenaId", r.get("cena_id"));
        out.put("concluidos", concluidos);
        return out;
    }

    @Transactional
    public Map<String, Object> guardarProgresso(
            UUID utilizadorId,
            String momentoId,
            String cenaId,
            List<String> concluidos
    ) {
        String json;
        try {
            json = mapper.writeValueAsString(concluidos != null ? concluidos : List.of());
        } catch (Exception e) {
            json = "[]";
        }
        jdbc.update(
                """
                INSERT INTO utilizador_jornada_progresso (utilizador_id, momento_id, cena_id, concluidos_json, actualizado_em)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (utilizador_id) DO UPDATE SET
                    momento_id = EXCLUDED.momento_id,
                    cena_id = EXCLUDED.cena_id,
                    concluidos_json = EXCLUDED.concluidos_json,
                    actualizado_em = NOW()
                """,
                utilizadorId, momentoId, cenaId, json
        );
        return obterProgresso(utilizadorId);
    }

    /**
     * A caminhada só expõe momentos ancorados em legislação que existe e está processada.
     */
    public Map<String, Object> disponibilidade() {
        Integer diplomas = jdbc.queryForObject(
                """
                SELECT COUNT(DISTINCT d.id)
                FROM diplomas d
                WHERE EXISTS (
                    SELECT 1 FROM artigos a WHERE a.diploma_id = d.id
                )
                """,
                Integer.class
        );
        int n = diplomas != null ? diplomas : 0;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("temDiplomaProcessado", n > 0);
        out.put("diplomasProcessados", n);
        return out;
    }

    private List<String> parseConcluidos(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
