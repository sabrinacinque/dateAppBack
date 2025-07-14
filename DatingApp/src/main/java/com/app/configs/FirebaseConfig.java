package com.app.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;
    
    @Value("${firebase.project.id}")
    private String projectId;
    
    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                
                // Controlla se il file delle credenziali esiste
                Resource resource = resourceLoader.getResource("file:" + credentialsPath);
                
                if (!resource.exists()) {
                    System.out.println("⚠️  File Firebase non trovato: " + credentialsPath);
                    System.out.println("📱 Le notifiche push saranno disabilitate, ma l'app funzionerà normalmente");
                    System.out.println("💡 Per abilitare Firebase, aggiungi il file firebase-service-account.json in: " + credentialsPath);
                    return;
                }
                
                InputStream serviceAccount = resource.getInputStream();
                
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();
                
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 Firebase inizializzato con successo per progetto: " + projectId);
            }
        } catch (IOException e) {
            System.err.println("❌ Errore inizializzazione Firebase: " + e.getMessage());
            System.out.println("📱 L'applicazione continuerà senza notifiche push");
        }
    }
}