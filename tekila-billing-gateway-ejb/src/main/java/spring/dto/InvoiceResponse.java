package spring.dto;

import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Invoice;

import java.util.Date;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class InvoiceResponse {
    public final Long id;
    public final String subscriberName;
    public final String subscriberAddress;
    public final String phones;
    public final Date creationDate;
    public final String closingBalance;
    public final String total;
    public final String sumPaid;
    public final InvoiceState state;

    private InvoiceResponse(Invoice invoice) {
        this.id = invoice.getId();
        this.subscriberName = invoice.getSubscriber().getDetails().getFirstName() + " " + invoice.getSubscriber().getDetails().getSurname() + " " +
                invoice.getSubscriber().getDetails().getMiddleName();
        this.subscriberAddress = Utils.merge(
                invoice.getSubscriber().getDetails().getCity(),
                invoice.getSubscriber().getDetails().getStreet(),
                invoice.getSubscriber().getDetails().getBuilding(),
                invoice.getSubscriber().getDetails().getApartment());
        this.phones = Utils.merge(
                invoice.getSubscriber().getDetails().getPhoneMobile(),
                invoice.getSubscriber().getDetails().getPhoneMobileAlt(),
                invoice.getSubscriber().getDetails().getPhoneLandline());
        this.creationDate = invoice.getCreationDate();
        this.closingBalance = invoice.getBalanceForView();
        this.total = invoice.getSumChargedForView();
        this.sumPaid = invoice.getSumPaidForView();
        this.state = invoice.getState();
    }

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice);
    }
}
