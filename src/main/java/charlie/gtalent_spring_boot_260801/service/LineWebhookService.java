package charlie.gtalent_spring_boot_260801.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.springframework.ai.chat.client.ChatClient;

import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.GroupSource;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.RoomSource;
import com.linecorp.bot.webhook.model.TextMessageContent;

@Service
public class LineWebhookService {

    private static final Logger log = LoggerFactory.getLogger(LineWebhookService.class);

    // LINE 單則文字訊息上限 5000 字
    private static final int LINE_TEXT_MAX_LENGTH = 5000;
    // 群組 / 多人聊天室中,只有以此開頭的訊息才交給 AI
    private static final String AI_PREFIX = "/ai";

    private final LineReplyService lineReplyService;
    private final ChatClient chatClient;
    // 呼叫 Gemini / LINE 都是會阻塞的網路 I/O,用虛擬執行緒處理
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LineWebhookService(LineReplyService lineReplyService, ChatClient.Builder chatClientBuilder) {
        this.lineReplyService = lineReplyService;
        this.chatClient = chatClientBuilder.build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public void handleEvent(Event event) {
        if (event instanceof MessageEvent messageEvent) {
            handleMessageEvent(messageEvent);
        }
    }

    private void handleMessageEvent(MessageEvent event) {
        MessageContent message = event.message();

        if (!(message instanceof TextMessageContent textMessage)) {
            return;
        }

        String lineUserId = event.source().userId();
        String text = textMessage.text().trim();
        log.info("LINE webhook text message from {}", lineUserId);

        String reply = switch (text.toLowerCase()) {
            case "ping" -> "pong";
            case "你好" -> "你好！我是群組機器人。";
            default -> null; // 沒命中關鍵字,交給 AI
        };

        if (reply != null) {
            lineReplyService.replyText(event.replyToken(), reply);
            return;
        }

        // 群組 / 多人聊天室:只有 "/ai 問題" 才呼叫 AI,避免每句話都回覆
        String question = text;
        if (event.source() instanceof GroupSource || event.source() instanceof RoomSource) {
            if (!text.toLowerCase().startsWith(AI_PREFIX)) {
                return;
            }
            question = text.substring(AI_PREFIX.length()).trim();
            if (question.isEmpty()) {
                return;
            }
        }

        // 沒命中關鍵字 → 非同步呼叫 Gemini
        String replyToken = event.replyToken();
        String prompt = question;
        CompletableFuture.supplyAsync(() -> {
            try {
                return chat(prompt);
            } catch (Exception ex) {
                log.error("Gemini 呼叫失敗", ex);
                return "抱歉,AI 暫時無法回應,請稍後再試。";
            }
        }, executor)
                .thenAccept(answer -> lineReplyService.replyText(replyToken, toLineText(answer)))
                .exceptionally(ex -> {
                    log.error("LINE 回覆失敗", ex);
                    return null;
                });
    }

    private String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    private String toLineText(String answer) {
        if (answer == null || answer.isBlank()) {
            return "抱歉,我目前沒有答案。";
        }
        if (answer.length() > LINE_TEXT_MAX_LENGTH) {
            return answer.substring(0, LINE_TEXT_MAX_LENGTH - 3) + "...";
        }
        return answer;
    }
}
