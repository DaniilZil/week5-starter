package com.dsu.roomapi;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomResponse> list(@RequestParam(required = false) Integer minCapacity,
                           @RequestParam(defaultValue = "") String keyword) {
        return roomService.search(minCapacity, keyword).stream().map(RoomResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> findOne(@PathVariable Long id) {
        return roomService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@RequestBody RoomCreateRequest request) {
        try {
            Room room = roomService.create(request.name(), request.capacity());
            return ResponseEntity
                    .created(URI.create("/api/rooms/" + room.getId()))
                    .body(RoomResponse.from(room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> replace(@PathVariable Long id,
                                        @RequestBody @Valid RoomCreateRequest request) {
        try {
            return ResponseEntity.ok(RoomResponse.from(roomService.update(id, request.name(), request.capacity())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }catch (NoSuchElementException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return roomService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
