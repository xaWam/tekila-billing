package spring.dto;

import com.jaravir.tekila.module.accounting.AccountingCategoryType;
import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.TaxationCategory;

import java.io.Serializable;

public class AccountingCategoryDTO implements Serializable {

    private Long id;
    private AccountingCategoryType type;
    private String name;
    private TaxationCategory taxCategory;

    public AccountingCategoryType getType() {
        return type;
    }

    public void setType(AccountingCategoryType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaxationCategory getTaxCategory() {
        return taxCategory;
    }

    public void setTaxCategory(TaxationCategory taxCategory) {
        this.taxCategory = taxCategory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
