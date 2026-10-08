package ma.solide.ki_demo;

@Service
public class AiService {
    private final ChatClient chatClient;

    public AiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String summarize(String input) {
        return chatClient.call("Fasse diesen Text zusammen: " + input);
    }
}
