package pl.training.mcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimeService {

    @Tool(description = "get current time", returnDirect = true)
    public String currentTime() {
        return LocalDateTime.now().toString();
    }

}
