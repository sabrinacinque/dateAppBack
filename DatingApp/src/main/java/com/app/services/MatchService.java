package com.app.services;

import com.app.dto.MatchDTO;
import com.app.entities.Match;
import com.app.entities.Utente;
import com.app.repositories.MatchRepository;
import com.app.repositories.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {
    
    @Autowired
    private MatchRepository matchRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
   
    
    /**
     * Verifica se un match esiste tra due utenti
     */
    public boolean existsMatchBetweenUsers(Utente utente1, Utente utente2) {
        
        System.out.println("=== MATCH SERVICE - CHECK MATCH EXISTS ===");
        System.out.println("Utente1 ID: " + utente1.getId() + " - Utente2 ID: " + utente2.getId());
        
        try {
            boolean exists = matchRepository.existsMatchBetweenUsers(utente1, utente2);
            System.out.println("Match esiste: " + exists);
            return exists;
            
        } catch (DataAccessException e) {
            throw new RuntimeException("Errore nella verifica del match", e);
        }
    }
    
    /**
     * Crea un nuovo match tra due utenti
     */
    @Transactional
    public Match createMatch(Utente utente1, Utente utente2) {
        
        System.out.println("=== MATCH SERVICE - CREATE MATCH ===");
        System.out.println("Utente1: " + utente1.getId() + " - Utente2: " + utente2.getId());
        
        try {
            // Verifica che non esista già un match
            boolean matchExists = existsMatchBetweenUsers(utente1, utente2);
            
            if (matchExists) {
                System.out.println("Match già esistente!");
                return null;
            }
            
            // Crea il nuovo match
            Match match = new Match();
            match.setUtente1(utente1);
            match.setUtente2(utente2);
            match.setTimestamp(java.time.LocalDateTime.now());
            
            Match savedMatch = matchRepository.save(match);
            
            System.out.println("Match creato con ID: " + savedMatch.getId());
            
            return savedMatch;
            
        } catch (DataAccessException e) {
            throw new RuntimeException("Errore nella creazione del match", e);
        }
    }
    
    
    
    // ========== METODI PRIVATI ==========
    
    /**
     * Converte un Match in MatchDTO per evitare problemi di serializzazione JSON
     */
    private MatchDTO convertToDTO(Match match) {
    	
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setTimestamp(match.getTimestamp());
        
        // Dati utente 1
        if (match.getUtente1() != null) {
        	//Utente utente1 = utenteRepository.findById(match.getUtente1().getId()).get();
        	Utente utente1 = match.getUtente1();
        	
            dto.setUtente1Id(match.getUtente1().getId());
            dto.setUtente1Nome(utente1.getNome());
            dto.setUtente1Email(utente1.getUsername());
        }
        
        // Dati utente 2
        if (match.getUtente2() != null) {
        	Utente utente2 = match.getUtente2();
            dto.setUtente2Id(match.getUtente2().getId());
            dto.setUtente2Nome(utente2.getNome());
            dto.setUtente2Email(utente2.getUsername());
        }
        
        return dto;
    }
    
    
    
    
    /**
     * Ottieni tutti i match di un utente - VERSIONE DTO (risolve problema JSON)
     * 🔥 AGGIORNATO: Filtra match con utenti disattivati
     */
    public List<MatchDTO> getMatchByUtente(String emailUtente) {
        
        System.out.println("=== MATCH SERVICE - GET MATCHES DTO ===");
        System.out.println("Email utente: " + emailUtente);
        
        try {
            // Trova l'utente che richiede i match
            Utente utente = utenteRepository.findByUsername(emailUtente)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
            
            System.out.println("Utente ID: " + utente.getId());
            
            // Trova tutti i match dell'utente
            List<Match> matches = matchRepository.findMatchesByUtente(utente);
            
            System.out.println("Matches trovati (totali): " + matches.size());
            
            // 🔥 FILTRA MATCH CON UTENTI ATTIVI
            List<Match> matchesAttivi = matches.stream()
                .filter(match -> {
                    boolean utente1Attivo = match.getUtente1().isAttivo();
                    boolean utente2Attivo = match.getUtente2().isAttivo();
                    boolean matchValido = utente1Attivo && utente2Attivo;
                    
                    if (!matchValido) {
                        System.out.println("🚫 Match " + match.getId() + " filtrato - Utente1 attivo: " + 
                            utente1Attivo + ", Utente2 attivo: " + utente2Attivo);
                    }
                    
                    return matchValido;
                })
                .collect(Collectors.toList());
            
            System.out.println("Matches con utenti attivi: " + matchesAttivi.size());
            
            // Converti in DTO per evitare problemi di serializzazione JSON
            List<MatchDTO> matchDTOs = matchesAttivi.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            System.out.println("MatchDTOs creati: " + matchDTOs.size());
            
            return matchDTOs;
            
        } catch (DataAccessException e) {
            throw new RuntimeException("Errore nel recupero dei match dal database", e);
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Utente non trovato", e);
        }
    }

    /**
     * Ottieni dettagli di un match specifico - VERSIONE DTO
     * 🔥 AGGIORNATO: Controlla che entrambi gli utenti siano attivi
     */
    public MatchDTO getMatchDetails(Long matchId, String emailUtente) {
        
        System.out.println("=== MATCH SERVICE - GET MATCH DETAILS DTO ===");
        System.out.println("Match ID: " + matchId);
        System.out.println("Email utente: " + emailUtente);
        
        try {
            // Trova l'utente
            Utente utente = utenteRepository.findByUsername(emailUtente)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
            
            // Trova il match specifico
            Match match = matchRepository.findById(matchId).orElse(null);
            
            if (match != null) {
                // Verifica che l'utente sia coinvolto nel match
                boolean isUserInMatch = match.getUtente1().equals(utente) || 
                                       match.getUtente2().equals(utente);
                
                if (isUserInMatch) {
                    // 🔥 CONTROLLA CHE ENTRAMBI GLI UTENTI SIANO ATTIVI
                    boolean utente1Attivo = match.getUtente1().isAttivo();
                    boolean utente2Attivo = match.getUtente2().isAttivo();
                    
                    if (utente1Attivo && utente2Attivo) {
                        System.out.println("Match trovato e autorizzato - entrambi utenti attivi");
                        return convertToDTO(match);
                    } else {
                        System.out.println("🚫 Match non disponibile - uno o entrambi gli utenti disattivati");
                        return null;
                    }
                } else {
                    System.out.println("Utente non autorizzato per questo match");
                    return null;
                }
            } else {
                System.out.println("Match non trovato");
                return null;
            }
            
        } catch (DataAccessException e) {
            throw new RuntimeException("Errore nel recupero del match dal database", e);
        }
    }

    /**
     * Conta i match totali di un utente
     * 🔥 AGGIORNATO: Conta solo match con utenti attivi
     */
    public Long countMatchByUtente(String emailUtente) {
        
        System.out.println("=== MATCH SERVICE - COUNT MATCHES ===");
        System.out.println("Email utente: " + emailUtente);
        
        try {
            Utente utente = utenteRepository.findByUsername(emailUtente)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
            
            // 🔥 USA IL METODO FILTRATO PER IL CONTEGGIO
            List<MatchDTO> matchesAttivi = getMatchByUtente(emailUtente);
            Long count = (long) matchesAttivi.size();
            
            System.out.println("Match attivi totali: " + count);
            
            return count;
            
        } catch (DataAccessException e) {
            throw new RuntimeException("Errore nel conteggio dei match", e);
        }
    }
    
 
    
}