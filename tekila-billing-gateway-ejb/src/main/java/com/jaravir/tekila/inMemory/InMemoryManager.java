/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.inMemory;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import java.util.List;
import javax.annotation.*;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * created by shnovruzov on 17.03.2016
 */
@Singleton
@Startup
public class InMemoryManager {

    @PersistenceContext
    private EntityManager em;
    private List<Subscription> subscriptionList;

    public List<Subscription> getAllSubscriptions() {
        return subscriptionList;
    }

    public void addSubscription(Subscription sub) {
        subscriptionList.add(sub);
    }

    public void updateSubscription(Subscription sub) {
        for(Subscription sbn : subscriptionList)
            if(sub.getId() == sbn.getId()){
                subscriptionList.remove(sbn);
                subscriptionList.add(sub);
                break;
            }
    }
    
    public void updateSubscriber(SubscriberDetails details){
        Subscriber sub = details.getSubscriber();
        for(Subscription sbn : subscriptionList){
            if(sbn.getSubscriber().getId() == sub.getId()){
                sbn.getSubscriber().setDetails(details);
            }
        }
    }
    
    @PostConstruct
    public void init() {
        if (subscriptionList == null) {
            //subscriptionList = getAllSubscriptionsInitially();
        }
    }

    private List<Subscription> getAllSubscriptionsInitially() {
        return em.createQuery("select distinct s from Subscription s", Subscription.class).getResultList();
    }

    @PreDestroy
    public void cleanup() {
        if (subscriptionList == null) {
            return;
        }
        subscriptionList.clear();
        subscriptionList = null;
    }
}
