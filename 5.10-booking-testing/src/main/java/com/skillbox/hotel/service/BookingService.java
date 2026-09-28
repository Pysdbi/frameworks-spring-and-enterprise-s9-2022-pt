package com.skillbox.hotel.service;

import com.skillbox.hotel.model.Booking;
import com.skillbox.hotel.model.Room;
import com.skillbox.hotel.service.external.NotificationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис управления бронированиями в отеле.
 */
public class BookingService {

    private final List<Booking> bookings = new ArrayList<>();
    private final RoomService roomService;
    private final NotificationService notificationService;

    /**
     * Конструктор сервиса бронирований.
     *
     * @param roomService Экземпляр {@link RoomService} для управления доступностью номеров.
     * @param notificationService Экземпляр {@link NotificationService} для уведомления клиентов.
     */
    public BookingService(RoomService roomService, NotificationService notificationService) {
        this.roomService = roomService;
        this.notificationService = notificationService;
    }

    /**
     * Создание бронирования
     *
     * @param bookingId ID брони
     * @param roomId ID номера
     * @param customerId ID клиента
     * @param startDate дата начала брони
     * @param endDate дата окончания брони
     * @return бронь
     */
    public Booking createBooking(Long bookingId, Long roomId,
                                 Long customerId, LocalDate startDate,
                                 LocalDate endDate) {
        if (bookingId == null || roomId == null || customerId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Недопустимые параметры бронирования");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Дата начала бронирования должна быть раньше даты окончания");
        }

        return roomService.findRoomById(roomId)
                .filter(Room::isAvailable)
                .map(room -> {
                    Booking booking = new Booking(bookingId, roomId, customerId, startDate, endDate);
                    bookings.add(booking);
                    roomService.updateRoomAvailability(roomId, false);
                    return booking;
                })
                .map(booking -> {
                    notificationService.sendNotification(customerId, "Ваше бронирование подтверждено: " + booking);
                    return booking;
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "Номер с ID " + roomId + " недоступен для бронирования."));
    }

    /**
     * Отмена бронирования
     *
     * @param bookingId ID брони
     */
    public void cancelBooking(Long bookingId) {
        bookings.stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .ifPresentOrElse(bookingToCancel -> {
                    roomService.updateRoomAvailability(bookingToCancel.getRoomId(), true);
                    notificationService.sendNotification(bookingToCancel.getCustomerId(),
                            "Ваше бронирование отменено: " + bookingToCancel);
                    bookings.remove(bookingToCancel);

                }, () -> {
                    throw new IllegalArgumentException("Бронирование с ID " + bookingId + " не найдено.");
                });
    }

    public List<Booking> getAllBookings() {
        return List.copyOf(bookings);
    }
}
