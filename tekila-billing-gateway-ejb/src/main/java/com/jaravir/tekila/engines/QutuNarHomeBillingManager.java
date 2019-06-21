
package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.periodic.Job;
import com.jaravir.tekila.module.periodic.JobProperty;
import com.jaravir.tekila.module.periodic.JobPropertyType;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.reactivation.SubscriptionReactivation;
import com.jaravir.tekila.module.subscription.persistence.management.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.ErrorLogger;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.*;
import java.util.List;


/**
 * Created by ShakirG on 10/09/2018.
 */
@Stateless(name = "QutuNarHomeBillingManager", mappedName = "QutuNarHomeBillingManager")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class QutuNarHomeBillingManager implements BillingEngine {

    private final static Logger log = Logger.getLogger(BillingManager.class);


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

    @Override
    public void applyLateFeeOrFinalize(Subscription subscription) {

    }

    @Override
    public void sipCharge(Subscription subscription, double amount) {

    }
}

