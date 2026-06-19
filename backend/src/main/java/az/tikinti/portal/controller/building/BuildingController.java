package az.tikinti.portal.controller.building;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.building.BuildingFilterRequest;
import az.tikinti.portal.model.dto.request.building.BuildingMediaRequest;
import az.tikinti.portal.model.dto.request.building.BuildingRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.building.BuildingMediaResponse;
import az.tikinti.portal.model.dto.response.building.BuildingResponse;
import az.tikinti.portal.service.building.BuildingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping("/search")
    public ResponseEntity<PageableResponse<BuildingResponse>> search(
            @Valid @RequestBody BuildingFilterRequest request) {
        return ResponseEntity.ok(buildingService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(buildingService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> create(@Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.status(CREATED).body(buildingService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuildingResponse> update(
            @PathVariable UUID id, @Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.ok(buildingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<BuildingMediaResponse> addMedia(
            @PathVariable UUID id, @Valid @RequestBody BuildingMediaRequest request) {
        return ResponseEntity.status(CREATED).body(buildingService.addMedia(id, request));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<Void> removeMedia(@PathVariable UUID id, @PathVariable UUID mediaId) {
        buildingService.removeMedia(id, mediaId);
        return ResponseEntity.noContent().build();
    }
}
