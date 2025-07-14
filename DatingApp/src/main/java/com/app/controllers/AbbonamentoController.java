package com.app.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.dto.AbbonamentoDTO;
import com.app.entities.Abbonamento;
import com.app.entities.Utente;
import com.app.repositories.AbbonamentoRepository;
import com.app.repositories.UtenteRepository;
import com.app.services.AbbonamentoService;
import com.app.services.StripeService;
import com.app.services.UtenteService;
import com.stripe.model.checkout.Session;

@RestController
@RequestMapping("/api/premium")
public class AbbonamentoController {

    @Autowired
    UtenteRepository utenteRepository;
    
    @Autowired
    UtenteService utenteService;
    
    @Autowired
    AbbonamentoRepository abbonamentoRepository;
    
    @Autowired
    AbbonamentoService abbonamentoService;
    
    @Autowired
    StripeService stripeService;

    AbbonamentoController(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }
    
    // --------------- UPGRADE ACCOUNT A PREMIUM (SISTEMATO)
    @PostMapping("/upgrade")
    public ResponseEntity<?> createSubscription(@RequestParam String metodoPagamento) {
        try {
            String stripeSubscriptionId = null;
            Utente utente = utenteService.getCurrentUser();
            
            metodoPagamento = metodoPagamento.toUpperCase();
            
            //ACCETTA SOLO STRIPE O PAYPAL
            if (!metodoPagamento.equals("STRIPE") && !metodoPagamento.equals("PAYPAL"))
                return ResponseEntity.badRequest().body("Metodo di pagamento non accettato");
            
            Optional<Abbonamento> ultimoAbbonamentoOpt = abbonamentoService.getLastSubscriptionByUserId(utente);
            
            if (ultimoAbbonamentoOpt.isPresent()) {
                Abbonamento ultimoAbbonamento = ultimoAbbonamentoOpt.get();
                
                if (ultimoAbbonamento.getTipo().equals("PREMIUM") && ultimoAbbonamento.isAttivo() == true) {
                    return ResponseEntity.ok().body("Abbonamento già attivo!");
                }
            }
            
            /* SE UTILIZZI METODO STRIPE RICEVI ID da collegamento API stripe */
            if (metodoPagamento.equals("STRIPE")) {
                Session session = stripeService.createCheckoutSession("http://localhost:8080/success",
                        "http://localhost:8080/cancel", "Abbonamento Premium", "price_1RY4sNP0z2XEGqAd6zLfQjCr");
                
                stripeSubscriptionId = session.getId();
            }
            
            Abbonamento nuovoAbbonamento = new Abbonamento(utente, "PREMIUM", metodoPagamento, stripeSubscriptionId);
            //Imposta tipoAccount dell'utente loggato a "PREMIUM"
            utente.setTipoAccount("PREMIUM");
            utenteRepository.save(utente);
            
            // 🔥 SISTEMATO: Ritorna DTO invece di entity
            AbbonamentoDTO abbonamentoDTO = abbonamentoService.createAbbonamentoDTO(nuovoAbbonamento);
            return ResponseEntity.ok(abbonamentoDTO);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore durante la creazione abbonamento:" + e.getMessage());
        }
    }
    
    // --------------- VERIFICA SE ABBONAMENTO ATTIVO (SISTEMATO)
    @GetMapping("/status")
    public ResponseEntity<?> subscriptionStatus() {
        try {
            Utente utente = utenteService.getCurrentUser();
            
            // 🔥 SISTEMATO: Ritorna DTO invece di Optional<Abbonamento>
            AbbonamentoDTO abbonamentoDTO = abbonamentoService.getLastSubscriptionDTOByUserId(utente);
            
            if (abbonamentoDTO != null) {
                return ResponseEntity.ok(abbonamentoDTO);
            } else {
                return ResponseEntity.ok("Nessun abbonamento trovato");
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore durante la visualizzazione abbonamento:" + e.getMessage());
        }
    }
    
    // --------------- STORICO ABBONAMENTI (SISTEMATO)
    @GetMapping("/subscriptionHistory")
    public ResponseEntity<?> getSubscriptionHistory() {
        try {
            Utente utente = utenteService.getCurrentUser();
            
            // 🔥 SISTEMATO: Ritorna List<AbbonamentoDTO> invece di List<Abbonamento>
            List<AbbonamentoDTO> storicoDTO = abbonamentoService.getSubscriptionHistoryDTOByUserId(utente);
            return ResponseEntity.ok(storicoDTO);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore durante la visualizzazione dello storico:" + e.getMessage());
        }
    }
    
    // --------------- ENDPOINT PER TEST DOWNLGRADE ABBONAMENTI
    @GetMapping("/test")
    public void testDowngrade() {
        abbonamentoService.controllaScadenzeAbbonamenti();
    }
}