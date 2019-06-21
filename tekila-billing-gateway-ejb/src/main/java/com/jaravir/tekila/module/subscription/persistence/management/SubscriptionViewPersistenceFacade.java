package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionView;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;

/**
 * Created by sajabrayilov on 1/19/2015.
 */
@Stateless
public class SubscriptionViewPersistenceFacade extends AbstractPersistenceFacade<SubscriptionView> {

    @PersistenceContext
    private EntityManager em;

    public enum Filter implements Filterable {
        AGREEMENT("agreement"),
        FIRSTNAME("firstName"),
        LASTNAME("surname"),
        MIDDLENAME("middleName"),
        CITY_OF_BIRTH("cityOfBirth"),
        CITIZENSHIP("citizenship"),
        COUNTRY("country"),
        PASSPORT_SERIES("passportSeries"),
        PASSPORT_NUMBER("passportNumber"),
        //PASSPORT_AUTHORITY("passportAuthority"),
        //PASSPORT_VALID("passportValidTill"),
        EMAIL("email"),
        PHONE_MOBILE("phoneMobile"),
        PHONE_ALT("phoneMobileAlt"),
        PHONE_LANDLINE("phoneLandline"),
        ADDRESS_CITY("city"),
        ADDRESS_ATS("ats"),
        ADDRESS_STREET("street"),
        ADDRESS_BUILDING("building"),
        ADDRESS_APARTMENT("apartment"),
        CORPORATE_COMPANY("companyName"),
        CORPORATE_COMPANY_TYPE("bankAccount"),
        DATE_OF_BIRTH("dateOfBirth"),
        CREATED_ON("creationDate"),
        ENTRY_DATE("entryDate");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
        }

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

    private final static Logger log = Logger.getLogger(SubscriptionViewPersistenceFacade.class);

    public SubscriptionViewPersistenceFacade() {
        super(SubscriptionView.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
