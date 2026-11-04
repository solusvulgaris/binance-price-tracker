# Binance Price Tracker

Сервис периодически получает среднюю цену торговой пары через Binance API, сохраняет историю в PostgreSQL и предоставляет данные через REST API. По умолчанию используется пара `BNBUSDT`.

## Технологии

- Java 17;
- Spring Boot 3.5.16;
- Maven и Maven Wrapper;
- Spring Data JPA;
- PostgreSQL 14;
- Flyway;
- Spring `RestClient`;
- JUnit 5, Mockito и MockMvc;
- Testcontainers 1.21.3 для интеграционного теста PostgreSQL.

## Требования

Для запуска нужны JDK 17+, Docker и Docker Compose. Maven устанавливать необязательно: в репозитории есть Maven Wrapper.

## Запуск

```bash
git clone https://github.com/solusvulgaris/binance-price-tracker.git
cd binance-price-tracker
docker compose up -d
```

Compose запускает PostgreSQL 14 на `localhost:5432` с базой `binance_prices`, пользователем `postgres` и паролем `postgres`.

Запуск приложения:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

Flyway автоматически применяет миграции из `src/main/resources/db/migration`. Стандартные параметры приложения совпадают с конфигурацией Compose.

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `BINANCE_SYMBOL` | `BNBUSDT` | Символ для планировщика и REST по умолчанию. |
| `BINANCE_POLL_INTERVAL` | `PT30S` | Интервал планировщика в ISO-8601 Duration (`PT30S` — 30 секунд). |
| `DB_URL` | `jdbc:postgresql://localhost:5432/binance_prices` | JDBC URL PostgreSQL. |
| `DB_USERNAME` | `postgres` | Пользователь базы. |
| `DB_PASSWORD` | `postgres` | Пароль базы. |

## Планировщик и получение данных

Планировщик вызывает endpoint `GET https://api.binance.com/api/v3/avgPrice`; торговая пара передаётся в query-параметре `symbol`, например `?symbol=BNBUSDT`. Клиент использует положительную `price` и сохраняет `symbol`, `price` и время получения `fetchedAt`. `fetchedAt` — время приложения в UTC (`OffsetDateTime.now(ZoneOffset.UTC)`), в БД используются `NUMERIC(20, 8)` и `TIMESTAMPTZ`.

Ошибки 429, 418, 5xx, соединения, тайм-ауты и некорректные ответы логируются планировщиком; следующий запуск продолжает работу, а ошибочная цена не сохраняется. `Retry-After` сохраняется клиентом, но планировщик пока не меняет расписание и не ожидает это значение. Автоматические повторы не реализованы.

## REST API

Базовый путь всех endpoint — `/api/v1/prices`. `symbol` необязателен; без него используется `BINANCE_SYMBOL` (по умолчанию `BNBUSDT`). Входные даты `from` и `to` принимаются как `OffsetDateTime` в формате ISO-8601 с часовым смещением, например `2026-11-01T00:00:00Z`. Поля `fetchedAt` в ответах возвращаются в UTC. Интервал включает границы, `from` не может быть позже `to`.

### Последняя цена

`GET /api/v1/prices/latest?symbol=BNBUSDT`

Успешный ответ — `200 OK`:

```json
{
  "symbol": "BNBUSDT",
  "price": 612.45000000,
  "fetchedAt": "2026-11-03T20:15:30Z"
}
```

`fetchedAt` сериализуется в UTC. При отсутствии записи — `404 Not Found` с текстом.

```bash
curl "http://localhost:8080/api/v1/prices/latest"
```

### Средняя цена

`GET /api/v1/prices/average?symbol=BNBUSDT&from=2026-11-01T00:00:00Z&to=2026-11-04T00:00:00Z`

`from` и `to` обязательны, `symbol` необязателен. Ответ содержит только `averagePrice`:

```json
{
  "averagePrice": 612.34000000
}
```

Пустой результат — `404`; неверные или отсутствующие параметры и неправильный интервал — `400`.

### История с пагинацией

`GET /api/v1/prices?symbol=BNBUSDT&from=2026-11-01T00:00:00Z&to=2026-11-04T00:00:00Z&page=0&size=20`

`page` по умолчанию `0`, `size` — `20`. Допустимы `page >= 0` и `size` от `1` до `100`. Сортировка: `fetchedAt ASC`, затем `id ASC`.

```json
{
  "items": [{
    "symbol": "BNBUSDT",
    "price": 600.25000000,
    "fetchedAt": "2026-11-01T10:30:00Z"
  }],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "hasNext": true
}
```

Пустая история возвращается как `200` с пустым `items`, `totalElements: 0` и `hasNext: false`. Ошибки параметров — `400`.

### Тренд

`GET /api/v1/prices/trend?symbol=BNBUSDT&from=2026-11-01T00:00:00Z&to=2026-11-04T00:00:00Z`

Тренд определяется сравнением первой и последней сохранённых цен за период, без анализа промежуточных колебаний. Возможные значения: `UPWARD`, `DOWNWARD`, `FLAT`. Даты — даты фактических записей в UTC.

```json
{
  "symbol": "BNBUSDT",
  "start_date": "2026-11-01",
  "end_date": "2026-11-04",
  "trend": "UPWARD"
}
```

Нет данных — `404`; некорректные параметры или интервал — `400`.

## Обработка ошибок

Контроллеры используют `200` для успешных ответов, `404` для отсутствующих данных и `400` для неверных/отсутствующих параметров, формата дат, интервала или пагинации. Ошибки возвращаются текстом; единого JSON-формата ошибок нет. Ошибки Binance обрабатываются планировщиком и не останавливают его последующие запуски.

## Тестирование

```bash
# Linux/macOS
./mvnw clean verify

# Windows
mvnw.cmd clean verify
```

Есть unit-тесты сервисов, клиента и планировщика, MVC-тесты MockMvc и интеграционный Spring Boot тест с Testcontainers PostgreSQL. Интеграционный тест запускает отдельный контейнер и не использует локальную базу проекта.

## Структура проекта

- `com.ak.binancepricetracker` — приложение и включение планировщика;
- `client` — DTO Binance, `RestClient` и ошибки внешнего API;
- `controller` — REST endpoint и DTO;
- `service` — валидация, сохранение, выборки, средняя цена и тренд;
- `repository` — Spring Data JPA запросы;
- `entity` — `PriceHistory`;
- `scheduler` — периодический опрос и сохранение;
- `src/main/resources/db/migration` — миграции Flyway.
