package com.swebench.model;

import lombok.Data;
import lombok.Getter;

import java.util.List;
@Getter
@Data
public class AgentResult {
    private boolean solved;
    private List<String> patches;
    private List<String> conversationLog;

    // Getters and setters
}