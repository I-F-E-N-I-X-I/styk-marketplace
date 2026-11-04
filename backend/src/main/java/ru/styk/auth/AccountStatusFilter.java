package ru.styk.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.IOException;
import java.util.List;

/** Re-read enabled flag and role on every request so existing sessions cannot retain revoked access. */
public class AccountStatusFilter extends OncePerRequestFilter {
    private final AuthService auth;
    public AccountStatusFilter(AuthService auth){this.auth=auth;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        var current=SecurityContextHolder.getContext().getAuthentication();
        if(current!=null&&current.isAuthenticated()&&!current.getName().equals("anonymousUser")){
            try{var a=auth.byEmail(current.getName());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(a.email,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role))));}
            catch(org.springframework.web.server.ResponseStatusException e){SecurityContextHolder.clearContext();var s=req.getSession(false);if(s!=null)s.invalidate();}
        }
        chain.doFilter(req,res);
    }
}
