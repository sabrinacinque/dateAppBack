package com.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendConfirmationEmail(String to, String token) {
        try {
            String subject = "Conferma la tua registrazione - LOVVAMI ❤️";
            String confirmationUrl = "https://dateappback-production.up.railway.app/api/auth/confirm?token=" + token;
            
            // Template HTML professionale
            String htmlBody = createConfirmationEmailTemplate(confirmationUrl, to);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            helper.setFrom("LOVVAMI <emailtestingapp2025@gmail.com>");
            
            mailSender.send(message);
            
        } catch (Exception e) {
            System.err.println("Errore invio email: " + e.getMessage());
            // Fallback: invia email semplice
            sendSimpleConfirmationEmail(to, token);
        }
    }
    
    private String createConfirmationEmailTemplate(String confirmationUrl, String email) {
        return "<!DOCTYPE html>" +
            "<html lang=\"it\">" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Conferma Registrazione - LOVVAMI</title>" +
                "<style>" +
                    "body {" +
                        "font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;" +
                        "line-height: 1.6;" +
                        "color: #333;" +
                        "max-width: 600px;" +
                        "margin: 0 auto;" +
                        "padding: 20px;" +
                        "background-color: #f8f9fa;" +
                    "}" +
                    ".container {" +
                        "background: white;" +
                        "border-radius: 12px;" +
                        "padding: 40px;" +
                        "box-shadow: 0 4px 20px rgba(0,0,0,0.1);" +
                        "text-align: center;" +
                    "}" +
                    ".logo-lovvami {" +
                        "font-size: 48px;" +
                        "font-weight: 900;" +
                        "margin: 0 auto 30px;" +
                        "text-align: center;" +
                        "letter-spacing: 2px;" +
                    "}" +
                    ".lov {" +
                        "color: #e91e63;" +
                        "position: relative;" +
                    "}" +
                    ".lov::after {" +
                        "content: '♥';" +
                        "position: absolute;" +
                        "top: -5px;" +
                        "right: -8px;" +
                        "font-size: 20px;" +
                        "color: #e91e63;" +
                    "}" +
                    ".vami {" +
                        "color: #1976d2;" +
                    "}" +
                    "h1 {" +
                        "color: #2c3e50;" +
                        "margin-bottom: 20px;" +
                        "font-size: 28px;" +
                    "}" +
                    ".welcome-text {" +
                        "font-size: 18px;" +
                        "color: #666;" +
                        "margin-bottom: 30px;" +
                        "line-height: 1.5;" +
                    "}" +
                    ".confirm-button {" +
                        "display: inline-block;" +
                        "background: linear-gradient(135deg, #ff6b6b, #ee5a6f);" +
                        "color: white;" +
                        "padding: 15px 40px;" +
                        "text-decoration: none;" +
                        "border-radius: 25px;" +
                        "font-weight: bold;" +
                        "font-size: 16px;" +
                        "margin: 20px 0;" +
                        "transition: all 0.3s ease;" +
                        "box-shadow: 0 4px 15px rgba(238, 90, 111, 0.3);" +
                    "}" +
                    ".info-text {" +
                        "font-size: 14px;" +
                        "color: #888;" +
                        "margin-top: 30px;" +
                        "padding-top: 20px;" +
                        "border-top: 1px solid #eee;" +
                    "}" +
                    ".footer {" +
                        "margin-top: 30px;" +
                        "font-size: 12px;" +
                        "color: #aaa;" +
                    "}" +
                    ".email-badge {" +
                        "background: #f8f9fa;" +
                        "padding: 10px;" +
                        "border-radius: 8px;" +
                        "margin: 20px 0;" +
                        "font-family: monospace;" +
                        "color: #666;" +
                    "}" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"logo-lovvami\">" +
                        "<span class=\"lov\">LOV</span><span class=\"vami\">VAMI</span>" +
                    "</div>" +
                    "<h1>Benvenuto in LOVVAMI!</h1>" +
                    "<div class=\"welcome-text\">" +
                        "Grazie per esserti registrato! Siamo entusiasti di averti nella nostra community.<br>" +
                        "Per completare la registrazione, conferma il tuo indirizzo email:" +
                    "</div>" +
                    "<div class=\"email-badge\">" + email + "</div>" +
                    "<a href=\"" + confirmationUrl + "\" class=\"confirm-button\">" +
                        "✨ Conferma Account ✨" +
                    "</a>" +
                    "<div class=\"info-text\">" +
                        "<strong>Cosa succede dopo?</strong><br>" +
                        "Una volta confermato l'account, potrai:<br>" +
                        "• Completare il tuo profilo<br>" +
                        "• Scoprire persone interessanti<br>" +
                        "• Iniziare a chattare e conoscere nuove persone<br>" +
                    "</div>" +
                    "<div class=\"footer\">" +
                        "Se non hai richiesto questa registrazione, puoi ignorare questa email.<br>" +
                        "<strong>Team LOVVAMI</strong> ❤️" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }
    
    // Fallback per email semplice in caso di errori
    private void sendSimpleConfirmationEmail(String to, String token) {
        String subject = "Conferma registrazione - DatingApp";
        String confirmationUrl = "https://dateappback-production.up.railway.app/api/auth/confirm?token=" + token;
        String body = "Clicca sul link per confermare l'account: " + confirmationUrl;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }
    
    public void sendPremiumSubscriptionReminder(String to, String name) {
        String subject = "Abbonamento PREMIUM in scadenza";
        String body = "Ciao " + name + "!\nIl tuo abbonamento è in scadenza, ricordati di rinnovare per non perdere l'accesso alle funzionalità PREMIUM! ;)";
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }
}