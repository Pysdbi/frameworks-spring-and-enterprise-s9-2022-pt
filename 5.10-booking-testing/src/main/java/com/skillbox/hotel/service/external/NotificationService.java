package com.skillbox.hotel.service.external;

/**
 * Внешний сервис для уведомления клиентов.
 */
public interface NotificationService {

    /**
     * Отправляет уведомление клиенту.
     *
     * @param customerId ID клиента.
     * @param message Сообщение для клиента.
     */
    void sendNotification(Long customerId, String message);
}
