package io.caudal.server.controller;

import io.caudal.core.FocusItem;
import io.caudal.core.NextHopItem;
import io.caudal.core.PathwayResult;
import io.caudal.server.CaudalProperties;
import io.caudal.server.api.QueriesApi;
import io.caudal.server.dto.FocusResponse;
import io.caudal.server.dto.PathwayRequest;
import io.caudal.server.dto.PathwayResponse;
import io.caudal.server.service.SpaceManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QueryController implements QueriesApi {

    private final SpaceManager spaceManager;
    private final CaudalProperties.Pathways pathwayModes;
    private final Timer focusTimer;
    private final Timer nextTimer;
    private final Timer pathwaysTimer;

    public QueryController(SpaceManager spaceManager, CaudalProperties properties,
                           MeterRegistry registry) {
        this.spaceManager = spaceManager;
        this.pathwayModes = properties.pathways();
        this.focusTimer = registry.timer("caudal.query.focus");
        this.nextTimer = registry.timer("caudal.query.next");
        this.pathwaysTimer = registry.timer("caudal.query.pathways");
    }

    @Override
    public ResponseEntity<FocusResponse> focus(String space, Integer k) {
        return focusTimer.record(() -> {
            List<FocusItem> items = spaceManager.focus(space, k);
            String asOf = spaceManager.clock().toInstant(spaceManager.clock().nowBucket()).toString();
            return ResponseEntity.ok(new FocusResponse(
                    items.stream().map(i -> new FocusResponse.ScoredItem(i.id(), i.score())).toList(),
                    asOf
            ));
        });
    }

    @Override
    public ResponseEntity<FocusResponse> next(String space, String src, Integer k) {
        return nextTimer.record(() -> {
            List<NextHopItem> items = spaceManager.next(space, src, k);
            String asOf = spaceManager.clock().toInstant(spaceManager.clock().nowBucket()).toString();
            return ResponseEntity.ok(new FocusResponse(
                    items.stream().map(i -> new FocusResponse.ScoredItem(i.id(), i.score())).toList(),
                    asOf
            ));
        });
    }

    @Override
    public ResponseEntity<PathwayResponse> pathways(PathwayRequest request) {
        return pathwaysTimer.record(() -> {
            CaudalProperties.ModeConfig mode = pathwayModes.resolve(request.mode());
            PathwayResult result = spaceManager.pathways(
                    request.space(), request.start(),
                    mode.ants(), mode.maxSteps(),
                    request.k(), null
            );
            String asOf = spaceManager.clock().toInstant(spaceManager.clock().nowBucket()).toString();
            return ResponseEntity.ok(new PathwayResponse(
                    result.paths().stream()
                            .map(p -> new PathwayResponse.PathItem(p.nodes(), p.score()))
                            .toList(),
                    result.topEntities().stream()
                            .map(e -> new FocusResponse.ScoredItem(e.id(), e.score()))
                            .toList(),
                    asOf
            ));
        });
    }
}
