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

import jakarta.validation.Valid;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
 
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
    private UtenteService utenteService;
 
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
                    .body(new LoginResponse(null, "Email già in uso", null, null));
            }
 
            // Crea il nuovo utente
            Utente nuovoUtente = utenteService.createUtente(registrazioneDto);
 
            return ResponseEntity.ok(new LoginResponse(
                "Disponibile dopo il login",												// Token vuoto volendo da implementare nuovo modello 
                "Registrazione completata con successo", 
                nuovoUtente.getId(),
                nuovoUtente.getTipoAccount()
            ));
 
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore durante la registrazione: " + e.getMessage(), null, null));
        }
    }
 
    /**
     * Endpoint per il login degli utenti.
     * POST /api/auth/login
     */
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Autentica l'utente usando email e password
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), 
                    loginRequest.getPassword()
                )
            );
 
            // Se l'autenticazione ha successo, carica i dettagli dell'utente
            //UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
 
            // Ottiene i dati dell'utente dal database
            Utente utente = utenteService.findByEmail(loginRequest.getEmail());
 
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
                utente.getTipoAccount()
            ));
 
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Credenziali non valide", null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore durante il login: " + e.getMessage(), null, null));
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
                        accountType
                    ));
                }
            }
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Token non valido", null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Errore nella validazione del token", null, null));
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
                    .header("Location", "http://localhost:4200/confirm?error=true&message=Token%20non%20valido%20o%20scaduto")
                    .build();
            }

            Utente utente = verificationToken.getUtente();
            utente.setAttivo(true);
            utenteRepository.save(utente);

            // Successo → redirect con conferma
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:4200/confirm?success=true")
                .build();
                
        } catch (Exception e) {
            // Errore generico → redirect con errore
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:4200/confirm?error=true&message=" + e.getMessage())
                .build();
        }
    }
}