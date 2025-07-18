package com.swebench;

import com.swebench.model.AgentResult;
import java.util.ArrayList;
import java.util.List;

public class AgentRunner {
    public AgentResult run(String prompt) {
        // Implement actual agent integration here
        AgentResult result = new AgentResult();
        result.setSolved(false);
        result.setPatches(new ArrayList<>());
        result.setConversationLog(List.of(
                "Agent initialized",
                "Received prompt: " + prompt.substring(0, 50) + "..."
        ));
        return result;
    }
}