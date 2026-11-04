package ru.styk.auth;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain chain(HttpSecurity http,AuthService auth) throws Exception {
        return http.csrf(c->c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .addFilterBefore(new AccountStatusFilter(auth),org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .securityContext(c->c.securityContextRepository(contextRepository()))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/api/catalog/**","/api/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/warehouse/**").hasAnyRole("ADMIN","WAREHOUSE")
                .requestMatchers("/api/seller/**").hasAnyRole("ADMIN","SELLER")
                .anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Войдите в аккаунт\"}");})
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Недостаточно прав или устарела сессия. Обновите страницу.\"}");}))
            .logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,authentication)->res.setStatus(204)))
            .build();
    }
}
