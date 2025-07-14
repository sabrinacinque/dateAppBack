package com.app.dto;

import java.time.LocalDate;

public class AbbonamentoDTO {
    
    private Long id;
    private String tipo;
    private String metodoPagamento;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private boolean attivo;
    private String stripeSubscriptionId;
    
    // ========== COSTRUTTORI ==========
    public AbbonamentoDTO() {}
    
    public AbbonamentoDTO(Long id, String tipo, String metodoPagamento, 
                         LocalDate dataInizio, LocalDate dataFine, 
                         boolean attivo, String stripeSubscriptionId) {
        this.id = id;
        this.tipo = tipo;
        this.metodoPagamento = metodoPagamento;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.attivo = attivo;
        this.stripeSubscriptionId = stripeSubscriptionId;
    }
    
    // ========== GETTER E SETTER ==========
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public String getMetodoPagamento() {
        return metodoPagamento;
    }
    
    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }
    
    public LocalDate getDataInizio() {
        return dataInizio;
    }
    
    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }
    
    public LocalDate getDataFine() {
        return dataFine;
    }
    
    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }
    
    public boolean isAttivo() {
        return attivo;
    }
    
    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }
    
    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }
    
    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }
}