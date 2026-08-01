package ao.magistratura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio real via SMTP (Spring Mail). Requer spring-boot-starter-mail no pom
 * e app.email.mode=smtp.
 */
@Service
@ConditionalOnProperty(name = "app.email.mode", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.from:${spring.mail.username:noreply@magistratura.local}}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void enviarRecuperacaoPassword(String destinatarioEmail, String destinatarioNome, String link) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(destinatarioEmail);
        msg.setSubject("Magistratura — recuperação de palavra-passe");
        msg.setText(
                "Olá " + (destinatarioNome != null ? destinatarioNome : "") + ",\n\n"
                        + "Recebemos um pedido para redefinir a tua palavra-passe.\n"
                        + "Abre o link seguinte (válido por tempo limitado):\n\n"
                        + link + "\n\n"
                        + "Se não foste tu, ignora este email.\n\n"
                        + "— Plataforma Magistratura\n"
        );
        mailSender.send(msg);
        log.info("Email de recuperação enviado para {}", destinatarioEmail);
    }
}
