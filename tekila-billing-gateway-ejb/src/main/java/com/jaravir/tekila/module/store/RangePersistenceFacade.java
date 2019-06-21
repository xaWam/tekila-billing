package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.store.ip.IpAddressResult;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 11/25/2014.
 */
@Stateless
public class RangePersistenceFacade extends AbstractPersistenceFacade<IpAddressRange> {
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(RangePersistenceFacade.class);

    public RangePersistenceFacade() {
        super(IpAddressRange.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public synchronized IpAddress findAndReserve(Nas nas) {
        List<IpAddressRange> rangeList = findAll();
        IpAddress ipAddress = null;
        for (IpAddressRange range : rangeList) {
            log.debug("Range: " + range);
            if (range.getNas() != null && range.getNas().getId() == nas.getId()) {
                ipAddress = range.findAndReserveFreeAddress();
                if (ipAddress.compareTo(range.getEnd()) > 0) {
                    ipAddress = null;
                    continue;
                }
                log.debug("Reserved IP Addresses: " + ipAddress);
                break;
            }
        }

        return ipAddress;
    }

    public synchronized IpAddressResult findResultAndReserve(Nas nas) {
        List<IpAddressRange> rangeList = findAll();
        IpAddress ipAddress = null;
        for (IpAddressRange range : rangeList) {
            log.debug("Range: " + range);
            if (range.getNas() != null && range.getNas().getId() == nas.getId()) {
                ipAddress = range.findAndReserveFreeAddress();
                if (ipAddress.compareTo(range.getEnd()) > 0) {
                    continue;
                }
                log.debug("Reserved IP Addresses: " + ipAddress);
                return new IpAddressResult(ipAddress, range);
            }
        }
        return null;
    }

    public List<IpAddressRange> findRangesByNas(Nas nas) {
        try {
            return em.createQuery("select rg from IpAddressRange rg where rg.nas = :nas", IpAddressRange.class)
                    .setParameter("nas", nas).getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public IpAddressRange findIpRange(Nas nas, IpAddress ipAddress) {
        try {
            List<IpAddressRange> rangeList = findRangesByNas(nas);
            IpAddressRange ipRange = null;
            boolean stop = false;
            for (IpAddressRange range : rangeList) {
                if (stop) {
                    break;
                }
                for (IpAddress address : range.findFreeAddresses()) {
                    if (address.equals(ipAddress)) {
                        ipRange = range;
                        stop = true;
                        break;
                    }
                }
            }
            return ipRange;
        } catch (Exception ex) {
            return null;
        }
    }

    public IpAddressRange findReservedIpRange(Nas nas, IpAddress ipAddress) {
        try {
            List<IpAddressRange> rangeList = findRangesByNas(nas);
            IpAddressRange ipRange = null;
            boolean stop = false;
            for (IpAddressRange range : rangeList) {
                if (stop) {
                    break;
                }
                for (IpAddress address : range.getReservedAddressList()) {
                    if (address.equals(ipAddress)) {
                        ipRange = range;
                        stop = true;
                        break;
                    }
                }
            }
            return ipRange;
        } catch (Exception ex) {
            return null;
        }
    }
}
