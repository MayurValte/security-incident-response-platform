package com.sirp.security.jwt;

import com.sirp.security.properties.JwtProperties;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * NEW - central to the HS256 -> RS256 migration. Loads the RSA key
 * material once at startup from jwt.public-key / jwt.private-key
 * (Base64, PEM headers optional - stripped automatically if present).
 *
 * Deliberately NOT a Spring bean created via @Component - it is wired
 * explicitly by SirpSecurityCoreAutoConfiguration's @Bean method. This
 * keeps the shared library free of component-scanned classes entirely,
 * which is what makes it safe from bean-name collisions no matter what
 * a consuming service names its own classes.
 *
 * getPrivateKey() throws if no private key was configured - by design,
 * so a downstream service or the Gateway gets a clear, immediate error
 * if it's accidentally used to try to SIGN a token instead of just
 * verifying one.
 */
public class JwtKeyProvider {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public JwtKeyProvider(JwtProperties properties) {
        this.publicKey = buildPublicKey(properties.getPublicKey());
        this.privateKey = isBlank(properties.getPrivateKey())
            ? null
            : buildPrivateKey(properties.getPrivateKey());
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException(
                "No RSA private key configured (jwt.private-key) on this service. "
                    + "Only the Auth Service should hold a private key and sign tokens.");
        }
        return privateKey;
    }

    private PublicKey buildPublicKey(String base64Key) {
        if (isBlank(base64Key)) {
            throw new IllegalStateException("jwt.public-key is not configured.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(strip(base64Key));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to load RSA public key from jwt.public-key", ex);
        }
    }

    private PrivateKey buildPrivateKey(String base64Key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(strip(base64Key));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to load RSA private key from jwt.private-key", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String strip(String key) {
        return key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    }
}
