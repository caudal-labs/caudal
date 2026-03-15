package io.caudal.server.controller;

import io.caudal.server.dto.DeleteSpaceResponse;
import io.caudal.server.persistence.PersistenceService;
import io.caudal.server.service.SpaceManager;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SpaceController {

    private final SpaceManager spaceManager;
    private final PersistenceService persistence;

    public SpaceController(SpaceManager spaceManager, PersistenceService persistence) {
        this.spaceManager = spaceManager;
        this.persistence = persistence;
    }

    @DeleteMapping("/spaces/{id}")
    public ResponseEntity<?> deleteSpace(@PathVariable String id) {
        boolean removed = spaceManager.deleteSpace(id);
        if (!removed) {
            return ResponseEntity.status(404).body(Map.of("error", "Space not found: " + id));
        }
        persistence.deleteSpaceData(id);
        return ResponseEntity.ok(new DeleteSpaceResponse(id, Instant.now().toString()));
    }
}
