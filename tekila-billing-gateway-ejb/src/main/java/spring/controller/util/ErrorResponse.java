package spring.controller.util;

import spring.exceptions.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ElmarMa on 3/15/2018
 */
public class ErrorResponse implements Serializable {

    public static final Map<Class, Integer> errorCodes = new HashMap<>();

    static {
        errorCodes.put(CustomerOperationException.class, -10);
        errorCodes.put(AdjustmentException.class, -20);
        errorCodes.put(BadRequestAlertException.class, -30);
        errorCodes.put(FinancialException.class, -40);
        errorCodes.put(VasException.class, -50);
        errorCodes.put(SubscriptionNotFoundException.class, -60);
        errorCodes.put(UserOperationException.class, -70);
    }

    private String message;
    private Integer code;
    private String description;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, Integer code, String description) {
        this.message = message;
        this.code = code;
        this.description = description;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String descirption) {
        this.description = descirption;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "message='" + message + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
