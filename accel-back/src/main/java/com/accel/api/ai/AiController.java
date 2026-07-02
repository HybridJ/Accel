package com.accel.api.ai;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI", description = "AI 호출 API")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    @PostMapping("/carbti")
    public ResponseEntity<AiResponse> carbti(@RequestBody(required = true) List<AiRequest.SurveyAnswer> answers) {
        return ResponseEntity.ok(aiChatService.carbti(new AiRequest(answers)));
    }
    
    @PostMapping("/carsulting")
    public ResponseEntity<AiResponse> carsulting(@RequestBody(required = true) List<AiRequest.SurveyAnswer> answers){
    	return ResponseEntity.ok(aiChatService.carsulting(new AiRequest(answers)));
    }

    @PostMapping("/ev/score")
    public ResponseEntity<EvScoreResponse> evScore(@RequestBody(required = true) EvScoreRequest request) {
        try {
            return ResponseEntity.ok(aiChatService.evScore(request));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        } catch (EvScoreException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();

            if (message.contains("timeout")) {
                throw new ResponseStatusException(GATEWAY_TIMEOUT, message, e);
            }

            throw new ResponseStatusException(BAD_GATEWAY, message, e);
        }
    }
}
