package ru.styk;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.styk.auth.AuthService;
import ru.styk.common.Store;
import ru.styk.domain.Models.*;
import ru.styk.order.OrderService;
import ru.styk.supply.SupplyService;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:styk-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","styk.seed=true","styk.demo-password=StykDemo2026!","styk.replenishment-delay=3600000"})
@AutoConfigureMockMvc
class MarketplaceTest {
 @Autowired OrderService orders; @Autowired SupplyService supplies; @Autowired AuthService auth;
 @Autowired Store db; @Autowired JdbcTemplate jdbc; @Autowired PlatformTransactionManager manager; @Autowired MockMvc mvc;
 @BeforeEach void reset(){
  for(String table:List.of("order_events","order_items","orders","cart_items","supply_requests","reviews","favorites","stock_movements"))jdbc.update("delete from "+table);
  jdbc.update("update products set stock=20,reserved=0,threshold=10,target=40,status='ACTIVE'");
  jdbc.update("update users set enabled=true");login("buyer@styk.local","BUYER");
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 static void login(String email,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
 long product(){return jdbc.queryForObject("select min(id) from products",Long.class);}
 int stock(String field){return jdbc.queryForObject("select "+field+" from products where id=?",Integer.class,product());}
 OrderService.OrderView purchase(int n,String key){orders.setCart(product(),n);return orders.checkout("Екатеринбург, Ленина, 10","COURIER","",key);}
 @Test void reserveCancelAndIdempotency(){var o=purchase(3,"repeat");assertEquals(3,stock("reserved"));assertEquals(20,stock("stock"));assertEquals(o.order().id,orders.checkout("другой адрес","PICKUP","","repeat").order().id);orders.transition(o.order().id,"CANCELLED",false);orders.transition(o.order().id,"CANCELLED",false);assertEquals(0,stock("reserved"));assertEquals(1,jdbc.queryForObject("select count(*) from stock_movements where type='RELEASE'",Integer.class));}
 @Test void shipmentReturnAndRepeatedReceipt(){var o=purchase(2,"ship");login("warehouse@styk.local","WAREHOUSE");for(String s:List.of("PICKING","PACKED","SHIPPED","DELIVERED"))orders.transition(o.order().id,s,true);assertEquals(18,stock("stock"));assertEquals(0,stock("reserved"));login("buyer@styk.local","BUYER");orders.transition(o.order().id,"RETURN_REQUESTED",false);login("warehouse@styk.local","WAREHOUSE");orders.transition(o.order().id,"RETURNED",true);orders.transition(o.order().id,"RETURNED",true);assertEquals(20,stock("stock"));}
 @Test void failedCheckoutRollsBack(){orders.setCart(product(),5);jdbc.update("update products set stock=2 where id=?",product());assertThrows(RuntimeException.class,()->orders.checkout("Екатеринбург, Ленина, 10","COURIER","","fail"));assertEquals(0,stock("reserved"));assertEquals(0,jdbc.queryForObject("select count(*) from orders",Integer.class));assertEquals(1,orders.cart().size());}
 @Test void replenishCountsIncoming(){jdbc.update("update products set stock=7 where id=?",product());assertEquals(1,supplies.replenish());assertEquals(0,supplies.replenish());assertEquals(33,jdbc.queryForObject("select quantity from supply_requests",Integer.class));}
 @Test void receiptHandlesDiscrepancyAndIsIdempotent(){jdbc.update("update products set stock=7 where id=?",product());supplies.replenish();long id=jdbc.queryForObject("select id from supply_requests",Long.class);login("seller@styk.local","SELLER");supplies.transition(id,"CONFIRMED",0,"",false);supplies.transition(id,"IN_TRANSIT",0,"",false);login("warehouse@styk.local","WAREHOUSE");assertThrows(RuntimeException.class,()->supplies.transition(id,"RECEIVED",30,"",true));supplies.transition(id,"RECEIVED",30,"3 повреждены",true);supplies.transition(id,"RECEIVED",30,"3 повреждены",true);assertEquals(37,stock("stock"));assertEquals(1,jdbc.queryForObject("select count(*) from stock_movements where type='RECEIVE'",Integer.class));}
 @Test void foreignSellerCannotChangeSupply(){jdbc.update("update products set stock=7 where id=?",product());supplies.replenish();long id=jdbc.queryForObject("select id from supply_requests",Long.class);login("studio@styk.local","SELLER");assertThrows(RuntimeException.class,()->supplies.transition(id,"CONFIRMED",0,"",false));}
 @Test void customerCannotSkipFulfillment(){var o=purchase(1,"skip");assertThrows(RuntimeException.class,()->orders.transition(o.order().id,"DELIVERED",false));}
 @Test void lastItemOnlySoldOnce()throws Exception{
  jdbc.update("update products set stock=1 where id=?",product());orders.setCart(product(),1);
  var second=auth.register("concurrent-"+UUID.randomUUID()+"@test.local","Другой покупатель","Password123!");login(second.email,"BUYER");orders.setCart(product(),1);
  var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try{List<Future<Boolean>> results=new ArrayList<>();for(String email:List.of("buyer@styk.local",second.email))results.add(pool.submit(()->{login(email,"BUYER");gate.await();try{orders.checkout("Екатеринбург, Ленина, 10","COURIER","",UUID.randomUUID().toString());return true;}catch(RuntimeException e){return false;}finally{SecurityContextHolder.clearContext();}}));gate.countDown();int won=0;for(var f:results)if(f.get(15,TimeUnit.SECONDS))won++;assertEquals(1,won);assertEquals(1,stock("reserved"));}finally{pool.shutdownNow();}
 }
 @Test void anonymousAndBuyerCannotAccessAdmin()throws Exception{mvc.perform(get("/api/admin/users").with(anonymous())).andExpect(status().isUnauthorized());mvc.perform(get("/api/admin/users").with(user("buyer@styk.local").roles("BUYER"))).andExpect(status().isForbidden());}
 @Test void csrfRequiredAndCatalogPublic()throws Exception{mvc.perform(get("/api/catalog/products")).andExpect(status().isOk());mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"buyer@styk.local\",\"password\":\"StykDemo2026!\"}")).andExpect(status().isForbidden());}
 @Test void disabledAccountLosesAccess()throws Exception{jdbc.update("update users set enabled=false where email='buyer@styk.local'");mvc.perform(get("/api/cart").with(user("buyer@styk.local").roles("BUYER"))).andExpect(status().isUnauthorized());}
 @Test void roleRevocationAppliesToExistingSession()throws Exception{mvc.perform(get("/api/admin/users").with(user("buyer@styk.local").roles("ADMIN"))).andExpect(status().isForbidden());}
}

