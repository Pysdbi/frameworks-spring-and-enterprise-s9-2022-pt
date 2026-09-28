package com.skillbox.hotel.service;


import com.skillbox.hotel.model.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Сервис управления номерами в отеле.
 */
public class RoomService {

    private final List<Room> rooms = new ArrayList<>();

    /**
     * Добавляет новый номер в список номеров.
     *
     * @param room Экземпляр {@link Room}, представляющий добавляемый номер.
     */
    public void addRoom(Room room) {
        if (room == null) {
            return;
        }
        rooms.add(room);
    }

    /**
     * Возвращает список номеров, удовлетворяющих фильтру.
     *
     * @param filter Условие фильтрации в виде функционального интерфейса {@link Predicate}.
     * @return Список номеров {@link Room}, доступных для бронирования и удовлетворяющих фильтру.
     */
    public List<Room> getAvailableRooms(Predicate<Room> filter) {
        return rooms.stream()
                .filter(room -> filter != null && filter.test(room))
                .collect(Collectors.toList());
    }

    /**
     * Ищет номер по его уникальному идентификатору.
     *
     * @param roomId Уникальный идентификатор номера.
     * @return Экземпляр {@link Optional}, содержащий номер, если он найден, или пустой.
     */
    public Optional<Room> findRoomById(Long roomId) {
        return rooms.stream()
                .filter(Objects::nonNull)
                .filter(room -> room.getRoomId().equals(roomId))
                .findFirst();
    }

    /**
     * Обновляет доступность номера.
     *
     * @param roomId Уникальный идентификатор номера.
     * @param available Новое значение доступности (true — доступен, false — недоступен).
     */
    public void updateRoomAvailability(Long roomId, boolean available) {
        findRoomById(roomId).ifPresent(room -> room.setAvailable(available));
    }

    /**
     * Возвращает список всех номеров.
     *
     * @return Список всех номеров {@link Room}.
     */
    public List<Room> getAllRooms() {
        return List.copyOf(rooms);
    }
}
