package lns.back.backend_pet_friendly.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import lns.back.backend_pet_friendly.domain.exception.DuplicateReviewException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest req(String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    @Test
    void notFound_maps404() {
        ResponseEntity<?> res = handler.handleNotFound(
                new ResourceNotFoundException("nope"), req("/api/v1/places/x"));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).error()).isEqualTo("nope");
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).path()).isEqualTo("/api/v1/places/x");
    }

    @Test
    void badRequest_maps400() {
        ResponseEntity<?> res = handler.handleBadRequest(
                new IllegalArgumentException("bad"), req("/x"));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).error()).isEqualTo("bad");
    }

    @Test
    void duplicateReview_maps409() {
        ResponseEntity<?> res = handler.handleConflict(
                new DuplicateReviewException("already reviewed"), req("/reviews"));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).error()).isEqualTo("already reviewed");
    }

    @Test
    void accessDenied_maps403() {
        ResponseEntity<?> res = handler.handleForbidden(req("/reviews/1"));
        assertThat(res.getStatusCode().value()).isEqualTo(403);
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).error()).isEqualTo("Access denied");
    }

    @Test
    void generic_maps500() {
        ResponseEntity<?> res = handler.handleGeneric(req("/boom"));
        assertThat(res.getStatusCode().value()).isEqualTo(500);
        assertThat(((GlobalExceptionHandler.ErrorResponse) res.getBody()).error()).isEqualTo("Internal server error");
    }
}
