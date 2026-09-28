package com.skillbox.hotel.model;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * Модель сущности бронирования.
 */
@Data
@AllArgsConstructor
public class Booking {

    /**
     * Уникальный идентификатор бронирования.
     */
    private Long bookingId;

    /**
     * Идентификатор номера, связанного с бронированием.
     */
    private Long roomId;

    /**
     * Имя клиента, сделавшего бронирование.
     */
    private Long customerId;

    /**
     * Дата начала бронирования.
     */
    private LocalDate startDate;

    /**
     * Дата окончания бронирования.
     */
    private LocalDate endDate;
}
