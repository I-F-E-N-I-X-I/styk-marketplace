package ru.styk.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import ru.styk.common.Store;
import ru.styk.domain.Models.Account;
import java.util.Locale;

@Service
@Transactional
public class AuthService {
    private final Store db; private final PasswordEncoder encoder;
    public AuthService(Store db, PasswordEncoder encoder) { this.db=db;this.encoder=encoder; }
    public Account current() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Войдите в аккаунт");
        return byEmail(auth.getName());
    }
    public Account byEmail(String email) {
        return db.query(Account.class,"from Account where email=?1 and enabled=true",email.toLowerCase(Locale.ROOT)).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Аккаунт недоступен"));
    }
    public Account login(String email,String password) {
        Account user=byEmail(email);
        if(!encoder.matches(password,user.password)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Неверная почта или пароль");
        return user;
    }
    public Account register(String email,String name,String password) {
        Account a=new Account();a.email=email.toLowerCase(Locale.ROOT).trim();a.name=name.trim();a.password=encoder.encode(password);a.role="BUYER";
        return db.add(a);
    }
}
