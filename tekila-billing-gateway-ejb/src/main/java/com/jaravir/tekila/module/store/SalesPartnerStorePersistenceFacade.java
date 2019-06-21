package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerCharge;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerInvoice;
import com.jaravir.tekila.module.sales.SalesPartnerType;
import com.jaravir.tekila.module.sales.persistence.entity.SalesPartner;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
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
import java.util.List;

/**
 * Created by sajabrayilov on 11/26/2015.
 */
@Stateless
public class SalesPartnerStorePersistenceFacade extends AbstractPersistenceFacade<SalesPartnerStore>{
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(SalesPartnerStorePersistenceFacade.class);
    @EJB private UserPersistenceFacade userFacade;
    @EJB private SystemLogger systemLogger;
    @EJB private SalesPartnerPersistenceFacade partnerFacade;
    @EJB private EquipmentPersistenceFacade equipmentFacade;

    @Resource
    private EJBContext ctx;

    public enum Filter implements Filterable {
        EQUIPMENT_PARTNUMBER("equipmentList.partNumber"),
        PARTNER_ID("owner.id"),
        PARTNER_NAME("owner.name"),
        PARTNER_TYPE("owner.type"),
        PRINCIPAL_ID("owner.principal.id");

        private String field;
        private MatchingOperation operation;

        Filter (String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public SalesPartnerStorePersistenceFacade () {
        super(SalesPartnerStore.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SalesPartnerStore create (SalesPartner partner) {
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
        SalesPartnerStore store = new SalesPartnerStore();
        store.setUser(user);
        store.setCreationDate(DateTime.now());
        store.setOwner(partner);
        store.setName(partner.getName());
        save(store);
        partner = partnerFacade.update(partner);
        partner.addStore(store);

        systemLogger.success(SystemEvent.SALES_PARTNER_STORE_CREATED, null, String.format("partner id=%d, store id=%d",
                partner.getId(), store.getId()));
        log.info(String.format("create: store id = %d for partner id = %d created successfully", partner.getId(), store.getId()));

        return store;
    }

    public SalesPartnerStore transfer (List<Equipment> equipmentList, SalesPartnerStore salesPartnerStore, SalesPartnerStore origin) {
        String logHeader = "transfer: ";
        EquipmentStatus oldStatus = null;

        salesPartnerStore = update(salesPartnerStore);
        origin = update(origin);

        for (Equipment equipment : equipmentList) {
            oldStatus = equipment.getStatus();
            equipment = equipmentFacade.update(equipment);
            equipment.setStatus(EquipmentStatus.TRANSFERED);
            origin.remove(equipment);
            salesPartnerStore.add(equipment);

            systemLogger.success(SystemEvent.EQUIPMENT_TRANSFERED, null,
                    String.format("transfered from partner store id=%d to partner store id=%d, equipment id=%d changed from status %s to %s",
                            origin.getId(), salesPartnerStore.getId(), equipment.getId(), oldStatus, equipment.getStatus()));
            log.info(String.format("%s, transfered from partner store id=%d to partner store id=%d equipment id=%d changed from status %s to %s",
                    logHeader, origin.getId(), salesPartnerStore.getId(), equipment.getId(), oldStatus, equipment.getStatus()));

            systemLogger.success(SystemEvent.SALES_PARTNER_STORE_EQUIPMENT_REMOVED, null,
                    String.format("partner store id=%d, equipment id=%d removed",
                            origin.getId(), equipment.getId()));
            log.info(String.format("%s, partner store id=%d, equipment id=%d removed",
                    logHeader, origin.getId(), equipment.getId()));

            systemLogger.success(SystemEvent.SALES_PARTNER_STORE_EQUIPMENT_ADDED, null,
                    String.format("partner store id=%d, equipment id=%d added",
                            salesPartnerStore.getId(), equipment.getId()));
            log.info(String.format("%s, partner store id=%d, equipment id=%d added",
                    logHeader, salesPartnerStore.getId(), equipment.getId()));
        }

        return salesPartnerStore;
    }

    public SalesPartnerStore findByPartner (SalesPartner partner) {
        return em.createQuery("select s from SalesPartnerStore s where s.owner.id = :ownerID", SalesPartnerStore.class).setParameter("ownerID", partner.getId())
                .getSingleResult();
    }

    public SalesPartnerStore findByPartnerUsername (String userName) {
        return em.createQuery("select s from SalesPartnerStore s where s.owner.partnerUser.userName = :username and s.owner.type = :type", SalesPartnerStore.class)
                .setParameter("username", userName).setParameter("type", SalesPartnerType.DEALER)
                .getSingleResult();
    }
}
