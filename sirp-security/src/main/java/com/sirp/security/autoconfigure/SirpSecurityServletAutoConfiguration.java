package com.sirp.security.autoconfigure;

import com.sirp.security.filter.CorrelationIdFilter;
import com.sirp.security.filter.JwtAuthenticationFilter;
import com.sirp.security.handler.RestAccessDeniedHandler;
import com.sirp.security.handler.RestAuthenticationEntryPoint;
import com.sirp.security.jwt.JwtValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * NEW - provides JwtAuthenticationFilter and the two REST handlers ONLY
 * to servlet-based applications (Auth Service, downstream microservices).
 *
 * @ConditionalOnClass(HttpServletRequest.class): Spring evaluates this
 * via bytecode inspection without ever loading HttpServletRequest, so it
 * safely no-ops on a classpath that doesn't have jakarta.servlet-api at
 * all (this is what makes the "provided" scope on that dependency safe).
 *
 * @ConditionalOnWebApplication(type = SERVLET): belt-and-braces - even if
 * servlet-api somehow ended up on a reactive app's classpath, this still
 * won't activate for WebFlux/Netty apps like the API Gateway.
 *
 * Net effect: the Gateway's NoClassDefFoundError from before can no
 * longer happen, structurally - there's nothing to manually exclude
 * anymore, unlike the earlier @ComponentScan(excludeFilters = ...)
 * workaround.
 */
@AutoConfiguration
@ConditionalOnClass(HttpServletRequest.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureAfter(SirpSecurityCoreAutoConfiguration.class)
public class SirpSecurityServletAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtValidationService jwtValidationService) {
        return new JwtAuthenticationFilter(jwtValidationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }
}
