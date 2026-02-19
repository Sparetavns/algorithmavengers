package org.example.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Serves root and /api so the app doesn't return 404 when opened in the browser.
 */
@RestController
public class HomeController {

    @GetMapping(value = {"/", "/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> home() {
        return Map.of(
            "message", "Voice Bot API",
            "usage", "POST /api/query with JSON body: {\"query\": \"your question\"}"
        );
    }
}
