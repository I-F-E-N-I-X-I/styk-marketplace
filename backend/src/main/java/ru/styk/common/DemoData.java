package ru.styk.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.styk.domain.Models.*;
import java.math.BigDecimal;

@Component
public class DemoData implements CommandLineRunner {
    private final Store db;private final PasswordEncoder encoder;
    @Value("${styk.seed}") private boolean enabled;
    @Value("${styk.demo-password}") private String password;
    public DemoData(Store db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
    @Override @Transactional public void run(String... args){
        if(!enabled||!db.query(Account.class,"from Account").isEmpty())return;
        if(password.length()<10)throw new IllegalStateException("Set DEMO_PASSWORD to at least 10 characters");
        account("buyer@styk.local","Александр","BUYER");
        Account seller=account("seller@styk.local","Бюро предметов","SELLER");
        account("warehouse@styk.local","Склад STYK","WAREHOUSE");account("admin@styk.local","Администратор","ADMIN");
        Account other=account("studio@styk.local","Студия Формы","SELLER");
        product(seller,"Тишина в деталях","Наушники Forma 02","FORMA","Техника",7490,9990,"headphones","#e4e7dd",7,"Беспроводные наушники с мягкими амбушюрами, активным шумоподавлением и автономностью до 40 часов. Bluetooth 5.3, зарядка USB-C. Цвет: шалфей.");
        product(other,"Свет для своих","Лампа Orbit Mini","ORBIT","Дом",4290,0,"lamp","#eee2d2",22,"Настольная лампа с тёплым рассеянным светом. Металлический корпус, три режима яркости, высота 28 см. Для вечеров без спешки.");
        product(other,"Маленький ритуал","Керамическая чашка №04","ТИХО","Дом",1290,1690,"cup","#e4d8c8",31,"Чашка ручной работы из каменной керамики. Объём 300 мл, матовая глазурь. Можно мыть в посудомоечной машине.");
        product(seller,"Всё по клавишам","Клавиатура Grid 75","GRID","Техника",8990,10990,"keyboard","#d8dfda",6,"Механическая клавиатура формата 75%. Тактильные переключатели, Bluetooth и USB-C, двойные PBT-кейкапы. Русская и английская раскладки.");
        product(other,"Город внутри","Рюкзак Route Day","ROUTE","Аксессуары",5690,0,"bag","#e4ded0",18,"Рюкзак на 18 литров из плотного водоотталкивающего текстиля. Мягкий карман для ноутбука до 15 дюймов, скрытый карман на спинке.");
        product(seller,"Музыка рядом","Колонка Pebble","FORMA","Техника",3490,4490,"speaker","#e0dedb",26,"Портативная колонка 12 Вт с объёмным звуком. Защита IPX5, до 12 часов работы. Компактный корпус с тканевой отделкой.");
        product(other,"Пауза на бумаге","Блокнот Field Notes","ПОЛЕ","Канцелярия",790,0,"notebook","#e5e1d2",42,"Блокнот A5, 160 страниц кремовой бумаги 100 г/м² в точку. Тканевая обложка и раскрытие на 180 градусов.");
        product(seller,"Держит температуру","Термобутылка Flow","FLOW","Аксессуары",2190,2790,"bottle","#dce4da",9,"Стальная бутылка объёмом 500 мл. Сохраняет тепло до 12 часов и холод до 24. Герметичная крышка, приятное матовое покрытие.");
        product(other,"Место для мелочей","Поднос Arc","ТИХО","Дом",1890,0,"tray","#e6d7cb",14,"Поднос из минерального композита с мягкой фактурой. Размер 24 × 14 см. Для украшений, ключей и маленьких ежедневных ритуалов.");
        product(seller,"Чёткий фокус","Мышь Point Wireless","GRID","Техника",2990,0,"mouse","#dddeda",24,"Беспроводная мышь с тихими кнопками и точным сенсором. Подключение к трём устройствам, USB-C, удобная симметричная форма.");
        product(other,"Мягкая геометрия","Плед Line","НИТЬ","Дом",4590,5990,"blanket","#dfdbd0",12,"Плед из хлопка и шерсти с графичным узором. Размер 140 × 200 см. Мягкий, лёгкий и уютный.");
        product(other,"Собрать мысли","Набор карандашей","ПОЛЕ","Канцелярия",490,0,"pencils","#e6dace",50,"Шесть графитных карандашей HB из кедра в картонном футляре. Для заметок, эскизов и больших идей.");
    }
    private Account account(String email,String name,String role){Account a=new Account();a.email=email;a.name=name;a.role=role;a.password=encoder.encode(password);return db.add(a);}
    private void product(Account seller,String tagline,String name,String brand,String category,int price,int old,String image,String color,int stock,String description){
        Product p=new Product();p.sellerId=seller.id;p.name=name;p.brand=brand;p.category=category;p.price=BigDecimal.valueOf(price);p.oldPrice=old>0?BigDecimal.valueOf(old):null;p.image="/assets/"+image+".svg";p.color=color;p.stock=stock;p.sku="ST-"+image.toUpperCase();p.status="ACTIVE";p.description=description;p.location="A-"+String.format("%02d",stock%8+1);db.add(p);
        Movement m=new Movement();m.productId=p.id;m.type="OPENING";m.quantity=stock;m.reference="INITIAL";db.add(m);
    }
}
