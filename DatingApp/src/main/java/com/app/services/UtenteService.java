package com.app.services;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.dto.ModificaUtenteDTO;
import com.app.dto.RegistrazioneDto;
import com.app.dto.UtenteDiscoverDTO;
import com.app.entities.Posizione;
import com.app.entities.Preferenze;
import com.app.entities.Utente;
import com.app.models.VerificationToken;
import com.app.repositories.MessaggioRepository;
import com.app.repositories.PreferenceRepository;
import com.app.repositories.UtenteRepository;
import com.app.repositories.VerificationTokenRepository;
import com.app.utils.SecurityUtils;

@Service
public class UtenteService {

    private final MessaggioRepository messaggioRepository;

	@Autowired
	private UtenteRepository utenteRepository;
	
	@Autowired 
	private PreferenceRepository preferenceRepository;
	
	@Autowired
    private VerificationTokenRepository tokenRepository;
	
	@Autowired
    private EmailService emailService;

    UtenteService(MessaggioRepository messaggioRepository) {
        this.messaggioRepository = messaggioRepository;
    }
	
	@Autowired
	private PasswordEncoder passwordEncoder;
    
	//REGISTRAZIONE
	public Utente createUtente(RegistrazioneDto registrazioneDto) {
			String encodedPassword = this.passwordEncoder.encode(registrazioneDto.getPassword().trim());
			Utente nuovoUtente = new Utente(registrazioneDto.getEmail(), encodedPassword);
			Preferenze preferenze = new Preferenze();
			
			preferenze.setUtente(nuovoUtente);
			
			utenteRepository.save(nuovoUtente);
			preferenceRepository.save(preferenze);
			
			//CREAZIONE ED INVIO TOKEN DI CONFERMA
	        String token = UUID.randomUUID().toString();
	        VerificationToken verificationToken = new VerificationToken(token, nuovoUtente);
	        tokenRepository.save(verificationToken);

	        emailService.sendConfirmationEmail(nuovoUtente.getUsername(), token);
			
			return nuovoUtente;
	}
	
	//CONTROLLO EMAIL ESISTENTE
	public boolean existsByEmail(String email)
	{
		return utenteRepository.existsByUsername(email);
	}
	
	//RITORNA UTENTE SE TROVATO PER EMAIL
	public Utente findByEmail(String email)
	{
		Utente utente = new Utente();
		if(utenteRepository.existsByUsername(email))
		{
			utente = utenteRepository.findByUsername(email).get();
			return utente;
		}
		return null;
	}
	
	//UPDATE UTENTE	
	public ResponseEntity<?> updateProfile(ModificaUtenteDTO uModificato) {
		
		try {			
				
			String email = SecurityUtils.getCurrentUserEmail();
			
			if (email == null)
			    return ResponseEntity.badRequest().body("Utente non autenticato");
			
			Utente uLoggato = utenteRepository.findByUsername(email)
			    .orElseThrow(() -> new RuntimeException("Utente non trovato"));
			  
			//VERIFICHE SU EMAIL  
			if ( !(uLoggato.getUsername().equals(uModificato.getUsername())) ) {
				if ( !(isEmailValida(uModificato.getUsername())) ) {
					return ResponseEntity.badRequest().body("Modifica username rifiutata: il valore digitato non è valido");
				} 
				else if ( utenteRepository.existsByUsername(uModificato.getUsername()) ) {
					return ResponseEntity.badRequest().body("Modifica username rifiutata: Esite già nel database"); 
				 }
			}
			
			if( uModificato.getDataNascita() == null)
				return ResponseEntity.badRequest().body("Non puoi lasciare il campo data di nascita vuoto!");
			
			// 🔥 NUOVA LOGICA PASSWORD - SOLO PER CAMBIO PASSWORD
			if (uModificato.getNewPassword() != null && !uModificato.getNewPassword().trim().isEmpty()) {
			    // CAMBIO PASSWORD - verifica password attuale
			    if (uModificato.getPassword() == null || uModificato.getPassword().trim().isEmpty()) {
			        return ResponseEntity.badRequest().body("Inserisci la password attuale per cambiarla!");
			    }
			    
			    // Verifica che la password attuale sia corretta
			    if (!passwordEncoder.matches(uModificato.getPassword(), uLoggato.getPassword())) {
			        return ResponseEntity.badRequest().body("Password attuale non corretta!");
			    }
			    
			    // Verifica nuova password
			    if (uModificato.getNewPassword().length() < 6) {
			        return ResponseEntity.badRequest().body("La nuova password deve essere di almeno 6 caratteri!");
			    }
			    
			    // Aggiorna con la nuova password
			    uLoggato.setPassword(passwordEncoder.encode(uModificato.getNewPassword().trim()));
			}
			// 🔥 NESSUNA VERIFICA PASSWORD PER AGGIORNAMENTI NORMALI - L'UTENTE È GIÀ AUTENTICATO CON JWT!
			
			uLoggato.setUsername(uModificato.getUsername().trim());
			
			if (uModificato.getNome() == null) uModificato.setNome("");
				uLoggato.setNome(uModificato.getNome().trim());

			uLoggato.setDataNascita(uModificato.getDataNascita());	
			
			if (uModificato.getBio() == (null)) uModificato.setBio("");
				uLoggato.setBio(uModificato.getBio().trim());
			
			if (uModificato.getGenere() == (null)) uModificato.setGenere(null);
			uLoggato.setGenere(uModificato.getGenere());
			
			if (uModificato.getInteressi() == (null)) uModificato.setInteressi("");
				uLoggato.setInteressi(uModificato.getInteressi().trim());
				
			// 🔥 GESTIONE POSIZIONE E COORDINATE
			if (uLoggato.getPosizione() == null) 
				uLoggato.setPosizione(new Posizione());
			
			// Aggiorna città se fornita
			if (uModificato.getCittà() != null && !uModificato.getCittà().trim().isEmpty()) {
				uLoggato.getPosizione().setCitta(uModificato.getCittà());
				System.out.println("✅ Città aggiornata: " + uModificato.getCittà());
			}
			
			// 🔥 COORDINATE DAL FRONTEND
			if (uModificato.getLatitudine() != null && uModificato.getLongitudine() != null) {
				uLoggato.getPosizione().setLatitudine(uModificato.getLatitudine());
				uLoggato.getPosizione().setLongitudine(uModificato.getLongitudine());
				System.out.println("✅ Coordinate aggiornate dal frontend: [" + 
					uModificato.getLatitudine() + ", " + uModificato.getLongitudine() + "]");
			}
			
			if (uModificato.getFotoProfilo() == (null)) uModificato.setFotoProfilo("");
				uLoggato.setFotoProfilo(uModificato.getFotoProfilo().trim());
			
			if (uModificato.getNotificheAttive()!=null)
			uLoggato.setNotificheAttive(uModificato.getNotificheAttive());
			
			uLoggato.setPrimoAccesso(false);
			
			utenteRepository.save(uLoggato);
			
			// Ritorna DTO invece dell'entity
			UtenteDiscoverDTO utenteDTO = new UtenteDiscoverDTO(
				    uLoggato.getId(),                    // 1. Long id
				    uLoggato.getNome(),                  // 2. String nome
				    uLoggato.getUsername(),              // 3. String username ✅
				    uLoggato.getGenere() != null ? uLoggato.getGenere().toString() : null, // 4. String genere ✅
				    uLoggato.getDataNascita(),           // 5. LocalDate dataNascita ✅
				    uLoggato.getBio(),                   // 6. String bio ✅
				    uLoggato.getInteressi(),             // 7. String interessi ✅
				    uLoggato.getFotoProfilo(),           // 8. String fotoProfilo ✅
				    uLoggato.getPosizione() != null ? uLoggato.getPosizione().getCitta() : null, // 9. String citta ✅
				    calcolaEta(uLoggato.getDataNascita()), // 10. Integer eta ✅
				    uLoggato.getNotificheAttive()        // 11. Boolean notificheAttive ✅
				);
			return ResponseEntity.ok(utenteDTO);
			
		 } catch (Exception e) {
			 return ResponseEntity.badRequest().body("Errore nell'aggiornamento del profilo: " + e.getMessage());
		 }	    
	}
		
	//UPDATE COORDINATE
	public ResponseEntity<?> updateLocation(double latitudine, double longitudine){
		
		Utente utente = getCurrentUser();
		if (utente.getPosizione() == null) {
			utente.setPosizione(new Posizione());
		}
		utente.getPosizione().setLatitudine(latitudine);
		utente.getPosizione().setLongitudine(longitudine);
		utenteRepository.save(utente);
		
		return ResponseEntity.ok("posizione aggiornata");
	}

	/**
	 * Metodo per verificare che l'email sia corretta.
	 */	
	public boolean isEmailValida(String email) {
	    if (email == null || email.trim().isEmpty()) {
	        return false;
	    }
	    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	}	

	/**
	 * Ottiene il profilo pubblico di un utente (senza informazioni sensibili)
	 * 🔥 VERSIONE AGGIORNATA CHE CONTROLLA UTENTI ATTIVI
	 */
	public UtenteDiscoverDTO getPublicProfile(Long userId) {
	    Utente utente = utenteRepository.findById(userId)
	        .orElseThrow(() -> new RuntimeException("Utente non trovato"));
	    
	    // 🔥 CONTROLLO SE L'UTENTE È ATTIVO
	    if (!utente.isAttivo()) {
	        throw new RuntimeException("Utente non disponibile");
	    }
	    
	    UtenteDiscoverDTO profiloDTO = new UtenteDiscoverDTO(
	        utente.getId(),                    // 1. Long id
	        utente.getNome(),                  // 2. String nome
	        utente.getUsername(),              // 3. String username ✅
	        utente.getGenere() != null ? utente.getGenere().toString() : null, // 4. String genere ✅
	        utente.getDataNascita(),           // 5. LocalDate dataNascita ✅
	        utente.getBio(),                   // 6. String bio ✅
	        utente.getInteressi(),             // 7. String interessi ✅
	        utente.getFotoProfilo(),           // 8. String fotoProfilo ✅
	        utente.getPosizione() != null ? utente.getPosizione().getCitta() : null, // 9. String citta ✅
	        calcolaEta(utente.getDataNascita()), // 10. Integer eta ✅
	        utente.getNotificheAttive()        // 11. Boolean notificheAttive ✅
	    );
	    
	    return profiloDTO;
	    
	}
	// Prende dati utente loggato (se loggato)
	public Utente getCurrentUser() {
		String currentUserEmail = SecurityUtils.getCurrentUserEmail();
		if (currentUserEmail == null) {
			throw new RuntimeException("Utente non autenticato");
		}
		return findByEmail(currentUserEmail);
	}
	
	// Restituisce true se il tipoAccount dell'utente loggato è "PREMIUM"
	public boolean isPremium() {
		Utente utente = getCurrentUser();
		boolean isPremium;
		
		if(utente.getTipoAccount().equals("PREMIUM")) {
			isPremium=true;
			}else {
				isPremium=false;
			}
		return isPremium;
	}
	
	// aggiorno device token per le notifiche di Firebase
	public void updateDeviceToken(String email, String deviceToken) {
		Utente utente = utenteRepository.findByUsername(email)
				.orElseThrow(()-> new RuntimeException("Utente non trovato"));
		
		utente.setDeviceToken(deviceToken);
		utenteRepository.save(utente);
	}
	
	// Controllo se l'utente ha notifiche attive
	public boolean hasNotificationsEnabled(Long utenteId) {
		Utente utente = utenteRepository.findById(utenteId)
				.orElse(null);
		
		return utente != null &&
				utente.getNotificheAttive() != null &&
				utente.getNotificheAttive() &&
				utente.getDeviceToken() != null;
	}
	
	// Abilita o disabilita le notifiche
	public void toggleNotifications(String email, boolean enabled) {
		Utente utente = utenteRepository.findByUsername(email)
				.orElseThrow(() -> new RuntimeException("Utente non trovato"));
		
		utente.setNotificheAttive(enabled);
		utenteRepository.save(utente);
	}
	
	//CALCOLA ETA
	public int calcolaEta(LocalDate dataNascita) {
		if (dataNascita == null) {
			System.out.println("===CALCOLA ETA DEBUG===");
			System.out.println("CAMPO ETA VUOTO");
			return 0;
	    }
	        return Period.between(dataNascita, LocalDate.now()).getYears();
	}
}