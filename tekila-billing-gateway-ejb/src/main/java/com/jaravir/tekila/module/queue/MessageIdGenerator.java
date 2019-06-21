package com.jaravir.tekila.module.queue;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.event.notification.MessageId;
import com.sun.xml.rpc.processor.model.Message;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by kmaharov on 08.07.2016.
 */
@Stateless
public class MessageIdGenerator extends AbstractPersistenceFacade<MessageId> {

    @PersistenceContext
    EntityManager em;

    public MessageIdGenerator() {
        super(MessageId.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public synchronized int getNext() {
        List<MessageId> messageIdList = findAll(); //list size should be 1 always
        MessageId newid = new MessageId();
        save(newid);
        for (MessageId id : messageIdList) {
            delete(id);
        }
        return (int)newid.getId();
    }
}
