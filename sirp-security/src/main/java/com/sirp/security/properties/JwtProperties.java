package com.sirp.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound automatically by SirpSecurityCoreAutoConfiguration - no
 * @EnableConfigurationProperties needed in any consuming service.
 *
 * publicKey  - Base64 DER (X.509) encoded RSA public key. Required on
 *              EVERY service (Auth Service, Gateway, downstream services).
 * privateKey - Base64 DER (PKCS8) encoded RSA private key. Required ONLY
 *              on the Auth Service. Leave unset everywhere else - only
 *              the service that signs tokens should ever hold it.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String publicKey;

    private String privateKey;

    private Long expiration;

    private Long refreshExpiration;
}
