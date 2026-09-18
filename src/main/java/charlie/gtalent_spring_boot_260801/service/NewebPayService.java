package charlie.gtalent_spring_boot_260801.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import charlie.gtalent_spring_boot_260801.constant.NotifyStatus;
import charlie.gtalent_spring_boot_260801.constant.OrderStatus;
import charlie.gtalent_spring_boot_260801.constant.PaymentStatus;
import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.entity.BookOrder;
import charlie.gtalent_spring_boot_260801.entity.Payment;
import charlie.gtalent_spring_boot_260801.entity.PaymentNotification;
import charlie.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import charlie.gtalent_spring_boot_260801.repository.BookOrderRepository;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;
import charlie.gtalent_spring_boot_260801.repository.PaymentNotificationRepository;
import charlie.gtalent_spring_boot_260801.repository.PaymentRepository;
import charlie.gtalent_spring_boot_260801.response.NewebPayPaymentFormResponse;
import jakarta.persistence.NoResultException;

@Service
public class NewebPayService {

    private final PaymentRepository paymentRepository;
    private final PaymentNotificationRepository paymentNotificationRepository;
    private final BookOrderRepository bookOrderRepository;
    private final BookRepository bookRepository;
    private final String merchantId;
    private final String hashKey;
    private final String hashIv;
    private final String version;
    private final String gatewayUrl;
    private final String notifyUrl;
    private final String returnUrl;

    // 抓取application.properties裡的藍新金流設定
    public NewebPayService(
            PaymentRepository paymentRepository,
            PaymentNotificationRepository paymentNotificationRepository,
            BookOrderRepository bookOrderRepository,
            BookRepository bookRepository,
            @Value("${newebpay.merchant-id}") String merchantId,
            @Value("${newebpay.hash-key}") String hashKey,
            @Value("${newebpay.hash-iv}") String hashIv,
            @Value("${newebpay.version}") String version,
            @Value("${newebpay.gateway-url}") String gatewayUrl,
            @Value("${newebpay.notify-url}") String notifyUrl,
            @Value("${newebpay.return-url}") String returnUrl) {
        this.paymentRepository = paymentRepository;
        this.paymentNotificationRepository = paymentNotificationRepository;
        this.bookOrderRepository = bookOrderRepository;
        this.bookRepository = bookRepository;
        this.merchantId = merchantId;
        this.hashKey = hashKey;
        this.hashIv = hashIv;
        this.version = version;
        this.gatewayUrl = gatewayUrl;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
    }

    @Transactional
    public NewebPayPaymentFormResponse createPaymentForm(Long paymentId) {
        // 產生付款表單前先確認必要設定都有填。
        validateConfig();

        Payment payment = paymentRepository.findById(paymentId)
                            .orElseThrow(() -> new ResourceNotFoundException("payment", ResponseMessages.RESOURCE_NOT_FOUND));

        BookOrder order = bookOrderRepository.findById(payment.getOrderId())
                            .orElseThrow(() -> new ResourceNotFoundException("order", ResponseMessages.RESOURCE_NOT_FOUND));
        
        Book book = findActiveBook(order.getBookId());

        // 組合字串為url
        String url = buildTradeInfo(payment, book);

        // url執行 AES-256-CBC (使用 PKCS7 填充)，並將結果轉換至十六進制
        String tradeInfo = encryptTradeInfo(url);
        
        // 組合字串為hashs
        String hashs = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIv;

        // 轉成大寫且加密sha256
        String tradeSha = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            tradeSha =  toHex(digest.digest(hashs.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }

        // 表單資料已產生後，付款進入等待使用者完成付款與等待藍新回呼的階段。
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTradeSha(tradeSha);
        paymentRepository.save(payment);

        return new NewebPayPaymentFormResponse(gatewayUrl, merchantId, version, tradeInfo, tradeSha);
    }


    @Transactional
    public String handleNotify(Map<String, String> params) {
        String tradeInfo = params.get("TradeInfo");
        String tradeSha = params.get("TradeSha");

        PaymentNotification notification = new PaymentNotification(params.toString());
        notification.setNotifyStatus(NotifyStatus.RECEIVED);

        if (isBlank(tradeInfo) || isBlank(tradeSha)) {
            return failNotification(notification, "TradeInfo or TradeSha is missing", "0|Missing TradeInfo or TradeSha");
        }

        try {
            if (!verifyTradeSha(tradeInfo, tradeSha)) {
                notification.setVerified((byte) 0);
                return failNotification(notification, "TradeSha mismatch", "0|TradeSha Invalid");
            }
        } catch (Exception exception) {
            return failNotification(notification, "TradeSha verify error: " + exception.getMessage(),
                    "0|TradeSha Verify Error");
        }

        notification.setVerified((byte) 1);

        Map<String, String> result;
        try {
            result = parseQueryString(decryptTradeInfo(tradeInfo));
        } catch (Exception exception) {
            return failNotification(notification, "TradeInfo decrypt error: " + exception.getMessage(),
                    "0|Decrypt Failed");
        }

        String resultMerchantId = result.get("MerchantID");
        String merchantOrderNo = result.get("MerchantOrderNo");
        String status = result.get("Status");
        String amtStr = result.get("Amt");
        String tradeNo = result.get("TradeNo");

        notification.setMerchantOrderNo(merchantOrderNo);
        notification.setProviderTradeNo(tradeNo);

        if (!merchantId.equals(resultMerchantId)) {
            return failNotification(notification, "MerchantID mismatch: " + resultMerchantId,
                    "0|MerchantID Mismatch");
        }

        Payment payment = paymentRepository.findByMerchantOrderNo(merchantOrderNo).orElse(null);
        if (payment == null) {
            return failNotification(notification, "Payment not found: " + merchantOrderNo, "0|Payment Not Found");
        }

        notification.setPaymentId(payment.getId());

        if (PaymentStatus.PAID.equals(payment.getPaymentStatus())
                || PaymentStatus.FAILED.equals(payment.getPaymentStatus())) {
            notification.setNotifyStatus(NotifyStatus.PROCESSED);
            notification.setProcessedAt(LocalDateTime.now());
            notification.setErrorMessage("Payment already processed");
            paymentNotificationRepository.save(notification);
            return "1|OK";
        }

        int notifyAmt;
        try {
            notifyAmt = Integer.parseInt(amtStr);
        } catch (NumberFormatException exception) {
            return failNotification(notification, "Amt invalid: " + amtStr, "0|Amount Invalid");
        }

        if (!payment.getAmount().equals(notifyAmt)) {
            return failNotification(notification,
                    "Amount mismatch: payment=" + payment.getAmount() + " notify=" + notifyAmt,
                    "0|Amount Mismatch");
        }

        BookOrder bookOrder = bookOrderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("bookOrder", ResponseMessages.RESOURCE_NOT_FOUND));

        payment.setTradeSha(tradeSha);
        payment.setProviderTradeNo(tradeNo);
        payment.setReturnCode(status);

        LocalDateTime now = LocalDateTime.now();
        if ("SUCCESS".equals(status)) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setReturnMessage("Payment success");
            bookOrder.setOrderStatus(OrderStatus.PAID);
            bookOrder.setPaidAt(now);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setReturnMessage(result.getOrDefault("Message", "Payment failed"));
            bookOrder.setOrderStatus(OrderStatus.CANCELLED);
            bookOrder.setCancelledAt(now);
        }

        paymentRepository.save(payment);
        bookOrderRepository.save(bookOrder);

        notification.setNotifyStatus(NotifyStatus.PROCESSED);
        notification.setProcessedAt(now);
        paymentNotificationRepository.save(notification);

        return "1|OK";
    }

    

    // 組合字串為hashs，轉成大寫且加密sha256
    private String createTradeSha(String tradeInfo) {
        String raw = "HashKey=" + hashKey + "&TradeInfo=" + tradeInfo + "&HashIV=" + hashIv;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean verifyTradeSha(String tradeInfo, String tradeSha) {
        return createTradeSha(tradeInfo).equalsIgnoreCase(tradeSha);
    }

    private String decryptTradeInfo(String tradeInfo) throws Exception {
        byte[] encryptedBytes = hexToBytes(tradeInfo);
        SecretKeySpec keySpec = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8).trim();
    }

    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }

        byte[] data = new byte[hex.length() / 2];
        for (int index = 0; index < hex.length(); index += 2) {
            int high = Character.digit(hex.charAt(index), 16);
            int low = Character.digit(hex.charAt(index + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("invalid hex character");
            }
            data[index / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> map = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    // 檢查藍新金流商店ID
    // 檢查藍新金流 HashKey
    // 檢查藍新金流 HashIV
    private void validateConfig() {
        if (isBlank(merchantId) || isBlank(hashKey) || isBlank(hashIv)
                || isBlank(version) || isBlank(gatewayUrl) || isBlank(notifyUrl) || isBlank(returnUrl)) {
            throw new IllegalStateException("NewebPay config is incomplete");
        }

        if (hashKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("NewebPay HashKey must be 32 bytes");
        }

        if (hashIv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalStateException("NewebPay HashIV must be 16 bytes");
        }
    }

    // 組合成url
    private String buildTradeInfo(Payment payment, Book book) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("RespondType", "String");
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Version", version);
        params.put("MerchantOrderNo", payment.getMerchantOrderNo());
        params.put("Amt", String.valueOf(payment.getAmount()));
        params.put("ItemDesc", book.getName());
        params.put("NotifyURL", notifyUrl);
        params.put("ReturnURL", returnUrl);

        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    // 將 url 進行 AES 加密，並產生 TradeInfo
    private String encryptTradeInfo(String rawTradeInfo) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            return toHex(cipher.doFinal(rawTradeInfo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("NewebPay TradeInfo encryption failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        // 將 byte array 轉成小寫 hex；TradeSha 會在呼叫端再轉大寫。
        return HexFormat.of().formatHex(bytes);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String failNotification(PaymentNotification notification, String errorMessage, String response) {
        notification.setNotifyStatus(NotifyStatus.FAILED);
        notification.setErrorMessage(errorMessage);
        paymentNotificationRepository.save(notification);
        return response;
    }

    // 先確認書籍存在且未被軟刪除；不存在就不要建立任何訂單或付款資料。
    private Book findActiveBook(Long bookId) {
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
    }
}
