# REST API

Корень `/api`. Вход через сессию; cookie нужно передавать во всех запросах. Перед изменением данных вызовите `GET /auth/csrf`: ответ содержит `token` и имя `header`. Передавайте токен в указанном заголовке. После выхода получите новый токен.

Ошибки бизнес-логики: `409 {"message":"…"}`; валидация: 400; без входа: 401; недостаточно прав или неверный CSRF: 403; отсутствующая запись: 404.

| Метод | Маршрут | Назначение / доступ |
|---|---|---|
| GET | /health | Проверка API |
| GET | /auth/csrf | CSRF-токен |
| POST | /auth/register | `{email,name,password}` — регистрация покупателя |
| POST | /auth/login | `{email,password}` — вход |
| POST | /auth/logout | Выход |
| GET | /auth/me | Текущий пользователь |
| GET | /catalog/products | Активные товары |
| GET | /catalog/products/{id} | Товар |
| GET | /catalog/products/{id}/reviews | Отзывы |
| POST | /products/{id}/reviews | `{rating,text}` — после доставки |
| GET | /favorites | ID избранных товаров |
| PUT, DELETE | /favorites/{id} | Добавить / удалить |
| GET | /cart | Корзина аккаунта |
| PUT | /cart/{productId} | `{quantity}` от 0 до 99; ноль удаляет |
| POST | /orders | Оформление, пример ниже |
| GET | /orders | Заказы текущего пользователя |
| POST | /orders/{id}/status | `{status}` CANCELLED или RETURN_REQUESTED |
| GET, POST | /seller/products | Свои товары / создание |
| PUT | /seller/products/{id} | Редактирование с повторной модерацией |
| GET, POST | /seller/supplies | Свои поставки / `{productId,quantity}` |
| POST | /seller/supplies/{id}/status | `{status,received:0,note:""}` |
| GET | /warehouse/orders | Все заказы для обработки |
| POST | /warehouse/orders/{id}/status | `{status}` — следующий этап |
| GET | /warehouse/inventory | Остатки и резервы |
| PUT | /warehouse/inventory/{id} | `{threshold,target,location}` |
| GET | /warehouse/movements | Журнал движений |
| GET | /warehouse/supplies | Все поставки |
| POST | /warehouse/supplies/{id}/status | Приёмка `{status:"RECEIVED",received,note}` |
| POST | /warehouse/replenish | Запустить расчёт; ответ `{created}` |
| GET | /admin/stats | Оборот, заказы, товары, продажи |
| GET | /admin/products | Все карточки |
| POST | /admin/products/{id}/status | ACTIVE, PENDING или REJECTED |
| GET | /admin/users | Пользователи без паролей |
| PUT | /admin/users/{id} | `{role,enabled}`; свою роль менять нельзя |
| GET | /admin/audit | Журнал модерации и административных изменений |

### Оформление

```json
{
  "address": "Екатеринбург, тестовый адрес 10",
  "delivery": "PICKUP",
  "promo": "STYK10",
  "requestKey": "уникальный-ключ-на-одно-оформление"
}
```

`delivery`: COURIER или PICKUP. `promo`: пустая строка или STYK10. Оплата отмечается DEMO_PAID, реальные реквизиты не запрашиваются. Ответ: `{order,items,events}`. Повтор с прежним ключом возвращает прежний заказ независимо от нового содержимого корзины.

### Создание товара

```json
{
  "name": "Чашка новая",
  "brand": "ТИХО",
  "category": "Дом",
  "description": "Керамическая чашка, 300 мл.",
  "price": 1290,
  "image": "/assets/cup.svg",
  "color": "#e4e7dd",
  "sku": "CUP-NEW-001"
}
```

### Переходы

Заказ: RESERVED → PICKING → PACKED → SHIPPED → DELIVERED → RETURN_REQUESTED → RETURNED. Отмена возможна до отгрузки. Покупатель не может выполнять складские переходы.

Поставка: REQUESTED → CONFIRMED → IN_TRANSIT → RECEIVED. До отправки можно отменить. При недостаче обязателен комментарий; принятое количество должно быть от 0 до заявленного.
