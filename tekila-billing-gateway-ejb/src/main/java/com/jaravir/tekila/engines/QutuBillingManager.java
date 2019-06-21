package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.ErrorLogger;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.*;
import java.util.List;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless(name = "QutuBillingManager", mappedName = "QutuBillingManager")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class QutuBillingManager implements BillingEngine {


    private transient final DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void billPrepaid(Subscription subscription) {

    }


    @Override
    public void billPostpaid(Subscription subscription) {

    }

    @Override
    public void manageLifeCyclePrepaid(Subscription subscription) {
    }


    @Override
    public void manageLifeCyclePostapid(Subscription subscription) {

    }

    @Override
    public void manageLifeCyclePrepaidGrace(Subscription subscription) {

    }

    @Override
    public void cancelPrepaid(Subscription subscription) {

    }

    @Override
    public void finalizePrepaid(Subscription subscription) {

    }

    private void finalizeSubscription(Subscription sub) throws Exception {
    }

    @Override
    public void applyLateFeeOrFinalize(Subscription subscription) {

    }

    @Override
    public void sipCharge(Subscription subscription, double amount) {

    }


}
