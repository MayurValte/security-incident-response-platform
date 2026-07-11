package com.sirp.security.autoconfigure;

import com.sirp.security.jwt.JwtKeyProvider;
import com.sirp.security.jwt.JwtTokenParser;
import com.sirp.security.jwt.JwtTokenValidator;
import com.sirp.security.jwt.JwtValidationService;
import com.sirp.security.properties.JwtProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * NEW - this is what replaces the old com.sirp.auth.config.JwtConfiguration
 * + @ComponentScan(basePackages = {"...", "com.sirp.security"}) approach
 * that caused three separate restart failures (JwtAuthenticationFilter
 * collision, handler collision, JwtService collision).
 *
 * Registered automatically for EVERY consuming service via
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * - no @ComponentScan, no @Import, no manual wiring required anywhere.
 * Because these beans are defined by explicit @Bean methods (not by
 * scanning @Component-annotated classes), a consuming service's own
 * @ComponentScan of its own base package can NEVER collide with these,
 * regardless of what the service names its own classes.
 *
 * @ConditionalOnMissingBean on each @Bean lets a service override any
 * piece if it ever needs to (e.g. a custom JwtKeyProvider for testing)
 * simply by defining its own bean of that type.
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SirpSecurityCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtKeyProvider jwtKeyProvider(JwtProperties properties) {
        return new JwtKeyProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenParser jwtTokenParser(JwtKeyProvider jwtKeyProvider) {
        return new JwtTokenParser(jwtKeyProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenValidator jwtTokenValidator(JwtTokenParser jwtTokenParser) {
        return new JwtTokenValidator(jwtTokenParser);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtValidationService jwtValidationService(JwtTokenValidator jwtTokenValidator) {
        return new JwtValidationService(jwtTokenValidator);
    }
}
