package com.ridex.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ridex.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.revoked = true
                WHERE r.id = :id
                  AND r.revoked = false
            """)
    int revokeIfActive(@Param("id") UUID id);

    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.replacedBy = :newTokenId
                WHERE r.id = :oldTokenId
            """)
    int setReplacedBy(
            @Param("oldTokenId") UUID oldTokenId,
            @Param("newTokenId") UUID newTokenId);

    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.revoked = true
                WHERE r.familyId = :familyId
                  AND r.revoked = false
            """)
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.revoked = true
                WHERE r.userId = :userId
                  AND r.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") UUID userId);
}