package org.example.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the support bot: submit a user query (string only) and get an answer.
 * Conversation history is maintained on the backend, not accepted from the UI.
 */
@RestController
@RequestMapping("/api")
public class QueryController {

    private final VoiceBotQueryService queryService;

    public QueryController(VoiceBotQueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest()
                .body(new QueryResponse(null, "Missing or empty 'query' in request body."));
        }
        try {
            String answer = queryService.answer(request.getQuery().trim());
            return ResponseEntity.ok(new QueryResponse(answer, null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new QueryResponse(null, "Error: " + e.getMessage()));
        }
    }

    @GetMapping(value = "/load", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationHistoryResponse> loadHistory() {
        return ResponseEntity.ok(new ConversationHistoryResponse(queryService.getConversationHistory()));
    }
}
