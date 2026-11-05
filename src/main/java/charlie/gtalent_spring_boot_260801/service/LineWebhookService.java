package charlie.gtalent_spring_boot_260801.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.client.ChatClient;

import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;

@Service
public class LineWebhookService {

    private final LineReplyService lineReplyService;
    private final ChatClient chatClient;

    public LineWebhookService(LineReplyService lineReplyService, ChatClient.Builder chatClientBuilder) {
        this.lineReplyService = lineReplyService;
        this.chatClient = chatClientBuilder.build();
    }

    public void handleEvent(Event event) {
        if (event instanceof MessageEvent messageEvent) {
            handleMessageEvent(messageEvent);
        }
    }

    private void handleMessageEvent(MessageEvent event) {
        MessageContent message = event.message();

        if (message instanceof TextMessageContent textMessage) {
           String lineUserId = event.source().userId();
        String text = textMessage.text().trim();
        System.out.println("LINE webhook text message from " + lineUserId + ": " + text);

        String reply = switch (text.toLowerCase()) {
            case "ping" -> "pong";
            case "你好" -> "你好！我是群組機器人。";
            default -> null; // 沒命中關鍵字,交給 AI
        };

        if (reply != null) {
            lineReplyService.replyText(event.replyToken(), reply);
            return;
        }

        // 沒命中關鍵字 → 非同步呼叫 Gemini
        String replyToken = event.replyToken();
        CompletableFuture.runAsync(() -> {
            try {
                String answer = chat(text);
                lineReplyService.replyText(replyToken, answer);
            } catch (Exception ex) {
                ex.printStackTrace();
                lineReplyService.replyText(replyToken, "抱歉,AI 暫時無法回應,請稍後再試。");
            }
        });
        }
    }
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}