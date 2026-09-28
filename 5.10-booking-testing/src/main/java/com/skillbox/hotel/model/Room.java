package com.skillbox.hotel.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Модель сущности номера отеля.
 */
@Data
@AllArgsConstructor
public class Room {

    /**
     * Уникальный идентификатор номера.
     */
    private Long roomId;

    /**
     * Тип номера (например, "Standard", "Deluxe", "Suite").
     */
    private String type;

    /**
     * Цена за ночь.
     */
    private double price;

    /**
     * Флаг доступности номера.
     */
    private boolean available;
}
