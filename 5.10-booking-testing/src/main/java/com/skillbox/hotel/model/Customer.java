package com.skillbox.hotel.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Модель клиента (пользователя).
 */
@Data
@AllArgsConstructor
public class Customer {

    /**
     * Уникальный идентификатор клиента.
     */
    private Long customerId;

    /**
     * Имя клиента.
     */
    private String name;
}
