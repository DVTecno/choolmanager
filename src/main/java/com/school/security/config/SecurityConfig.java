package com.school.security.config;

import com.school.confing.filter.JwtTokenValidator;
import com.school.exception.handler.CustomAccessDeniedHandler;
import com.school.utility.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
    private final JwtUtils jwtUtils;

    public SecurityConfig(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(http -> {
                    // Rutas accesibles sin autenticación
                    http.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**").permitAll();
                    http.requestMatchers(HttpMethod.GET, "/api/v1/status").permitAll();
                    http.requestMatchers(HttpMethod.GET, "/api/reset_password").permitAll();
                    http.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    http.requestMatchers(HttpMethod.POST, "/admin/**").permitAll();
                    http.requestMatchers(HttpMethod.POST, "/auth/refresh-token").permitAll();
                    http.requestMatchers(HttpMethod.POST, "/api/forgot-password", "/api/reset_password").permitAll();

                    http.requestMatchers(Arrays.toString(HttpMethod.values()), "/api/v1/subject/**").permitAll();
                    http.requestMatchers(Arrays.toString(HttpMethod.values()), "/api/v1/course/**").permitAll();
                    http.requestMatchers(Arrays.toString(HttpMethod.values()), "/api/v1/course-student/**").permitAll();



                    // Rutas accesibles solo con roles específicos
                    http.requestMatchers(HttpMethod.GET, "/teacher/verifyStudent/**").hasAuthority("READ_PRIVILEGES");
                    http.requestMatchers(HttpMethod.GET, "/admin/**").hasRole("ADMIN");
                    http.requestMatchers(HttpMethod.GET, "/api/v1/status/auth-student").hasAnyRole("STUDENT");
                    http.requestMatchers(HttpMethod.GET, "/api/v1/status/auth-teacher", "/notifications/verify/{dni}").hasAnyRole("TEACHER");
                    http.requestMatchers(HttpMethod.GET, "/api/v1/status/auth-parent").hasAnyRole("PARENT");

                    // Permisos para evaluaciones
                    http.requestMatchers(HttpMethod.POST, "/admin/**").hasRole("ADMIN");
                    http.requestMatchers(HttpMethod.POST, "/evaluations/load").hasRole("TEACHER");
                    http.requestMatchers(HttpMethod.POST, "/evaluations/student").hasAnyRole("STUDENT", "PARENT");

                    // Permisos para notificaciones
                    http.requestMatchers(HttpMethod.POST, "/notifications/send/all").hasRole("TEACHER");
                    http.requestMatchers(HttpMethod.POST, "/notifications/send/student").hasRole("TEACHER");
                    http.requestMatchers(HttpMethod.POST, "/notifications/send/parent").hasRole("TEACHER");
                    http.requestMatchers(HttpMethod.GET, "/notifications/course-notifications").hasAnyRole("STUDENT", "PARENT", "TEACHER");
                    http.requestMatchers(HttpMethod.GET, "/notifications/student/{dni}").hasRole("STUDENT");
                    http.requestMatchers(HttpMethod.GET, "/notifications/parent/{dni}", "/evaluations/students/parent/{dni}").hasRole("PARENT");

                    // Denegar todas las demás solicitudes
                    http.anyRequest().denyAll();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtTokenValidator(jwtUtils), BasicAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(new CustomAccessDeniedHandler())
                                .authenticationEntryPoint((request, response, authException) -> {
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Authentication failed.");
                                })
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "https://schoolmanager-nine.vercel.app",
                "https://schoolmanager-nzlyngd5b-dvtecnos-projects.vercel.app",
                "https://choolmanager-production.up.railway.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}