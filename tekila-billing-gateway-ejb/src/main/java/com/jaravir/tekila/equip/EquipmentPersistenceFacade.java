package com.jaravir.tekila.equip;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.entity.Util;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerCharge;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerInvoice;
import com.jaravir.tekila.module.accounting.manager.SalesPartnerChargePersitenceFacade;
import com.jaravir.tekila.module.accounting.manager.SalesPartnerInvoicePersistenceFacade;
import com.jaravir.tekila.module.sales.persistence.entity.SalesPartner;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.store.SalesPartnerStore;
import com.jaravir.tekila.module.store.SalesPartnerStorePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentModel;
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
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by sajabrayilov on 12/18/2014.
 */
@Stateless
public class EquipmentPersistenceFacade extends AbstractPersistenceFacade<Equipment> {

    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(EquipmentPersistenceFacade.class);
    @Resource
    private EJBContext ctx;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private SalesPartnerChargePersitenceFacade partnerChargeFacade;
    @EJB
    private SalesPartnerInvoicePersistenceFacade partnerInvoiceFacade;
    @EJB
    private SalesPartnerStorePersistenceFacade partnerStoreFacade;

    public enum Filter implements Filterable {
        PART_NUMBER("partNumber"),
        MODEL("model.name"),
        STATUS("status"),
        MAC_ADDRESS("macAddress"),
        BRAND("brand.name"),
        TYPE("type.name"),
        PRICE("price"),
        PROVIDER("provider.id");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public EquipmentPersistenceFacade() {
        super(Equipment.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public Query getPaginatedQueryWithFilters() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Equipment> criteriaQuery = cb.createQuery(Equipment.class);

        Root root = criteriaQuery.from(Equipment.class);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(root);
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");

        if (!getFilters().isEmpty()) {
            log.debug("Filters: " + getFilters().toString());
            return em.createQuery(criteriaQuery.where(getPredicateWithFilters(cb, root)));
        } else {
            return super.getPaginatedQueryWithFilters();
        }
    }

    @Override
    public Query countAllWithFilters() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);

        Root root = criteriaQuery.from(Equipment.class);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(cb.count(root));
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");

        if (!getFilters().isEmpty()) {
            return em.createQuery(criteriaQuery.where(getPredicateWithFilters(cb, root)));
        } else {
            return super.countAllWithFilters();
        }
    }

    public Equipment findByPartNumber(String partNumber) {
        return em.createQuery("select e from Equipment e where e.partNumber = :part", Equipment.class)
                .setParameter("part", partNumber).getSingleResult();
    }

    public Equipment findById(Long eId) {
        return em.createQuery("select e from Equipment e where e.id = :id", Equipment.class)
                .setParameter("id", eId).getSingleResult();
    }

    public List<Equipment> findByProviderIdWithPage(Long provider, int start, int end, String partName) {

        String sql = "select * " +
                "  from (select rownum r, eq.* " +
                "          from tekila.equipment eq " +
                "         where provider_id = "+provider + " and partnumber like '%"+partName+"%'"+
                "           and status = "+EquipmentStatus.AVAILABLE.getCode()+") " +
                " where r between "+start+" and "+end;

        log.debug(sql);
        return em.createNativeQuery(sql, Equipment.class).getResultList();

//        return em.createQuery("select e from Equipment e where e.provider.id = :prov and e.status = :stat", Equipment.class)
//                .setParameter("prov", provider)
//                .setParameter("stat", EquipmentStatus.AVAILABLE).getResultList();
    }


    public List<Equipment> findByEquipmentsByProviderId(Long provider) {
        String sql = "select * " +
        "  from (select rownum r, eq.* " +
                "          from tekila.equipment eq " +
                "         where provider_id = "+provider +
                "           and status = "+EquipmentStatus.AVAILABLE.getCode()+") ";

        log.debug(sql);

        return em.createNativeQuery(sql, Equipment.class).getResultList();

//        return em.createQuery("select e from Equipment e where e.provider.id = :prov and e.status = :stat", Equipment.class)
//                .setParameter("prov", provider)
//                .setParameter("stat", EquipmentStatus.AVAILABLE).getResultList();
    }


    public BigDecimal findByProviderId(Long provider) {

        return (BigDecimal) em.createNativeQuery(
                " select count(*) " +
                "          from tekila.equipment eq " +
                "         where provider_id = '"+provider +"' "+
                "           and status = "+EquipmentStatus.AVAILABLE.getCode()).getSingleResult();
    }




    public SalesPartnerInvoice transfer(List<Equipment> equipmentList, SalesPartnerStore salesPartnerStore, Double transferPrice) {
        String logHeader = "transfer: ";
        SalesPartnerInvoice invoice = new SalesPartnerInvoice();
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());

        invoice.setUser(user);
        invoice.setPartner(salesPartnerStore.getOwner());
        invoice.setCreationDate(DateTime.now().toDate());
        partnerInvoiceFacade.save(invoice);

        String chargeDesc = null;
        EquipmentStatus oldStatus = null;

        salesPartnerStore = partnerStoreFacade.update(salesPartnerStore);
        Long transferPriceLong = null;

        if (transferPrice != null) {
            transferPriceLong = Util.convertFromDoubleToLong(transferPrice);
        }

        for (Equipment equipment : equipmentList) {
            equipment = update(equipment);
            SalesPartnerCharge charge = new SalesPartnerCharge();
            charge.setUser(user);
            charge.setStatus(AccountingStatus.REGULAR);
            charge.setSalesPartner(salesPartnerStore.getOwner());

            charge.setAmount(transferPriceLong != null ? transferPriceLong : equipment.getPrice().getPrice());
            chargeDesc = String.format("Charge for equipment partNumber %s, id=%d", equipment.getPartNumber(), equipment.getId());
            charge.setDsc(chargeDesc);
            partnerChargeFacade.save(charge);

            invoice.addCharge(charge);

            systemLogger.success(SystemEvent.SALES_PARTNER_CHARGED, null,
                    String.format("sales partner id=%d, %s", salesPartnerStore.getOwner().getId(), chargeDesc));
            log.info(String.format("%s, sales partner id=%d, %s", logHeader, salesPartnerStore.getOwner().getId(), chargeDesc));
            oldStatus = equipment.getStatus();

            equipment.setStatus(EquipmentStatus.TRANSFERED);
            salesPartnerStore.add(equipment);

            systemLogger.success(SystemEvent.EQUIPMENT_TRANSFERED, null,
                    String.format("partner store id=%d, equipment id=%d changed from status %s to %s",
                            salesPartnerStore.getId(), equipment.getId(), oldStatus, equipment.getStatus()));
            log.info(String.format("%s, partner store id=%d, equipment id=%d changed from status %s to %s",
                    logHeader, salesPartnerStore.getId(), equipment.getId(), oldStatus, equipment.getStatus()));

            systemLogger.success(SystemEvent.SALES_PARTNER_STORE_EQUIPMENT_ADDED, null,
                    String.format("partner store id=%d, equipment id=%d added",
                            salesPartnerStore.getId(), equipment.getId()));
            log.info(String.format("%s, partner store id=%d, equipment id=%d added",
                    logHeader, salesPartnerStore.getId(), equipment.getId()));
        }

        systemLogger.success(SystemEvent.SALES_PARTNER_INVOICE_CREATED, null,
                String.format("sales partner id=%d, %s", salesPartnerStore.getOwner().getId(), chargeDesc));
        log.info(String.format("%s, sales partner id=%d invoice id=%d created", logHeader, salesPartnerStore.getOwner().getId(), invoice.getId()));

        return invoice;
    }




    public String getAllEquimpentSqlQuery(Map<Filterable, Object> filters) {

        String query = "select distinct eqp from Equipment eqp ";
        String where = "where ";

        if (filters.get(EquipmentPersistenceFacade.Filter.PART_NUMBER) != null) {
            where += "(lower(eqp.partNumber) like '%" + filters.get(EquipmentPersistenceFacade.Filter.PART_NUMBER).toString().toLowerCase() + "%')";
            filters.remove(EquipmentPersistenceFacade.Filter.PART_NUMBER);
        }

        if (filters.get(EquipmentPersistenceFacade.Filter.MAC_ADDRESS) != null) {
            if (where.length() != 6) {
                where += " and ";
            }
            where += "(lower(eqp.macAddress) like '%" + filters.get(EquipmentPersistenceFacade.Filter.MAC_ADDRESS).toString().toLowerCase() + "%')";
            filters.remove(EquipmentPersistenceFacade.Filter.MAC_ADDRESS);
        }

        if (filters.get(EquipmentPersistenceFacade.Filter.BRAND) != null) {
            if (where.length() != 6) {
                where += " and ";
            }
            where += "(lower(eqp.brand.name) like '%" + filters.get(EquipmentPersistenceFacade.Filter.BRAND).toString().toLowerCase() + "%')";
            filters.remove(EquipmentPersistenceFacade.Filter.BRAND);
        }

        if (filters.get(EquipmentPersistenceFacade.Filter.MODEL) != null) {
            if (where.length() != 6) {
                where += " and ";
            }
            where += "(lower(eqp.model.name) like '%" + filters.get(EquipmentPersistenceFacade.Filter.MODEL).toString().toLowerCase() + "%')";
            filters.remove(EquipmentPersistenceFacade.Filter.MODEL);
        }

        if (filters.get(EquipmentPersistenceFacade.Filter.TYPE) != null) {
            if (where.length() != 6) {
                where += " and ";
            }
            where += "(lower(eqp.type.name) like '%" + filters.get(EquipmentPersistenceFacade.Filter.TYPE).toString().toLowerCase() + "%')";
            filters.remove(EquipmentPersistenceFacade.Filter.TYPE);
        }

        if (filters.get(EquipmentPersistenceFacade.Filter.STATUS) != null) {
            if (where.length() != 6) {
                where += " and ";
            }
            where += "eqp.status = :status";
        }
        return query + where;
    }
}
