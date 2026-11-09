package com.dsu.roomapi;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
    public RoomResponse findOne(@PathVariable Long id) {
        return RoomResponse.from(roomService.get(id));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
        RoomResponse room = RoomResponse.from(roomService.create(request.name(), request.capacity()));
        return ResponseEntity.created(URI.create("/api/rooms/" + room.id())).body(room);
    }

    @PutMapping("/{id}")
    public RoomResponse replace(@PathVariable Long id, @Valid @RequestBody RoomCreateRequest request) {
        return RoomResponse.from(roomService.update(id, request.name(), request.capacity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
