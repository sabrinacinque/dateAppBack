package com.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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

            // Template HTML con logo inline e bottone rosa
            String htmlBody = createConfirmationEmailTemplate(confirmationUrl, to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText("Conferma la tua registrazione su LOVVAMI", htmlBody); // plain text + html
            helper.setFrom("LOVVAMI <emailtestingapp2025@gmail.com>");

            try {
                // ✅ CORRETTO: image/png (NON image/png+xml)
                ClassPathResource logo = new ClassPathResource("lovvami_logo_dark.png");

                if (logo.exists()) {
                    helper.addInline("logoLovvami", logo, "image/png");
                    System.out.println("✅ Logo LOVVAMI aggiunto da classpath!");
                } else {
                    // Fallback locale
                    java.io.File logoFile = new java.io.File("src/main/resources/lovvami_logo_dark.png");
                    if (logoFile.exists()) {
                        helper.addInline("logoLovvami", new org.springframework.core.io.FileSystemResource(logoFile), "image/png");
                        System.out.println("✅ Logo LOVVAMI aggiunto da filesystem!");
                    } else {
                        System.err.println("❌ Logo LOVVAMI non trovato");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Errore durante l'aggiunta del logo: " + e.getMessage());
                e.printStackTrace();
            }

            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Errore invio email: " + e.getMessage());
            // fallback
            sendSimpleConfirmationEmail(to, token);
        }
    }

    
    private String createConfirmationEmailTemplate(String confirmationUrl, String email) {
        return String.format("""
            <html>
            <body>
                <div style="text-align:center; max-width:600px; margin:0 auto; padding:20px; background-color:#f8f9fa;">
                    <div style="background:white; border-radius:12px; padding:40px; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                    
                        <h1 style="color:#2c3e50; margin-bottom:20px; font-size:28px;">Benvenuto in </h1>
                        
                        <div style="text-align:center; margin-bottom:30px;">
                            <img src='cid:logoLovvami' alt='LOVVAMI' style='width:200px; height:auto; margin:0 auto;'/>
                        </div>
                        
                        
                        
                        <div style="font-size:18px; color:#666; margin-bottom:30px; line-height:1.5;">
                            Grazie per esserti registrato!<br>
                            Per completare la registrazione, conferma il tuo indirizzo email:
                        </div>

                        <div style="background:#f8f9fa; padding:10px; border-radius:8px; margin:20px 0; font-family:monospace; color:#666;">
                            %s
                        </div>
                        
                        <a href="%s" style="display:inline-block; background:#E41196; color:#fff; padding:15px 40px; text-decoration:none; border-radius:25px; font-weight:bold; font-size:16px; margin:20px 0;">
                            ✨ Conferma Account ✨
                        </a>
                        
                        <div style="font-size:14px; color:#888; margin-top:30px; padding-top:20px; border-top:1px solid #eee;">
                            <strong>Cosa succede dopo?</strong><br>
                            • Completare il profilo<br>
                            • Scoprire persone interessanti<br>
                            • Iniziare a chattare ❤️
                        </div>

                        <div style="margin-top:30px; font-size:12px; color:#aaa;">
                            Se non hai richiesto questa registrazione, puoi ignorare questa email.<br>
                            <strong>Team LOVVAMI</strong>
                        </div>

                    </div>
                </div>
            </body>
            </html>
            """, email, confirmationUrl);
    }

    
    // Fallback per email semplice in caso di errori
    private void sendSimpleConfirmationEmail(String to, String token) {
        String subject = "Conferma registrazione - LOVVAMI";
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