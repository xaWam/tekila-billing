package com.jaravir.tekila.module.store.ats;


import com.jaravir.tekila.base.entity.ATSGOV;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.module.subscription.persistence.entity.AtsStatus;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Stateless
public class AtsGovsPersistenceFacade extends AbstractPersistenceFacade<ATSGOV> {
    private final static Logger log = Logger.getLogger(AtsGovsPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;


    public enum Filter implements Filterable {
        NAME("name") ;

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



    public AtsGovsPersistenceFacade() {
        super(ATSGOV.class);
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ATSGOV> findAllAts() {
        Query query = em.createQuery("select ats from ATSGOV  ats", ATSGOV.class);
        return query.getResultList();
    }


    public ATSGOV create(String sname, String name, String dirfname, String dirname) {
        ATSGOV atsgov = new ATSGOV();
        atsgov.setName(name);
        atsgov.setSname(sname);
        atsgov.setDirFname(dirfname);
        atsgov.setDirName(dirname);
        save(atsgov);
        return update(atsgov);
    }


}
