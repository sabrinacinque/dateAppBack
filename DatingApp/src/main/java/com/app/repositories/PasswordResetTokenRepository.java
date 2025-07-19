package com.app.repositories;

import com.app.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * 🔍 Trova token per valore
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * 🔍 Trova token attivo per email (non usato)
     */
    Optional<PasswordResetToken> findByEmailAndUsedFalse(String email);
    
    /**
     * 🔍 Trova token valido per email (non usato e non scaduto)
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.email = :email AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findValidTokenByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    /**
     * 🗑️ Elimina token scaduti (pulizia automatica)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 🔄 Marca token come usato
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token")
    void markTokenAsUsed(@Param("token") String token);
    
    /**
     * 🚫 Invalida tutti i token di un utente
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.used = false")
    void invalidateAllTokensForEmail(@Param("email") String email);
    
    /**
     * 📊 Conta token attivi per email
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.email = :email AND t.used = false AND t.expiryDate > :now")
    long countActiveTokensForEmail(@Param("email") String email, @Param("now") LocalDateTime now);
}