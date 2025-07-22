package com.app.services;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.dto.PreferenzeDto;
import com.app.dto.UtenteDiscoverDTO;
import com.app.entities.Preferenze;
import com.app.entities.Utente;
import com.app.repositories.PreferenceRepository;
import com.app.repositories.UtenteRepository;

import jakarta.validation.Validator;

@Service
public class PreferenzeService {

	@Autowired
	private UtenteRepository utenteRepository;

	@Autowired
	private PreferenceRepository preferenceRepository;
	
	@Autowired 
	Validator validator;

	// Metodo per visualizzare le preferenze dell'utente
    public ResponseEntity<?> getPreferenzeByUtenteId(String username) {		
		
    	// Recupera l'utente autenticato
		Utente utente = utenteRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Utente non trovato"));					
		          
		// Cerca le preferenze esistenti
        Optional<Preferenze> preferenzeOpt = preferenceRepository.findByUtenteId(utente.getId());           
        
        Preferenze preferenze;       
        
        if (preferenzeOpt.isEmpty()) {  
        	// Crea nuove preferenze vuote se non esistono
        	preferenze = new Preferenze(utente);
        	preferenceRepository.save(preferenze);
        } else {
        	// Visualizza le preferenze esistenti
        	preferenze = preferenzeOpt.get();
        }
        
        // Ritorna DTO invece dell'entity
        PreferenzeDto preferenzeDto = new PreferenzeDto(
            preferenze.getGenerePreferito(),
            preferenze.getMinEta(),
            preferenze.getMaxEta(),
            preferenze.getDistanzaMax()
        );
        return ResponseEntity.ok(preferenzeDto);
    }

	// Metodo per modificare le preferenze dell'utente
    public ResponseEntity<?> modificaPreferenze(String username, PreferenzeDto nuovePreferenze) {
    	
    	// Recupera l'utente autenticato
		Utente utente = utenteRepository.findByUsername(username)		
				.orElseThrow(() -> new RuntimeException("Utente non trovato"));		
		
		if (nuovePreferenze.getMinEta() != null && nuovePreferenze.getMinEta() < 18) 
		    return ResponseEntity.badRequest().body("Età Minima: Inserisci un valore maggiore di 17 anni");		
		if (nuovePreferenze.getMinEta() != null && nuovePreferenze.getMinEta() > 100) 
			return ResponseEntity.badRequest().body("Età Minima: Inserisci un valore minore di 100 anni");
		if (nuovePreferenze.getMaxEta() != null && nuovePreferenze.getMaxEta() < 18) 
			return ResponseEntity.badRequest().body("Età Massima: Inserisci un valore maggiore di 18 anni");
		if (nuovePreferenze.getMaxEta() != null && nuovePreferenze.getMaxEta() > 100) 
			return ResponseEntity.badRequest().body("Età Massima: Inserisci un valore minore di 100 anni");
		if (nuovePreferenze.getMinEta() != null && nuovePreferenze.getMaxEta() != null && nuovePreferenze.getMinEta() > nuovePreferenze.getMaxEta()) 
			return ResponseEntity.badRequest().body("L'Età Minima non può essere maggiore dell'età massima. Inserisci dei valori corretti");
		if (nuovePreferenze.getDistanzaMax() != null && nuovePreferenze.getDistanzaMax() < 0) 
			return ResponseEntity.badRequest().body("Distanza Massima: Inserisci un valore maggiore di zero!");		
		
		// Cerca le preferenze esistenti
		Optional<Preferenze> preferenzeOpt = preferenceRepository.findByUtenteId(utente.getId());
		
		Preferenze preferenze;			
		
        if (preferenzeOpt.isEmpty()) { 
        	// Crea nuove preferenze se non esistono
        	preferenze = new Preferenze(
        			utente, 
        			nuovePreferenze.getGenerePreferito(),
        			nuovePreferenze.getMinEta(), 
        			nuovePreferenze.getMaxEta(), 
        			nuovePreferenze.getDistanzaMax());	        	
        } else {
        	// Aggiorna le preferenze esistenti
        	preferenze = preferenzeOpt.get();
        	preferenze.setGenerePreferito(nuovePreferenze.getGenerePreferito());
        	preferenze.setMinEta(nuovePreferenze.getMinEta());
        	preferenze.setMaxEta(nuovePreferenze.getMaxEta());
        	preferenze.setDistanzaMax(nuovePreferenze.getDistanzaMax());
        }
        
        // Salva le preferenze (nuove o aggiornate)
    	preferenceRepository.save(preferenze);
    	
    	// Ritorna DTO invece dell'entity
        PreferenzeDto preferenzeDto = new PreferenzeDto(
            preferenze.getGenerePreferito(),
            preferenze.getMinEta(),
            preferenze.getMaxEta(),
            preferenze.getDistanzaMax()
        );
        return ResponseEntity.ok(preferenzeDto);
    }    
    
    // Metodo per calcolare l'età
    private int calcolaEta(LocalDate dataNascita) {
        if (dataNascita == null) {
            return 0;
        }
        return Period.between(dataNascita, LocalDate.now()).getYears();
    }
    

 // 🔥 SOSTITUISCI IL METODO getUtentiByPreferenze nel PreferenzeService

    public ResponseEntity<?> getUtentiByPreferenze(String username, int page, int size) {
        
        // Recupero l'utente autenticato e le sue preferenze
        Utente utente = utenteRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        
        // Cerca le preferenze del utente
        Optional<Preferenze> preferenzeOpt = preferenceRepository.findByUtenteId(utente.getId());       
        Preferenze preferenze = preferenzeOpt.orElse(null);
        
        // Calcola il filtro da applicare al database in base alle preferenza di età salvate 
        LocalDate dataRiferimento = LocalDate.of(LocalDate.now().getYear(), 1, 1); // Prendo come data di riferimento il primo giorno dell'anno
        // Calcolo la data di nascita minima preferita: data di nascita minima = anno corrente - eta minima
        LocalDate dataMin = preferenze != null && preferenze.getMinEta() != null ? dataRiferimento.minusYears(preferenze.getMinEta()) : null;
        // Calcolo la data di nascita massima preferita: data di nascita massima = anno corrente - eta massima
        LocalDate dataMax = preferenze != null && preferenze.getMaxEta() != null ? dataRiferimento.minusYears(preferenze.getMaxEta()) : null;

        // Richiedo l'estrapolazione di una sola pagina con max 20 utenti per non appesantire il programma
        Pageable pageable = PageRequest.of(page, size);

        List<Utente> utenti = utenteRepository.findUtentiByPreferenze(
            utente.getId(),
            preferenze != null ? preferenze.getGenerePreferito().name() : null,
            preferenze != null ? preferenze.getMinEta() : null,
            preferenze != null ? preferenze.getMaxEta() : null,
            dataMin,
            dataMax,
            preferenze != null ? preferenze.getDistanzaMax() : null,
            utente.getPosizione().getLatitudine(),
            utente.getPosizione().getLongitudine(),
            pageable
        );

        // 🔥 FILTRA SOLO UTENTI ATTIVI
        List<Utente> utentiAttivi = utenti.stream()
            .filter(u -> u.isAttivo()) // 🔥 SOLO UTENTI CON ACCOUNT ATTIVO
            .collect(Collectors.toList());

        System.out.println("🔍 Utenti totali trovati: " + utenti.size());
        System.out.println("✅ Utenti attivi filtrati: " + utentiAttivi.size());

        // Converte gli utenti in DTO
        List<UtenteDiscoverDTO> utentiDTO = utentiAttivi.stream()
            .map(u -> new UtenteDiscoverDTO(
                u.getId(),                    // 1. Long id
                u.getNome(),                  // 2. String nome  
                u.getUsername(),              // 3. String username ✅
                u.getGenere() != null ? u.getGenere().toString() : null, // 4. String genere ✅
                u.getDataNascita(),           // 5. LocalDate dataNascita ✅
                u.getBio(),                   // 6. String bio ✅
                u.getInteressi(),             // 7. String interessi ✅
                u.getFotoProfilo(),           // 8. String fotoProfilo ✅
                u.getPosizione() != null ? u.getPosizione().getCitta() : null, // 9. String citta ✅
                calcolaEta(u.getDataNascita()), // 10. Integer eta ✅
                u.getNotificheAttive()        // 11. Boolean notificheAttive ✅
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(utentiDTO);
    }
}