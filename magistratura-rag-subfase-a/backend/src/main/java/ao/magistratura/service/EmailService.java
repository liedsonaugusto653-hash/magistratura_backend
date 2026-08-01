package ao.magistratura.service;

public interface EmailService {
    void enviarRecuperacaoPassword(String destinatarioEmail, String destinatarioNome, String link);
}
