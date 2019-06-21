package spring.dto;

import com.jaravir.tekila.module.service.model.BillingPrinciple;

import java.io.Serializable;
import java.util.Arrays;

/*
public class BillingPrinciplesResponse {

    private final String id;
    private final String name;

    private BillingPrinciplesResponse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static BillingPrinciplesResponse from(BillingPrinciple principle) {
        return new BillingPrinciplesResponse(principle.name(), principle.name());
    }
}
*/

public class BillingPrinciplesResponse implements Serializable {

    private String id;
    private String name;

    public BillingPrinciplesResponse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static BillingPrinciplesResponse from(BillingPrinciple principle) {
        return new BillingPrinciplesResponse(principle.name(), principle.name());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}