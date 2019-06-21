package com.jaravir.tekila.module.subscription.persistence.management.sequence;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.subscription.persistence.entity.agreement.AgreementSequence;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.sound.midi.Sequence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sajabrayilov on 12/23/2014.
 */
@Singleton
@Startup
public class AgreementGenerator extends AbstractPersistenceFacade<AgreementSequence>{
    @PersistenceContext
    private EntityManager em;

    private final static Logger log = Logger.getLogger(AgreementGenerator.class);
    private HashMap<Long, AgreementSequence> sequenceMap;

    public AgreementGenerator() {
        super(AgreementSequence.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @PostConstruct
    public void init () {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<AgreementSequence> criteriaQuery = cb.createQuery(AgreementSequence.class);
        Root root = criteriaQuery.from(AgreementSequence.class);
        criteriaQuery.select(root);
        List<AgreementSequence> sequenceList = getEntityManager().createQuery(criteriaQuery).getResultList();

        log.info(String.format("Read %d agreement sequences from datastore", sequenceList.size()));

        if (sequenceList == null || sequenceList.size() == 0)
            return;

        sequenceMap = new HashMap<>();

        for (AgreementSequence seq : sequenceList) {
            sequenceMap.put(seq.getProvider().getId(), seq);
        }

        log.info(String.format("Finished reading agreement sequences. Map is: %s", sequenceMap));
    }

    @PreDestroy
    public void cleanup() {
        if (sequenceMap == null || sequenceMap.isEmpty())
            return;
        AgreementSequence sequence = null;
        log.info("Cleaning up AgreementGenerator. Persisting sequences");

        for (Map.Entry<Long, AgreementSequence> entry : sequenceMap.entrySet()) {
                try {
                sequence = entry.getValue();
                log.info(String.format("Sequence %s fetched", sequence.toString()));
                update(sequence);
                log.info(String.format("Sequence updated to %s", sequence.toString()));
            }
            catch (Exception ex) {
                log.error("Cannot persist agreement sequence: ", ex);
            }
        }

        log.info("Finished persisting sequences");
    }

    private AgreementSequence findByProvider (long providerID) {
        return em.createQuery("select a from AgreementSequence a where a.provider.id = :prov", AgreementSequence.class)
                .setParameter("prov", providerID).getSingleResult();
    }

    public synchronized Long generate(ServiceProvider provider) {
        if (provider == null)
            throw new IllegalArgumentException("Service provider is required");

        init();
        log.debug("provider string "+provider.getId());
        log.debug("res read : "+sequenceMap.get(provider.getId()).toString());
        long res = sequenceMap.get(provider.getId()).next();
        log.debug("res : "+res);
        cleanup();
        return res;
    }
}
