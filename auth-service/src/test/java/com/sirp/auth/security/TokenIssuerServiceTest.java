package com.sirp.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sirp.security.constants.JwtClaimConstants;
import com.sirp.security.jwt.JwtKeyProvider;
import com.sirp.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenIssuerServiceTest {

    @Mock
    private JwtKeyProvider keyProvider;

    private JwtProperties properties;
    private TokenIssuerService tokenIssuerService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        properties = new JwtProperties();
        properties.setExpiration(900000L);

        tokenIssuerService = new TokenIssuerService(properties, keyProvider);
    }

    @Test
    void generatesTokenSignedWithPrivateKeyCarryingExpectedClaims() {
        when(keyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());
        UUID userId = UUID.randomUUID();
        long before = System.currentTimeMillis();

        String token = tokenIssuerService.generateAccessToken(userId, "jdoe@sirp.local", "ENGINEER");

        Claims claims = Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo("jdoe@sirp.local");
        assertThat(claims.get(JwtClaimConstants.USER_ID, String.class)).isEqualTo(userId.toString());
        assertThat(claims.get(JwtClaimConstants.ROLE, String.class)).isEqualTo("ENGINEER");
        assertThat(claims.getIssuer()).isEqualTo(JwtClaimConstants.ISSUER);
        assertThat(claims.getAudience()).containsExactly(JwtClaimConstants.AUDIENCE);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(new Date(before + 900000L - 5000L));
    }

    @Test
    void generatesADifferentJwtIdOnEachCall() {
        when(keyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());
        UUID userId = UUID.randomUUID();

        String first = tokenIssuerService.generateAccessToken(userId, "jdoe@sirp.local", "ENGINEER");
        String second = tokenIssuerService.generateAccessToken(userId, "jdoe@sirp.local", "ENGINEER");

        assertThat(first).isNotEqualTo(second);
    }
}
