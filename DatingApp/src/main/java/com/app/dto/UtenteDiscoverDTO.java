package com.app.dto;

import java.time.LocalDate;

public class UtenteDiscoverDTO {
    private Long id;
    private String nome;
    private String username; // 🔥 AGGIUNGI - Email dell'utente
    private String genere; // 🔥 AGGIUNGI - Genere
    private LocalDate dataNascita; // 🔥 AGGIUNGI - Data nascita
    private String bio;
    private String interessi;
    private String fotoProfilo;
    private String citta;
    private Integer eta;
    private Boolean notificheAttive; // 🔥 AGGIUNGI - Notifiche

    // Costruttore aggiornato
    public UtenteDiscoverDTO(Long id, String nome, String username, String genere, 
                           LocalDate dataNascita, String bio, String interessi,
                           String fotoProfilo, String citta, Integer eta, Boolean notificheAttive) {
        this.id = id;
        this.nome = nome;
        this.username = username; // 🔥 AGGIUNGI
        this.genere = genere; // 🔥 AGGIUNGI  
        this.dataNascita = dataNascita; // 🔥 AGGIUNGI
        this.bio = bio;
        this.interessi = interessi;
        this.fotoProfilo = fotoProfilo;
        this.citta = citta;
        this.eta = eta;
        this.notificheAttive = notificheAttive; // 🔥 AGGIUNGI
    }
    
    // ========== GETTER E SETTER ==========
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getInteressi() {
        return interessi;
    }
    
    public void setInteressi(String interessi) {
        this.interessi = interessi;
    }
    
    public String getFotoProfilo() {
        return fotoProfilo;
    }
    
    public void setFotoProfilo(String fotoProfilo) {
        this.fotoProfilo = fotoProfilo;
    }
    
    public String getCitta() {
        return citta;
    }
    
    public void setCitta(String citta) {
        this.citta = citta;
    }
    
    public Integer getEta() {
        return eta;
    }
    
    public void setEta(Integer eta) {
        this.eta = eta;
    }
    
    public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getGenere() {
		return genere;
	}

	public void setGenere(String genere) {
		this.genere = genere;
	}

	public LocalDate getDataNascita() {
		return dataNascita;
	}

	public void setDataNascita(LocalDate dataNascita) {
		this.dataNascita = dataNascita;
	}

	public Boolean getNotificheAttive() {
		return notificheAttive;
	}

	public void setNotificheAttive(Boolean notificheAttive) {
		this.notificheAttive = notificheAttive;
	}

	@Override
    public String toString() {
        return "UtenteDiscoverDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", bio='" + bio + '\'' +
                ", citta='" + citta + '\'' +
                ", eta=" + eta +
                '}';
    }
}