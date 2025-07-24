package com.app.controllers;

import com.app.dto.LoginRequest;
import com.app.dto.LoginResponse;
import com.app.dto.RegistrazioneDto;
import com.app.entities.Utente;
import com.app.models.VerificationToken;
import com.app.repositories.UtenteRepository;
import com.app.repositories.VerificationTokenRepository;
import com.app.security.JwtUtil;
import com.app.services.UtenteService;

import com.app.entities.PasswordResetToken;
import com.app.repositories.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.app.services.EmailService;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

 
/**
 * Controller per gestire autenticazione e registrazione degli utenti.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Permette richieste da qualsiasi origine (per sviluppo)
public class AuthController {
 
    @Autowired
    private AuthenticationManager authenticationManager;
 
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
 
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

 
    @Autowired
    private UtenteService utenteService;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
 
    /**
     * Endpoint per la registrazione di un nuovo utente.
     * POST /api/auth/register
     */

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrazioneDto registrazioneDto) {
        try {
            // Verifica se l'email è già in uso
            if (utenteService.existsByEmail(registrazioneDto.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(new LoginResponse(null, "Email già in uso", null, null,null));
            }
 
            // Crea il nuovo utente
            Utente nuovoUtente = utenteService.createUtente(registrazioneDto);
 
            return ResponseEntity.ok(new LoginResponse(
                "Disponibile dopo il login",												// Token vuoto volendo da implementare nuovo modello 
                "Registrazione completata con successo", 
                nuovoUtente.getId(),
                nuovoUtente.getTipoAccount(),
                nuovoUtente.isPrimoAccesso()
            ));
 
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore durante la registrazione: " + e.getMessage(), null, null,null));
        }
    }
 
    /**
     * Endpoint per il login degli utenti.
     * POST /api/auth/login
     */
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            
            // 🔥 PRIMA CONTROLLA SE L'UTENTE ESISTE E È ATTIVO
            Utente utente = utenteService.findByEmail(loginRequest.getEmail());
            
            if (utente == null) {
                return ResponseEntity.badRequest()
                    .body(new LoginResponse(null, "Credenziali non valide", null, null, null));
            }
            
            // 🔥 CONTROLLO ACCOUNT ATTIVO
            if (!utente.isAttivo()) {
                return ResponseEntity.badRequest()
                    .body(new LoginResponse(null, "Account non attivato. Controlla la tua email per attivare l'account.", null, null, null));
            }
            
            // Autentica l'utente usando email e password
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), 
                    loginRequest.getPassword()
                )
            );

            // 🔥 GESTIONE PRIMO ACCESSO
            if (!utente.isPrimoAccesso()) { // Se NON ha mai fatto il primo accesso
                utente.setPrimoAccesso(true); // Segna che ha fatto il primo accesso
                utenteRepository.save(utente);
                System.out.println("✅ Primo login completato per: " + utente.getUsername());
            }

            // Genera il token JWT
            String token = jwtUtil.generateToken(
                utente.getId(), 
                utente.getUsername(), 
                utente.getTipoAccount()
            );

            return ResponseEntity.ok(new LoginResponse(
                token, 
                "Login effettuato con successo", 
                utente.getId(),
                utente.getTipoAccount(),
                utente.isPrimoAccesso()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Credenziali non valide", null, null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore durante il login: " + e.getMessage(), null, null, null));
        }
    }
 
    /**
     * Endpoint per validare un token JWT.
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                String username = jwtUtil.extractUsername(jwtToken);
                Long userId = jwtUtil.extractUserId(jwtToken);
                String accountType = jwtUtil.extractAccountType(jwtToken);
 
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
 
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    return ResponseEntity.ok(new LoginResponse(
                        jwtToken, 
                        "Token valido", 
                        userId,
                        accountType,
                        null
                    ));
                }
            }
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Token non valido", null, null,null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore nella validazione del token", null, null,null));
        }
    }
    
  //ENDPOINT CONFERMA REGISTRAZIONE
    @GetMapping("/confirm")
    public ResponseEntity<Void> confirmUser(@RequestParam("token") String token) {
        try {
            VerificationToken verificationToken = verificationTokenRepository.findByToken(token);

            if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                // Token non valido → redirect con errore
                return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/confirm?error=true&message=Token%20non%20valido%20o%20scaduto") // 🔥 CAMBIATO
                    .build();
            }

            Utente utente = verificationToken.getUtente();
            utente.setAttivo(true);
            utenteRepository.save(utente);

            // Successo → redirect con conferma
            return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/confirm?success=true") // 🔥 CAMBIATO
                .build();
                
        } catch (Exception e) {
            // Errore generico → redirect con errore
            return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/confirm?error=true&message=" + e.getMessage()) // 🔥 CAMBIATO
                .build();
        }
    }
    
    /**
     * Endpoint per il logout degli utenti.
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                
                // Verifica che il token sia valido
                String username = jwtUtil.extractUsername(jwtToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    // Qui potresti aggiungere logica per invalidare il token
                    // Per ora facciamo un logout "soft" - il frontend rimuoverà il token
                    
                    return ResponseEntity.ok(new LoginResponse(
                        null, 
                        "Logout effettuato con successo", 
                        null,
                        null,
                        null
                    ));
                } else {
                    return ResponseEntity.badRequest()
                        .body(new LoginResponse(null, "Token non valido", null, null,null));
                }
            }
            
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Token mancante", null, null,null));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore durante il logout: " + e.getMessage(), null, null,null));
        }
    }
    
    
 // 🔥 ENDPOINT RECUPERO PASSWORD CON INVIO EMAIL
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email richiesta"));
        }
        
        try {
            // Cerca l'utente nel database
            Optional<Utente> utenteOpt = utenteRepository.findByUsername(email);
            
            if (utenteOpt.isEmpty()) {
                // Per sicurezza, rispondi sempre OK anche se l'email non esiste
                return ResponseEntity.ok().body(Map.of("message", "Se l'email esiste, riceverai il link di reset"));
            }
            
            // Invalida eventuali token precedenti per questa email
            passwordResetTokenRepository.invalidateAllTokensForEmail(email);
            
            // Genera nuovo token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, email);
            passwordResetTokenRepository.save(resetToken);
            
            // 🔥 INVIA EMAIL REALE
            try {
                emailService.sendPasswordResetEmail(email, token);
                System.out.println("✅ Email di reset inviata a: " + email);
            } catch (Exception emailError) {
                System.err.println("❌ Errore invio email: " + emailError.getMessage());
                // Continua comunque, il token è salvato
            }
            
            return ResponseEntity.ok().body(Map.of("message", "Se l'email esiste, riceverai il link di reset"));
            
        } catch (Exception e) {
            System.err.println("❌ Errore forgot password: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Errore interno del server"));
        }
    }

    // 🔥 ENDPOINT RESET PASSWORD - VERSIONE CORRETTA
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token e password (min 6 caratteri) richiesti"));
        }
        
        try {
            // Cerca il token
            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
            
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token non valido"));
            }
            
            PasswordResetToken resetToken = tokenOpt.get();
            
            // Verifica che il token sia valido
            if (!resetToken.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token scaduto o già utilizzato"));
            }
            
            // Cerca l'utente
            Optional<Utente> utenteOpt = utenteRepository.findByUsername(resetToken.getEmail());
            
            if (utenteOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Utente non trovato"));
            }
            
            Utente utente = utenteOpt.get();
            
            // Aggiorna la password
            utente.setPassword(passwordEncoder.encode(newPassword));
            utenteRepository.save(utente);
            
            // Marca token come usato
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);
            
            System.out.println("✅ Password reset completato per: " + utente.getUsername());
            
            return ResponseEntity.ok().body(Map.of("message", "Password aggiornata con successo"));
            
        } catch (Exception e) {
            System.err.println("❌ Errore reset password: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Errore interno del server"));
        }
    }
}