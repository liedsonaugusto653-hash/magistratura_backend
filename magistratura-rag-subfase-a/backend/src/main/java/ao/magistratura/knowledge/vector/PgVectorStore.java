package ao.magistratura.knowledge.vector;

import ao.magistratura.knowledge.api.KnowledgeContentKind;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Persistência vetorial em PostgreSQL + extensão pgvector.
 * Ativar com app.knowledge.vector-store=pgvector (requer migration V21 e imagem pgvector).
 */
@Component
@ConditionalOnProperty(name = "app.knowledge.vector-store", havingValue = "pgvector")
@RequiredArgsConstructor
public class PgVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;

    @Override
    public void upsert(VectorRecord r) {
        jdbc.update("""
            INSERT INTO knowledge_vectors (
              id, artigo_id, diploma_id, documento_id, kind, texto,
              embedding, modelo_embedding, metadados)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?, ?, CAST(? AS vector), ?, CAST(? AS jsonb))
            ON CONFLICT (id) DO UPDATE SET
              texto = EXCLUDED.texto,
              embedding = EXCLUDED.embedding,
              modelo_embedding = EXCLUDED.modelo_embedding,
              metadados = EXCLUDED.metadados,
              atualizado_em = now()
            """,
                r.id().toString(),
                uuidOrNull(r.artigoId()),
                uuidOrNull(r.diplomaId()),
                uuidOrNull(r.documentoId()),
                r.kind() != null ? r.kind().name() : KnowledgeContentKind.LEGISLACAO.name(),
                r.texto(),
                toVectorLiteral(r.embedding()),
                r.modeloEmbedding(),
                r.metadadosJson() != null ? r.metadadosJson() : "{}");
    }

    @Override
    public void upsertAll(List<VectorRecord> records) {
        if (records == null) {
            return;
        }
        for (VectorRecord r : records) {
            upsert(r);
        }
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM knowledge_vectors WHERE id = ?::uuid", id.toString());
    }

    @Override
    public void deleteByDocumentoId(UUID documentoId) {
        jdbc.update("DELETE FROM knowledge_vectors WHERE documento_id = ?::uuid", documentoId.toString());
    }

    @Override
    public void deleteByArtigoId(UUID artigoId) {
        jdbc.update("DELETE FROM knowledge_vectors WHERE artigo_id = ?::uuid", artigoId.toString());
    }

    @Override
    public List<VectorSearchHit> search(VectorSearchRequest req) {
        String lit = toVectorLiteral(req.query());
        StringJoiner where = new StringJoiner(" AND ", " WHERE ", "");
        where.add("embedding IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(lit);

        if (req.modeloEmbedding() != null) {
            where.add("modelo_embedding = ?");
            args.add(req.modeloEmbedding());
        }
        if (req.diplomaId() != null) {
            where.add("diploma_id = ?::uuid");
            args.add(req.diplomaId().toString());
        }
        if (req.kind() != null) {
            where.add("kind = ?");
            args.add(req.kind().name());
        }

        args.add(lit);
        args.add(req.limite() > 0 ? req.limite() : 10);

        String sql = """
            SELECT id, artigo_id, diploma_id, documento_id, texto,
                   (embedding <=> CAST(? AS vector)) AS distance,
                   COALESCE(metadados::text, '{}') AS metadados
            FROM knowledge_vectors
            """ + where + """
            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT ?
            """;

        return jdbc.query(sql, (rs, rowNum) -> new VectorSearchHit(
                UUID.fromString(rs.getString("id")),
                parseUuid(rs.getString("artigo_id")),
                parseUuid(rs.getString("diploma_id")),
                parseUuid(rs.getString("documento_id")),
                rs.getString("texto"),
                rs.getDouble("distance"),
                rs.getString("metadados")
        ), args.toArray());
    }

    static String toVectorLiteral(float[] v) {
        if (v == null || v.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(v.length * 8).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    private static String uuidOrNull(UUID id) {
        return id != null ? id.toString() : null;
    }

    private static UUID parseUuid(String s) {
        return s != null ? UUID.fromString(s) : null;
    }
}
