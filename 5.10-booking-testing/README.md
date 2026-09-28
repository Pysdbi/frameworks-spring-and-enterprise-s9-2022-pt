# 5.10 — Тесты для BookingTesting (JUnit 5, AssertJ, Mockito)

Упрощённая система бронирования номеров в отеле (`com.skillbox.hotel`) и тесты для неё.

## Что сделано

- Подключены JUnit 5, AssertJ и Mockito, отчёт о покрытии строит JaCoCo.
- `lombok.config` — `lombok.addLombokGeneratedAnnotation = true`: методы, которые генерирует Lombok, помечаются `@Generated`, и JaCoCo не включает их в отчёт.
- `jacocoTestCoverageVerification` роняет `check`, если покрытие строк ниже 80%.
- Тесты:
  - `RoomServiceTest` — добавление номеров (в т.ч. `null`), поиск по ID (найден / не найден / `null`), смена доступности, фильтрация (параметризованный тест с `@MethodSource`, `null`-фильтр), неизменяемость и копирование `getAllRooms`;
  - `BookingServiceTest` — `RoomService` и `NotificationService` мокируются: успешное бронирование, порядок и аргументы вызовов (`InOrder`, `ArgumentCaptor`), параметризованные тесты корректных периодов и некорректных параметров, недоступный и несуществующий номер, отмена брони и уведомление, отмена несуществующей брони, неизменяемость `getAllBookings`;
  - `BookingFlowTest` — сценарий с настоящим `RoomService`: после отмены номер снова доступен.

Итог: 49 тестов, покрытие `BookingService` и `RoomService` — 100% по инструкциям и ветвлениям.

## Запуск

```bash
./gradlew test          # тесты + отчёт build/reports/jacoco/test/html/index.html
./gradlew check         # тесты + проверка порога покрытия 80%
```
