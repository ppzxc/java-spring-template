package io.github.ppzxc.boilerplate.boot.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtRequestContextFilter ctxFilter)
      throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/identity/users")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterAfter(ctxFilter, BasicAuthenticationFilter.class)
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(new ProblemDetailAuthEntryPoint())
                    .accessDeniedHandler(new ProblemDetailAccessDeniedHandler()))
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${security.jwt.jwk-set-uri:}") String jwkSetUri,
      @Value("${security.jwt.hmac-secret:}") String hmacSecret) {
    if (!jwkSetUri.isBlank()) {
      return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
    var key = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}
