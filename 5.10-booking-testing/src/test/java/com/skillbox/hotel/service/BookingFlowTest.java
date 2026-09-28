package com.skillbox.hotel.service;

import com.skillbox.hotel.model.Room;
import com.skillbox.hotel.service.external.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

/**
 * Сценарий целиком на настоящем RoomService: мокируется только внешний сервис уведомлений.
 */
@DisplayName("Бронирование и отмена с настоящим RoomService")
@ExtendWith(MockitoExtension.class)
class BookingFlowTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 3);

    @Mock
    private NotificationService notificationService;

    private RoomService roomService;
    private BookingService bookingService;
    private Room room;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        room = new Room(1L, "Deluxe", 7_500, true);
        roomService.addRoom(room);
        bookingService = new BookingService(roomService, notificationService);
    }

    @Test
    @DisplayName("после бронирования номер занят, после отмены снова доступен")
    void roomBecomesAvailableAfterCancel() {
        bookingService.createBooking(10L, 1L, 5L, START, END);
        assertThat(room.isAvailable()).isFalse();
        assertThat(roomService.getAvailableRooms(Room::isAvailable)).isEmpty();

        bookingService.cancelBooking(10L);

        assertThat(room.isAvailable()).isTrue();
        assertThat(roomService.getAvailableRooms(Room::isAvailable)).containsExactly(room);
        verify(notificationService).sendNotification(eq(5L), startsWith("Ваше бронирование подтверждено"));
        verify(notificationService).sendNotification(eq(5L), startsWith("Ваше бронирование отменено"));
    }

    @Test
    @DisplayName("занятый номер нельзя забронировать второй раз, пока бронь не отменена")
    void roomCannotBeBookedTwice() {
        bookingService.createBooking(10L, 1L, 5L, START, END);

        assertThatThrownBy(() -> bookingService.createBooking(11L, 1L, 6L, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("недоступен");

        bookingService.cancelBooking(10L);

        assertThat(bookingService.createBooking(11L, 1L, 6L, START, END).getCustomerId()).isEqualTo(6L);
    }
}
