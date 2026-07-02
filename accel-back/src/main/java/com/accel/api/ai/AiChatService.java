package com.accel.api.ai;

public interface AiChatService {
    AiResponse carbti(AiRequest request);
    
    AiResponse carsulting(AiRequest request);

    EvScoreResponse evScore(EvScoreRequest request);
}
