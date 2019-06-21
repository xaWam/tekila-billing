package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.PaymentTypes;

import java.io.Serializable;

/*
public class PaymentTypesResponse implements Serializable {
    private final String id;
    private final String name;

    private PaymentTypesResponse(String id, String name){
        this.id = id;
        this.name = name;
    }

    public static PaymentTypesResponse from (PaymentTypes paymentType){
        return new PaymentTypesResponse(paymentType.name(), paymentType.name());
    }

}
*/


public class PaymentTypesResponse implements Serializable {
    private String id;
    private String name;

    public PaymentTypesResponse(String id, String name){
        this.id = id;
        this.name = name;
    }

    public static PaymentTypesResponse from (PaymentTypes paymentType){
        return new PaymentTypesResponse(paymentType.name(), paymentType.name());
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}