package io.caudal.server.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventItemTest {

    @Test
    void eventItem_nullOrOmittedIntensity_defaultsToOne() {
        EventRequest.EventItem item = new EventRequest.EventItem("a", "b", null, null, null, null);
        assertEquals(1.0, item.intensity());
    }
}
