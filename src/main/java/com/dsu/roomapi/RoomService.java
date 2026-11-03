package com.dsu.roomapi;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> search(Integer minCapacity, String keyword) {
        if(minCapacity == null)
        {
            return roomRepository.findByNameContainingIgnoreCase(keyword);
        }
        return roomRepository.findByCapacityGreaterThanEqualAndNameContainingIgnoreCase(minCapacity, keyword);

    }

    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    public Room create(String name, int capacity) {
        checkCapacity(capacity);
        return roomRepository.save(new Room(null, name, capacity));
    }
    @Transactional
    public Room update(Long id, String name, int capacity) {
        checkCapacity(capacity);
        Room room = roomRepository.findById(id).orElseThrow();
        room.update(name,capacity);
        return room;
    }

    public boolean delete(Long id) {
        if (!roomRepository.existsById(id))
        {
            return false;
        }
        roomRepository.deleteById(id);
        return true;
    }

    public long count() {
        return roomRepository.count();
    }

    private void checkCapacity(int capacity) {
        if (capacity < 1 || capacity > 20) {
            throw new IllegalArgumentException("capacity must be between 1 and 20");
        }
    }
}
