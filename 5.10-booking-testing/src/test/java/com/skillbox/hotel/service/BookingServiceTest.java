package com.skillbox.hotel.service;

import com.skillbox.hotel.model.Booking;
import com.skillbox.hotel.model.Room;
import com.skillbox.hotel.service.external.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("BookingService")
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Long BOOKING_ID = 100L;
    private static final Long ROOM_ID = 1L;
    private static final Long CUSTOMER_ID = 10L;
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 5);

    @Mock
    private RoomService roomService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingService bookingService;

    private static Room availableRoom() {
        return new Room(ROOM_ID, "Standard", 3_000, true);
    }

    /** Создаёт бронь по умолчанию, чтобы проверять отмену и список броней. */
    private Booking createDefaultBooking() {
        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(availableRoom()));
        return bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END);
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("создаёт бронь, если все параметры корректны")
        void createsBooking() {
            when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(availableRoom()));

            Booking booking = bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END);

            assertThat(booking).isNotNull()
                    .extracting(Booking::getBookingId, Booking::getRoomId, Booking::getCustomerId,
                            Booking::getStartDate, Booking::getEndDate)
                    .containsExactly(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END);
            assertThat(bookingService.getAllBookings()).containsExactly(booking);
        }

        @Test
        @DisplayName("ищет номер, занимает его и отправляет уведомление — именно в таком порядке")
        void callsDependenciesWithCorrectArguments() {
            when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(availableRoom()));

            Booking booking = bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END);

            InOrder order = inOrder(roomService, notificationService);
            order.verify(roomService).findRoomById(ROOM_ID);
            order.verify(roomService).updateRoomAvailability(ROOM_ID, false);
            order.verify(notificationService)
                    .sendNotification(CUSTOMER_ID, "Ваше бронирование подтверждено: " + booking);
            verifyNoMoreInteractions(roomService, notificationService);
        }

        @Test
        @DisplayName("отправляет клиенту уведомление с данными брони")
        void sendsConfirmationNotification() {
            when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(availableRoom()));
            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

            bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END);

            verify(notificationService).sendNotification(eq(CUSTOMER_ID), message.capture());
            assertThat(message.getValue())
                    .startsWith("Ваше бронирование подтверждено")
                    .contains("bookingId=" + BOOKING_ID, "roomId=" + ROOM_ID, START.toString(), END.toString());
        }

        static Stream<Arguments> validPeriods() {
            return Stream.of(
                    Arguments.of("заезд и выезд в один день", START, START),
                    Arguments.of("одна ночь", START, START.plusDays(1)),
                    Arguments.of("длительное проживание", START, START.plusMonths(3)),
                    Arguments.of("через границу года", LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("validPeriods")
        @DisplayName("создаёт бронь для корректного периода")
        void createsBookingForValidPeriod(String description, LocalDate start, LocalDate end) {
            when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(availableRoom()));

            Booking booking = bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, start, end);

            assertThat(booking.getStartDate()).isEqualTo(start);
            assertThat(booking.getEndDate()).isEqualTo(end);
            verify(roomService).updateRoomAvailability(ROOM_ID, false);
        }

        static Stream<Arguments> invalidParameters() {
            String nullMessage = "Недопустимые параметры бронирования";
            String datesMessage = "Дата начала бронирования должна быть раньше даты окончания";
            return Stream.of(
                    Arguments.of("bookingId = null", null, ROOM_ID, CUSTOMER_ID, START, END, nullMessage),
                    Arguments.of("roomId = null", BOOKING_ID, null, CUSTOMER_ID, START, END, nullMessage),
                    Arguments.of("customerId = null", BOOKING_ID, ROOM_ID, null, START, END, nullMessage),
                    Arguments.of("startDate = null", BOOKING_ID, ROOM_ID, CUSTOMER_ID, null, END, nullMessage),
                    Arguments.of("endDate = null", BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, null, nullMessage),
                    Arguments.of("все параметры null", null, null, null, null, null, nullMessage),
                    Arguments.of("начало на день позже окончания",
                            BOOKING_ID, ROOM_ID, CUSTOMER_ID, END.plusDays(1), END, datesMessage),
                    Arguments.of("начало на год позже окончания",
                            BOOKING_ID, ROOM_ID, CUSTOMER_ID, START.plusYears(1), START, datesMessage));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidParameters")
        @DisplayName("бросает исключение при некорректных параметрах и не трогает зависимости")
        void rejectsInvalidParameters(String description, Long bookingId, Long roomId, Long customerId,
                                      LocalDate start, LocalDate end, String expectedMessage) {
            assertThatThrownBy(() -> bookingService.createBooking(bookingId, roomId, customerId, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage);

            verifyNoInteractions(roomService, notificationService);
            assertThat(bookingService.getAllBookings()).isEmpty();
        }

        @Test
        @DisplayName("не бронирует недоступный номер")
        void rejectsUnavailableRoom() {
            when(roomService.findRoomById(ROOM_ID))
                    .thenReturn(Optional.of(new Room(ROOM_ID, "Standard", 3_000, false)));

            assertThatThrownBy(() -> bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Номер с ID " + ROOM_ID + " недоступен для бронирования.");

            verify(roomService, never()).updateRoomAvailability(anyLong(), eq(false));
            verifyNoInteractions(notificationService);
            assertThat(bookingService.getAllBookings()).isEmpty();
        }

        @Test
        @DisplayName("не бронирует несуществующий номер")
        void rejectsMissingRoom() {
            when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(BOOKING_ID, ROOM_ID, CUSTOMER_ID, START, END))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("недоступен для бронирования");

            verify(roomService).findRoomById(ROOM_ID);
            verifyNoMoreInteractions(roomService);
            verify(notificationService, never()).sendNotification(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("освобождает номер, уведомляет клиента и удаляет бронь")
        void cancelsBooking() {
            Booking booking = createDefaultBooking();

            bookingService.cancelBooking(BOOKING_ID);

            verify(roomService).updateRoomAvailability(ROOM_ID, true);
            verify(notificationService).sendNotification(CUSTOMER_ID, "Ваше бронирование отменено: " + booking);
            assertThat(bookingService.getAllBookings()).isEmpty();
        }

        @Test
        @DisplayName("удаляет только указанную бронь")
        void cancelsOnlyGivenBooking() {
            Booking first = createDefaultBooking();
            when(roomService.findRoomById(2L)).thenReturn(Optional.of(new Room(2L, "Suite", 9_000, true)));
            Booking second = bookingService.createBooking(200L, 2L, 20L, START, END);

            bookingService.cancelBooking(BOOKING_ID);

            assertThat(bookingService.getAllBookings()).containsExactly(second).doesNotContain(first);
            verify(roomService).updateRoomAvailability(ROOM_ID, true);
            verify(roomService, never()).updateRoomAvailability(2L, true);
        }

        @Test
        @DisplayName("бросает исключение, если брони с таким ID нет")
        void rejectsUnknownBooking() {
            assertThatThrownBy(() -> bookingService.cancelBooking(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Бронирование с ID 999 не найдено.");

            verifyNoInteractions(roomService, notificationService);
        }

        @Test
        @DisplayName("бросает исключение при повторной отмене той же брони")
        void rejectsDoubleCancel() {
            createDefaultBooking();
            bookingService.cancelBooking(BOOKING_ID);

            assertThatThrownBy(() -> bookingService.cancelBooking(BOOKING_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("не найдено");

            verify(roomService).updateRoomAvailability(ROOM_ID, true);
        }
    }

    @Nested
    @DisplayName("getAllBookings")
    class GetAllBookings {

        @Test
        @DisplayName("возвращает пустой список, пока броней нет")
        void emptyByDefault() {
            assertThat(bookingService.getAllBookings()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("возвращает неизменяемый список")
        void returnsUnmodifiableList() {
            createDefaultBooking();
            List<Booking> bookings = bookingService.getAllBookings();

            assertThatThrownBy(() -> bookings.add(new Booking(1L, 1L, 1L, START, END)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> bookings.remove(0))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(bookings::clear)
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("возвращает копию: отмена брони не меняет ранее полученный список")
        void returnsCopy() {
            Booking booking = createDefaultBooking();
            List<Booking> snapshot = bookingService.getAllBookings();

            bookingService.cancelBooking(BOOKING_ID);

            assertThat(snapshot).hasSize(1).containsExactly(booking);
            assertThat(bookingService.getAllBookings()).isEmpty();
        }
    }
}
