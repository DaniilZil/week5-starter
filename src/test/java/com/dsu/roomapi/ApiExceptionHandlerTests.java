package com.dsu.roomapi;

import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {
    @Test
    void unexpectedFailureReturnsApiErrorWithoutExposingInternalDetails() throws Exception {
        RoomService service = mock(RoomService.class);
        when(service.get(1L)).thenThrow(new IllegalStateException("internal diagnostic"));
        var mvc = MockMvcBuilders.standaloneSetup(new RoomController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(get("/api/rooms/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"status":500,"error":"Internal Server Error","message":"unexpected server error",
                         "path":"/api/rooms/1","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }
}
