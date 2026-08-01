package ao.magistratura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Email apenas em log — activo quando app.email.mode=logging (defeito em dev).
 * Em produção, ProductionStartupValidator exige mode=smtp.
 */
@Service
@ConditionalOnProperty(name = "app.email.mode", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void enviarRecuperacaoPassword(String destinatarioEmail, String destinatarioNome, String link) {
        log.info("[EMAIL SIMULADO] Recuperação de password para {} <{}>: {}",
                destinatarioNome, destinatarioEmail, link);
    }
}
