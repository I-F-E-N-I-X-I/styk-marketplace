package ru.styk.common;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Repository
public class Store {
    @PersistenceContext private EntityManager em;
    public <T> T add(T entity) { em.persist(entity); return entity; }
    public void remove(Object entity) { em.remove(entity); }
    public <T> T get(Class<T> type, Long id) {
        T value = em.find(type, id);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена");
        return value;
    }
    public <T> T lock(Class<T> type, Long id) {
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена");
        em.refresh(value, LockModeType.PESSIMISTIC_WRITE);
        return value;
    }
    public <T> List<T> query(Class<T> type, String jpql, Object... params) {
        var query = em.createQuery(jpql, type);
        for (int i=0;i<params.length;i++) query.setParameter(i+1,params[i]);
        return query.getResultList();
    }
    public void flush() { em.flush(); }
}
