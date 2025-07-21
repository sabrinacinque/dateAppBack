package com.app.services;

import com.app.services.FirebaseService;
import com.app.entities.Match;
import com.app.entities.Messaggio;
import com.app.entities.Utente;
import com.app.dto.MessaggioDTO;
import com.app.repositories.MatchRepository;
import com.app.repositories.MessaggioRepository;
import com.app.repositories.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessaggioService {
    
    @Autowired
    private MessaggioRepository messaggioRepository;
    
    @Autowired
    private MatchRepository matchRepository;
    
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private FirebaseService firebaseService;
    
    // ========== GET MESSAGGI PER MATCH ==========
    public List<MessaggioDTO> getMessaggiByMatch(Long matchId, String emailUtente) {
        
        System.out.println("=== MESSAGGIO SERVICE DEBUG ===");
        System.out.println("Match ID: " + matchId);
        System.out.println("Email utente: " + emailUtente);
        
        try {
            // Trova l'utente che sta facendo la richiesta
            Utente utente = utenteRepository.findByUsername(emailUtente)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
            
            System.out.println("Utente trovato - ID: " + utente.getId());
            
            // Trova il match
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match non trovato"));
            
            System.out.println("Match trovato - Utente1 ID: " + match.getUtente1().getId() + 
                             ", Utente2 ID: " + match.getUtente2().getId());
            
            // 🔥 VERIFICA AUTORIZZAZIONE - CONFRONTO ID INVECE DI OGGETTI
            if (!match.getUtente1().getId().equals(utente.getId()) && 
                !match.getUtente2().getId().equals(utente.getId())) {
                throw new IllegalArgumentException("Non sei autorizzato a vedere questa chat");
            }
            
            System.out.println("Utente autorizzato: " + utente.getId());
            
            // Recupera tutti i messaggi del match ordinati per timestamp
            List<Messaggio> messaggi = messaggioRepository.findByMatchOrderByTimestampAsc(match);
            
            System.out.println("Messaggi trovati: " + messaggi.size());
            
            // SETTIAMO TUTTI I MESSAGGI RICEVUTI COME LETTI
            List<Messaggio> messaggiDaAggiornare = new ArrayList<>();

            for (Messaggio m : messaggi) {
                // 🔥 CONFRONTO ID INVECE DI OGGETTI ANCHE QUI
                if (!m.getMittente().getId().equals(utente.getId()) && !m.getStato().equals("letto")) {
                    messaggiDaAggiornare.add(m);
                }
            }

            for (Messaggio m : messaggiDaAggiornare) {
                m.setStato("letto");
            }

            if (!messaggiDaAggiornare.isEmpty()) {
                messaggioRepository.saveAll(messaggiDaAggiornare);
                System.out.println("Aggiornati " + messaggiDaAggiornare.size() + " messaggi come letti");
            }
            
            return messaggi.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        } catch (DataAccessException e) {
            System.err.println("Errore database nel recupero messaggi: " + e.getMessage());
            throw new RuntimeException("Errore recupero messaggi", e);
        } catch (Exception e) {
            System.err.println("Errore generico nel recupero messaggi: " + e.getMessage());
            throw e;
        }
    }
    
    // ========== INVIA MESSAGGIO ==========
    public void inviaMessaggio(Long matchId, MessaggioDTO dto, String emailMittente) {
        
        System.out.println("=== INVIA MESSAGGIO SERVICE DEBUG ===");
        System.out.println("Match ID: " + matchId);
        System.out.println("Email mittente: " + emailMittente);
        System.out.println("Contenuto: " + dto.getContenuto());
        
        try {
            // Trova il mittente
            Utente mittente = utenteRepository.findByUsername(emailMittente)
                .orElseThrow(() -> new EntityNotFoundException("Utente mittente non trovato"));
            
            System.out.println("Mittente trovato - ID: " + mittente.getId());
            
            // Trova il match
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match non trovato"));
            
            System.out.println("Match trovato - Utente1 ID: " + match.getUtente1().getId() + 
                             ", Utente2 ID: " + match.getUtente2().getId());
            
            // 🔥 VERIFICA AUTORIZZAZIONE - CONFRONTO ID INVECE DI OGGETTI
            if (!match.getUtente1().getId().equals(mittente.getId()) && 
                !match.getUtente2().getId().equals(mittente.getId())) {
                throw new IllegalArgumentException("Non sei autorizzato a scrivere in questa chat");
            }
            
            // Valida il contenuto
            if (dto.getContenuto() == null || dto.getContenuto().trim().isEmpty()) {
                throw new IllegalArgumentException("Il messaggio non può essere vuoto");
            }
            
            if (dto.getContenuto().length() > 1000) {
                throw new IllegalArgumentException("Il messaggio è troppo lungo (max 1000 caratteri)");
            }
            
            // Crea il messaggio
            Messaggio messaggio = new Messaggio();
            messaggio.setMatch(match);
            messaggio.setMittente(mittente);
            messaggio.setContenuto(dto.getContenuto().trim());
            messaggio.setTimestamp(LocalDateTime.now());
            messaggio.setStato("inviato");
            
            messaggioRepository.save(messaggio);
            System.out.println("Messaggio salvato con successo!");
            
            // 🔥 TROVA IL DESTINATARIO - CONFRONTO ID INVECE DI OGGETTI
            Long destinatarioId = match.getUtente1().getId().equals(mittente.getId()) ?
                match.getUtente2().getId() : match.getUtente1().getId();
            
            System.out.println("Invio notifica Firebase al destinatario ID: " + destinatarioId);
            
            // Invio notifica Firebase al destinatario
            firebaseService.inviaNotificaMessaggio(destinatarioId, mittente.getNome(), dto.getContenuto());
            
        } catch (DataAccessException e) {
            System.err.println("Errore database nel salvataggio messaggio: " + e.getMessage());
            throw new RuntimeException("Errore salvataggio messaggio", e);
        } catch (Exception e) {
            System.err.println("Errore generico nell'invio messaggio: " + e.getMessage());
            throw e;
        }
    }
    
    // ========== METODI PRIVATI ==========
    
    private MessaggioDTO convertToDTO(Messaggio entity) {
        MessaggioDTO dto = new MessaggioDTO();
        dto.setId(entity.getId());
        dto.setMatchId(entity.getMatch().getId());
        dto.setMittenteId(entity.getMittente().getId());
        dto.setContenuto(entity.getContenuto());
        dto.setTimestamp(entity.getTimestamp());
        dto.setStato(entity.getStato());
        return dto;
    }
}