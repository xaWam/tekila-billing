package com.jaravir.tekila.module.campaign;

import com.jaravir.tekila.base.entity.OnlineJoinerCampaign;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author ElmarMa on 7/31/2018
 */
@Stateless
public class OnlineJoinerCampaignPf extends AbstractPersistenceFacade<OnlineJoinerCampaign> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OnlineJoinerCampaignPf() {
        super(OnlineJoinerCampaign.class);
    }





}
