package charlie.gtalent_spring_boot_260801.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String errorKey, String messageCode) {
        super(errorKey, messageCode);
    }

}