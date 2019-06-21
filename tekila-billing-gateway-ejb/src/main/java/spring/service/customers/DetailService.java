package spring.service.customers;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionDetails;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.springframework.stereotype.Service;
import spring.dto.CustomerDetailRequestDTO;
import spring.exceptions.CustomerOperationException;

import javax.ejb.EJB;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 4/25/2018
 */
@Service
public class DetailService {

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;


    public void updateSubscriptionDeatils(Long subscriptionId, CustomerDetailRequestDTO newDetails) {
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not find subscription,can not update detailes");
        SubscriptionDetails details = subscription.getDetails();
        if (newDetails.getName() != null && !newDetails.getName().isEmpty())
            details.setName(newDetails.getName());
        if (newDetails.getSurname() != null && !newDetails.getSurname().isEmpty())
            details.setSurname(newDetails.getSurname());
        if (newDetails.getApartment() != null && !newDetails.getApartment().isEmpty())
            details.setApartment(newDetails.getApartment());
        if (newDetails.getBuilding() != null && !newDetails.getBuilding().isEmpty())
            details.setBuilding(newDetails.getBuilding());
        if (newDetails.getCity() != null && !newDetails.getCity().isEmpty())
            details.setCity(newDetails.getCity());
        if (newDetails.getComment() != null && !newDetails.getComment().isEmpty())
            details.setComments(newDetails.getComment());
        if (newDetails.getStreet()!= null && !newDetails.getStreet().isEmpty())
            details.setStreet(newDetails.getStreet());

        subscription.setDetails(details);
        subscriptionPersistenceFacade.update(subscription);
    }

}
