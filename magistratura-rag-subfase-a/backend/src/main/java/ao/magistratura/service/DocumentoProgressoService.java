package ao.magistratura.service;

import ao.magistratura.repository.DocumentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Actualiza progresso de processamento em transacções curtas ({@code REQUIRES_NEW})
 * para o frontend ver paginação OCR enquanto o job corre.
 * <p>
 * <strong>Pré-requisito:</strong> o pipeline NÃO pode correr dentro de uma
 * {@code @Transactional} longa que já tenha feito {@code save} na mesma linha
 * {@code documentos} — caso contrário esta TX nova bloqueia no lock de linha.
 * <p>
 * Com {@code lock_timeout} no PostgreSQL (ver {@code application.yml} /
 * Hikari {@code connection-init-sql}), a espera aborta em vez de hang infinito
 * (SQLState {@code 55P03}).
 */
@Service
public class DocumentoProgressoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProgressoService.class);

    private final DocumentoRepository documentoRepository;

    public DocumentoProgressoService(DocumentoRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void actualizar(UUID documentoId, int ok, int total, String mensagem) {
        try {
            documentoRepository.findById(documentoId).ifPresent(doc -> {
                doc.setProgressoPaginasOk(ok);
                doc.setProgressoPaginasTotal(total);
                int pct = total > 0 ? (int) Math.min(100, Math.round(100.0 * ok / total)) : 0;
                doc.setProgressoPercentagem(pct);
                if (mensagem != null && !mensagem.isBlank()) {
                    doc.setMensagemProgresso(mensagem.length() > 200 ? mensagem.substring(0, 200) : mensagem);
                }
                documentoRepository.save(doc);
            });
        } catch (PessimisticLockingFailureException | QueryTimeoutException e) {
            log.warn("Timeout de lock ao actualizar progresso documento={} (ok={}/{}): {} — "
                            + "verifica se o pipeline ainda corre numa TX longa; "
                            + "lock_timeout JDBC deve abortar em vez de hang.",
                    documentoId, ok, total, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mensagem(UUID documentoId, String mensagem) {
        try {
            documentoRepository.findById(documentoId).ifPresent(doc -> {
                if (mensagem != null) {
                    doc.setMensagemProgresso(mensagem.length() > 200 ? mensagem.substring(0, 200) : mensagem);
                    documentoRepository.save(doc);
                }
            });
        } catch (PessimisticLockingFailureException | QueryTimeoutException e) {
            log.warn("Timeout de lock ao gravar mensagem de progresso documento={}: {}",
                    documentoId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void limpar(UUID documentoId) {
        try {
            documentoRepository.findById(documentoId).ifPresent(doc -> {
                doc.setProgressoPaginasOk(null);
                doc.setProgressoPaginasTotal(null);
                doc.setProgressoPercentagem(null);
                documentoRepository.save(doc);
            });
        } catch (PessimisticLockingFailureException | QueryTimeoutException e) {
            log.warn("Timeout de lock ao limpar progresso documento={}: {}",
                    documentoId, e.getMessage());
        }
    }
}
