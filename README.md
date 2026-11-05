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

Для рекомендуемого запуска нужны Docker Desktop с Docker Compose. 
Maven и JDK отдельно устанавливать необязательно: они используются внутри Dockerfile. 
Для запуска тестов вне контейнера нужен JDK 17+ и Maven Wrapper.

## Запуск через Docker Compose

```bash
git clone https://github.com/solusvulgaris/binance-price-tracker.git
cd binance-price-tracker
```

Запустите всю систему одной командой:

```bash
docker compose up --build -d
```

Compose запускает PostgreSQL 14 и Spring Boot приложение. 
Приложение зависит от healthcheck PostgreSQL и запускается после того, как база становится готова.

Проверьте состояние контейнеров:

```bash
docker compose ps
```

Посмотрите логи приложения:

```bash
docker compose logs -f app
```

После запуска API доступно по адресу `http://localhost:8080`. 
Например:

```bash
curl http://localhost:8080/api/v1/prices/latest
```

Flyway автоматически применяет миграции при старте приложения.

Остановить контейнеры и сеть, сохранив данные PostgreSQL в именованном volume, можно командой:

```bash
docker compose down
```

Команда ниже также удаляет volume с данными базы:

```bash
docker compose down -v
```

Используйте `down -v` только если сохранённые данные PostgreSQL больше не нужны.

Для запуска приложения напрямую через Maven Wrapper:

Запуск приложения:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

При прямом запуске PostgreSQL всё равно должен быть доступен на `localhost:5432`. 
Значения подключения по умолчанию совпадают с конфигурацией PostgreSQL из Compose.

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `BINANCE_SYMBOL` | `BNBUSDT` | Символ для планировщика и REST по умолчанию. |
| `BINANCE_POLL_INTERVAL` | `PT30S` | Интервал планировщика в ISO-8601 Duration (`PT30S` — 30 секунд). |
| `DB_URL` | `jdbc:postgresql://localhost:5432/binance_prices` | JDBC URL PostgreSQL. В Compose переопределяется на `jdbc:postgresql://postgres:5432/binance_prices`. |
| `DB_USERNAME` | `postgres` | Пользователь базы. |
| `DB_PASSWORD` | `postgres` | Пароль базы. |

В Docker Compose приложение подключается к PostgreSQL по имени сервиса `postgres`, а не через `localhost`, поскольку оба контейнера находятся в одной Compose-сети.

## Файловое логирование

Spring Boot продолжает выводить логи в консоль и дополнительно записывает их в файл `logs/binance-price-tracker.log` относительно рабочей директории приложения. В Compose каталог `./logs` на хосте монтируется в `/app/logs` контейнера, поэтому файл доступен на хостовой машине. Каталог `logs/` исключён из Git через `.gitignore`.

В PowerShell файл логов можно просматривать так:

```powershell
Get-Content .\logs\binance-price-tracker.log -Wait
```

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
curl http://localhost:8080/api/v1/prices/latest
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

## Postman Collection

В проект входит Postman-коллекция с запросами для всех четырёх REST endpoint:

`postman/binance-price-tracker.postman_collection.json`

Чтобы использовать коллекцию:

1. Откройте Postman.
2. Выберите **Import**.
3. Укажите JSON-файл коллекции.
4. Задайте переменные коллекции `from` и `to`.
5. Отправляйте запросы из коллекции.

Коллекция содержит запросы:

- `GET /api/v1/prices/latest`;
- `GET /api/v1/prices/average`;
- `GET /api/v1/prices`;
- `GET /api/v1/prices/trend`.

Значения `from` и `to` передаются в формате ISO-8601 с часовым поясом, например `2026-11-01T00:00:00Z`.

## Тестирование

```bash
# Linux/macOS
./mvnw clean verify

# Windows
.\mvnw.cmd clean verify
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
