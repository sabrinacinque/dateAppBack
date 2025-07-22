package com.app.services;

import com.app.dto.CustomUserDto;
import com.app.entities.Utente;
import com.app.repositories.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
 
import java.util.Arrays;
import java.util.List;
 
/**
 * Servizio personalizzato per caricare i dettagli dell'utente dal database.
 * Implementa UserDetailsService di Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
 
    @Autowired
    private UtenteRepository utenteRepository;
 
    /**
     * Carica un utente dal database usando l'email come username
     * @param email l'email dell'utente (usata come username)
     * @return UserDetails oggetto che contiene le informazioni dell'utente
     * @throws UsernameNotFoundException se l'utente non viene trovato
     */
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
 
        // Cerca l'utente nel database usando l'email
        Utente utente = utenteRepository.findByUsername(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con email: " + email));
        
        // 🔥 CONTROLLO ACCOUNT ATTIVO - BLOCCA LOGIN SE NON ATTIVATO
        if (!utente.isAttivo()) {
            throw new UsernameNotFoundException("Account non attivato. Controlla la tua email per attivare l'account.");
        }
        
        // Definisce i ruoli dell'utente basati sul tipo di account
        List<SimpleGrantedAuthority> authorities = getAuthorities(utente);
    
        // Restituisce un oggetto User di Spring Security con:
        // - email come username, password hash, e lista di autorizzazioni
        return new CustomUserDto(
            utente.getId(),             // id
            utente.getUsername(),       // username (useremo l'email)
            utente.getPassword(),       // password hashata
            utente.isAttivo(),          // 🔥 account abilitato = attivo
            true,                       // account non scaduto
            true,                       // credenziali non scadute
            utente.isAttivo(),          // 🔥 account non bloccato = attivo
            authorities                 // lista delle autorizzazioni/ruoli
        );
    }
 
    /**
     * Determina le autorizzazioni dell'utente basate sul tipo di account
     * @param utente l'entità utente
     * @return lista delle autorizzazioni
     */
    
    private List<SimpleGrantedAuthority> getAuthorities(Utente utente) {
        
        if("ADMIN".equals(utente.getTipoAccount())) {
            // Gli utenti admin hanno tutti i ruoli
            return Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_PREMIUM"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        else if ("PREMIUM".equals(utente.getTipoAccount())) {
            // Gli utenti premium hanno entrambi i ruoli
            return Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_PREMIUM")
            );
        } else {
            // Gli utenti standard hanno solo il ruolo USER
            return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
}