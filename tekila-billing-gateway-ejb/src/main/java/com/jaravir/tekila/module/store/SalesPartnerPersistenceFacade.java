package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.auth.GroupPersistenceFacade;
import com.jaravir.tekila.module.auth.security.PasswordGenerator;
import com.jaravir.tekila.module.sales.SalesPartnerType;
import com.jaravir.tekila.module.sales.persistence.entity.SalesPartner;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 11/26/2015.
 */
@Stateless
public class SalesPartnerPersistenceFacade extends AbstractPersistenceFacade<SalesPartner>{
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(SalesPartnerStorePersistenceFacade.class);
    @Resource
    private EJBContext ctx;
    @EJB private UserPersistenceFacade userFacade;
    @EJB private SystemLogger systemLogger;
    @EJB private GroupPersistenceFacade groupFacade;
    @EJB private PasswordGenerator passwordGenerator;

    private final static String DEALER_GROUP_NAME = "diler_tv";

    public enum Filter implements Filterable {
        NAME("name"), TYPE("type");

        private String field;
        private MatchingOperation operation;

        private Filter (String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation (MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public SalesPartnerPersistenceFacade () {
        super(SalesPartner.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SalesPartner create (SalesPartner partner) {
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
        partner.setCreationDate(DateTime.now());
        partner.setUser(user);

        if (partner.getType() == SalesPartnerType.DISTRIBUTOR) {
            partner = createDistributor(partner);
        }
        /*if (partner.getType() == SalesPartnerType.DEALER) {
            partner = createDealer(partner);
        }*/

        save(partner);

        systemLogger.success(SystemEvent.SALES_PARTNER_CREATED, null, String.format("sales partner id=%d", partner.getId()));
        log.info(String.format("create: sales partner id=%d successfully created", partner.getId()));
        return partner;
    }

    private SalesPartner createDistributor (SalesPartner partner) {
        return partner;
    }

    private SalesPartner createDealer (SalesPartner partner) {
        SalesPartner distributor = partner.getPrincipal();
        return partner;
    }

    public SalesPartner addPartner (SalesPartner principal, SalesPartner partner) {
        principal.addPartner(partner);
        update(principal);
        return principal;
    }
    public User createDealerUser (SalesPartner partner,String userEmail, String firstName, String surname) {
        partner = update(partner);
        Group group = groupFacade.findByName(DEALER_GROUP_NAME);

        char[] rawPass = passwordGenerator.generatePassword();
        User user = new User();
        user.setUserName(partner.getName());
        user.setDsc(partner.getDesc());
        user.setPassword(passwordGenerator.encodePassword(rawPass));
        user.setGroup(group);
        user.setEmail(userEmail);
        user.setFirstName(firstName);
        user.setSurname(surname);
        userFacade.save(user, String.valueOf(rawPass), true);

        partner.setPartnerUser(user);

        systemLogger.success(SystemEvent.USER_CREATED, null, String.format("user id=%d, partner id=%d", user.getId(), partner.getId()));
        log.info(String.format("createDealerUser: user id=%d for partner id=%d successfully created", user.getId(),partner.getId()));

        return user;
    }

    public SalesPartner findByUsername (String userName) {
        return em.createQuery("select s from SalesPartner s where s.type = :type and s.partnerUser.userName = :username", SalesPartner.class)
                .setParameter("type", SalesPartnerType.DEALER).setParameter("username", userName).getSingleResult();
    }
}
