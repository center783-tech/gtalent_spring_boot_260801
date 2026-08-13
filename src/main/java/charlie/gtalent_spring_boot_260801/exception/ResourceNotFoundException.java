package charlie.gtalent_spring_boot_260801.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String errorKey;

    private final String messageCode;

    public ResourceNotFoundException(String errorKey, String messageCode) {
        this.errorKey = errorKey;
        this.messageCode = messageCode;
    }

    public String getErrorKey() {
        return errorKey;
    }
    
    public String getMessageCode() {
        return messageCode;
    }

}