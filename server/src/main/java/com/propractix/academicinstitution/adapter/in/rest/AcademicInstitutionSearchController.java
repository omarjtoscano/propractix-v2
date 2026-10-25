package com.propractix.academicinstitution.adapter.in.rest;

import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutions;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.platform.observability.CorrelationIdFilter;
import com.propractix.platform.web.PublicProblemDetails;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class AcademicInstitutionSearchController {
    static final String PATH = "/api/v1/academic-institutions";
    private final SearchPublishedInstitutions search;
    private final AcademicInstitutionSearchMapper mapper;

    public AcademicInstitutionSearchController(SearchPublishedInstitutions search, AcademicInstitutionSearchMapper mapper) {
        this.search = search;
        this.mapper = mapper;
    }

    @GetMapping(PATH)
    public ResponseEntity<?> search(@RequestParam MultiValueMap<String, String> parameters,
                                    HttpServletResponse response) {
        var result = search.search(mapper.toQuery(parameters));
        if (result instanceof SearchPublishedInstitutionsResult.Found found) {
            return ResponseEntity.ok(mapper.toResponse(found.page()));
        }
        int status = result instanceof SearchPublishedInstitutionsResult.SnapshotChanged ? 409 : 503;
        String code = status == 409 ? "catalog_snapshot_changed" : "academic_institution_catalog_unavailable";
        return ResponseEntity.status(status).body(PublicProblemDetails.create(status, code, PATH,
                response.getHeader(CorrelationIdFilter.HEADER_NAME), List.of()));
    }
}
