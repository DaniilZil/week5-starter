package com.dsu.roomapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final RoomService roomService;

    public StatsController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/api/stats")
    public StatsResponse stats() {
        return new StatsResponse(roomService.count());
    }

    public record StatsResponse(long rooms) {}
}
