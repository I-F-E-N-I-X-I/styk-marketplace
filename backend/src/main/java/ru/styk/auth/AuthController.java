package ru.styk.auth;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import ru.styk.domain.Models.Account;
import java.util.*;

@RestController @RequestMapping("/api")
public class AuthController {
    private final AuthService auth; private final SecurityContextRepository contexts;
    public AuthController(AuthService auth,SecurityContextRepository contexts){this.auth=auth;this.contexts=contexts;}
    public record Credentials(@Email @NotBlank String email,@NotBlank @Size(max=72) String password){}
    public record Registration(@Email @NotBlank @Size(max=160) String email,@NotBlank @Size(max=100) String name,@Size(min=10,max=72) @NotNull String password){}
    @GetMapping("/health") Map<String,String> health(){return Map.of("status","ok");}
    @GetMapping("/auth/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"header",token.getHeaderName());}
    @GetMapping("/auth/me") Account me(){return auth.current();}
    @PostMapping("/auth/register") Account register(@Valid @RequestBody Registration r,HttpServletRequest req,HttpServletResponse res){return establish(auth.register(r.email,r.name,r.password),req,res);}
    @PostMapping("/auth/login") Account login(@Valid @RequestBody Credentials r,HttpServletRequest req,HttpServletResponse res){return establish(auth.login(r.email,r.password),req,res);}
    private Account establish(Account a,HttpServletRequest req,HttpServletResponse res){
        req.getSession();req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(a.email,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role))));
        SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);return a;
    }
}
