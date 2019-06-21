package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.interceptor.SecurityInterceptor;
import com.jaravir.tekila.base.interceptor.annot.PermissionRequired;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.TaxationCategory;
import com.jaravir.tekila.module.archive.subscription.SubscriberArchivePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class SubscriberDetailsPeristenceFacade extends AbstractPersistenceFacade<SubscriberDetails> {

    private static final Logger log = LoggerFactory.getLogger(SubscriberDetailsPeristenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @EJB
    private SubscriberPersistenceFacade subFacade;
    @EJB
    private SubscriberArchivePersistenceFacade archiveFacade;
    @Resource
    private SessionContext ctx;
    private SubscriberType type;

    public SubscriberDetailsPeristenceFacade() {
        super(SubscriberDetails.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    @PermissionRequired(subModule = "Subscriber", privilege = Privilege.INSERT)
    @Interceptors(SecurityInterceptor.class)
    public void createFromForm(SubscriberDetails details, SubscriberFunctionalCategory fnCat, boolean isManagedByLifecycle, SubscriberLifeCycleType lifeCycleType, TaxationCategory taxCategory, User user) {
        details.setEntryDate(
                DateTime.now().toDate()
        );
        details.setUser(user);
        Subscriber sub = new Subscriber();
        sub.setFnCategory(fnCat);

        sub.setMasterAccount(Long.valueOf(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())));
        sub.setDetails(details);
        sub.setBilledByLifeCycle(isManagedByLifecycle);
        sub.setLifeCycle(lifeCycleType);
        sub.setTaxCategory(taxCategory);

        details.setSubscriber(sub);

        try {
            subFacade.save(sub);
            //this.save(details);
        } catch (Exception ex) {
            //Logger log = LoggerFactory.getLogger(SubscriberDetailsPeristenceFacade.class);
            //log.error("Cannot create Subscriber: " + ex.getMessage());
            ctx.setRollbackOnly();
        }
    }

    @Override
    public List<SubscriberDetails> findAll() {
        return (this.type != null ? this.findAllByType(type) : this.findAllRegardlessOfType());
    }

    @Override
    public List<SubscriberDetails> findAllPaginated(int first, int pageSize) {
        return (type != null ? findAllPagintatedWithType(first, pageSize) : super.findAllPaginated(first, pageSize));
    }

    private List<SubscriberDetails> findAllPagintatedWithType(int first, int pageSize) {
        List<SubscriberDetails> ls = this.em.
                createQuery(
                        "select d from SubscriberDetails d where d.type = :type order by d.id desc",
                        SubscriberDetails.class)
                .setParameter("type", type)
                .setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();
        return ls;
    }

    private List<SubscriberDetails> findAllRegardlessOfType() {
        List<SubscriberDetails> ls = this.em.
                createQuery(
                        "select d from SubscriberDetails d",
                        SubscriberDetails.class)
                .getResultList();
        return ls;
    }

    public SubscriberType getType() {
        return type;
    }

    public void setType(SubscriberType type) {
        this.type = type;
    }

    public List<SubscriberDetails> findAllByType(SubscriberType type) {
        List<SubscriberDetails> ls = this.em.
                createQuery(
                        "select d from SubscriberDetails d where d.type = :type order by d.id desc",
                        SubscriberDetails.class)
                .setParameter("type", type)
                .getResultList();
        return ls;
    }

    @Override
    public SubscriberDetails update(SubscriberDetails entity) {
        SubscriberDetails oldEntity = find(entity.getId());
        archiveFacade.archive(oldEntity);
        return super.update(entity);
    }


    public SubscriberDetails getDuplicatePassportNumber(String passNum, SubscriberType type) {
        List<SubscriberDetails> detailsList = getEntityManager().createQuery(
                "select s from SubscriberDetails s where s.passportNumber = :passNum and s.type = :type order by s.id", SubscriberDetails.class)
                .setParameter("passNum", passNum)
                .setParameter("type", type)
                .getResultList();

        if (detailsList != null && !detailsList.isEmpty())
            return detailsList.get(0);

        return null;
    }

    public SubscriberDetails getDuplicatePincode(SubscriberDetails details, SubscriberType type) {
        List<SubscriberDetails> detailsList = getEntityManager().createQuery(
                "select s from SubscriberDetails s where s.pinCode = :pinCode and s.type = :type order by s.id", SubscriberDetails.class)
                .setParameter("pinCode", details.getPinCode())
                .setParameter("type", type)
                .getResultList();

        if (detailsList != null && !detailsList.isEmpty())
            return detailsList.get(0);

        return null;
    }

//    public List<SubscriberDetails> getAllUpdates() {
//        log.debug("~~~Debug from persistence level... "+DateTime.now().minusMonths(5));
//        return getEntityManager().createQuery(
//                "select s from SubscriberDetails s where s.lastUpdateDate >= :lastUpDate", SubscriberDetails.class)
//                .setParameter("lastUpDate", DateTime.now().minusDays(1)).getResultList();
//
//    }

    public List<SubscriberDetails> getAllUpdatesNative(){
        return getEntityManager()
                .createNativeQuery("select * from tekila.subscriber_details where last_update_date > sysdate-1", SubscriberDetails.class)
                .getResultList();
    }

//    public List<SubscriberDetails> getAllUpdates(){
//        log.debug("~~~~Debug from persistence level...");
//        return getEntityManager().createNativeQuery("select * from tekila.subscriber_details where last_update_date >= SYSDATE -1", SubscriberDetails.class)
//                .getResultList();
//    }

}
