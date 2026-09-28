package com.skillbox.hotel.service;

import com.skillbox.hotel.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoomService")
class RoomServiceTest {

    private RoomService roomService;

    private final Room standard = new Room(1L, "Standard", 3_000, true);
    private final Room deluxe = new Room(2L, "Deluxe", 6_000, false);
    private final Room suite = new Room(3L, "Suite", 12_000, true);

    @BeforeEach
    void setUp() {
        // Новый сервис на каждый тест — тесты не влияют друг на друга
        roomService = new RoomService();
    }

    private void addAllRooms() {
        roomService.addRoom(standard);
        roomService.addRoom(deluxe);
        roomService.addRoom(suite);
    }

    @Nested
    @DisplayName("addRoom")
    class AddRoom {

        @Test
        @DisplayName("добавляет номер в список")
        void addsRoom() {
            roomService.addRoom(standard);

            assertThat(roomService.getAllRooms()).containsExactly(standard);
        }

        @Test
        @DisplayName("сохраняет порядок добавления номеров")
        void keepsInsertionOrder() {
            addAllRooms();

            assertThat(roomService.getAllRooms()).containsExactly(standard, deluxe, suite);
        }

        @Test
        @DisplayName("игнорирует null")
        void ignoresNull() {
            roomService.addRoom(null);

            assertThat(roomService.getAllRooms()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRoomById")
    class FindRoomById {

        @Test
        @DisplayName("находит номер по ID")
        void findsExistingRoom() {
            addAllRooms();

            assertThat(roomService.findRoomById(2L)).containsSame(deluxe);
        }

        @Test
        @DisplayName("возвращает пустой Optional для неизвестного ID")
        void returnsEmptyForUnknownId() {
            addAllRooms();

            assertThat(roomService.findRoomById(99L)).isEmpty();
        }

        @Test
        @DisplayName("возвращает пустой Optional для ID = null")
        void returnsEmptyForNullId() {
            addAllRooms();

            assertThat(roomService.findRoomById(null)).isEmpty();
        }

        @Test
        @DisplayName("возвращает пустой Optional, если номеров нет")
        void returnsEmptyWhenNoRooms() {
            assertThat(roomService.findRoomById(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateRoomAvailability")
    class UpdateRoomAvailability {

        @Test
        @DisplayName("делает доступный номер недоступным")
        void marksRoomUnavailable() {
            roomService.addRoom(standard);

            roomService.updateRoomAvailability(1L, false);

            assertThat(roomService.findRoomById(1L)).get()
                    .extracting(Room::isAvailable)
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("делает недоступный номер доступным")
        void marksRoomAvailable() {
            roomService.addRoom(deluxe);

            roomService.updateRoomAvailability(2L, true);

            assertThat(deluxe.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("не меняет другие номера")
        void doesNotTouchOtherRooms() {
            addAllRooms();

            roomService.updateRoomAvailability(1L, false);

            assertThat(roomService.getAllRooms())
                    .extracting(Room::getRoomId, Room::isAvailable)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(1L, false),
                            org.assertj.core.groups.Tuple.tuple(2L, false),
                            org.assertj.core.groups.Tuple.tuple(3L, true));
        }

        @Test
        @DisplayName("ничего не делает для неизвестного ID")
        void ignoresUnknownId() {
            addAllRooms();

            roomService.updateRoomAvailability(99L, false);

            assertThat(roomService.getAllRooms())
                    .extracting(Room::isAvailable)
                    .containsExactly(true, false, true);
        }
    }

    @Nested
    @DisplayName("getAvailableRooms")
    class GetAvailableRooms {

        static Stream<Arguments> filters() {
            return Stream.of(
                    Arguments.of("только доступные", (Predicate<Room>) Room::isAvailable, List.of(1L, 3L)),
                    Arguments.of("дешевле 5000", (Predicate<Room>) r -> r.getPrice() < 5_000, List.of(1L)),
                    Arguments.of("от 6000 и дороже", (Predicate<Room>) r -> r.getPrice() >= 6_000, List.of(2L, 3L)),
                    Arguments.of("тип Suite", (Predicate<Room>) r -> "Suite".equals(r.getType()), List.of(3L)),
                    Arguments.of("доступные дороже 5000",
                            (Predicate<Room>) r -> r.isAvailable() && r.getPrice() > 5_000, List.of(3L)),
                    Arguments.of("все номера", (Predicate<Room>) r -> true, List.of(1L, 2L, 3L)),
                    Arguments.of("ни один не подходит", (Predicate<Room>) r -> false, List.of()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("filters")
        @DisplayName("возвращает номера, подходящие под фильтр")
        void filtersRooms(String description, Predicate<Room> filter, List<Long> expectedIds) {
            addAllRooms();

            assertThat(roomService.getAvailableRooms(filter))
                    .extracting(Room::getRoomId)
                    .containsExactlyElementsOf(expectedIds);
        }

        @Test
        @DisplayName("возвращает пустой список, если фильтр null")
        void returnsEmptyForNullFilter() {
            addAllRooms();

            assertThat(roomService.getAvailableRooms(null)).isEmpty();
        }

        @Test
        @DisplayName("возвращает пустой список, если номеров нет")
        void returnsEmptyWhenNoRooms() {
            assertThat(roomService.getAvailableRooms(r -> true)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllRooms")
    class GetAllRooms {

        @Test
        @DisplayName("возвращает пустой список для нового сервиса")
        void emptyByDefault() {
            assertThat(roomService.getAllRooms()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("возвращает неизменяемый список")
        void returnsUnmodifiableList() {
            addAllRooms();
            List<Room> rooms = roomService.getAllRooms();

            assertThatThrownBy(() -> rooms.add(new Room(4L, "Econom", 1_000, true)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> rooms.remove(0))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(rooms::clear)
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("возвращает копию: новые номера не попадают в ранее полученный список")
        void returnsCopy() {
            roomService.addRoom(standard);
            List<Room> snapshot = roomService.getAllRooms();

            roomService.addRoom(suite);

            assertThat(snapshot).hasSize(1).containsExactly(standard);
            assertThat(roomService.getAllRooms()).hasSize(2);
        }
    }
}
