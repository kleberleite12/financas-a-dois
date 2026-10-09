package io.github.kleberleite12.financas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(autorizacao -> autorizacao

                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/icons/**",
                                "/manifest.webmanifest",
                                "/service-worker.js"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.kleber.password}") String senhaKleber,
            @Value("${app.giovanna.password}") String senhaGiovanna) {

        UserDetails kleber =
                User.builder()
                        .username("kleber")
                        .password(
                                passwordEncoder.encode(
                                        senhaKleber
                                )
                        )
                        .roles("USER")
                        .build();


        UserDetails giovanna =
                User.builder()
                        .username("giovanna")
                        .password(
                                passwordEncoder.encode(
                                        senhaGiovanna
                                )
                        )
                        .roles("USER")
                        .build();


        return new InMemoryUserDetailsManager(
                kleber,
                giovanna
        );
    }
}