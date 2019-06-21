package reseller;

import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.service.entity.Zone;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ZonePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.PaymentTypes;
import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.apache.log4j.Logger;
import org.jfree.util.Log;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Stateless
@Path("list")
@Produces(APPLICATION_JSON)
@Transactional
public class ListEndpoint {

    private final static Logger log = Logger.getLogger(ListEndpoint.class);
    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;
    @EJB
    private ZonePersistenceFacade zonePersistenceFacade;
    @EJB
    private SubscriptionServiceTypePersistenceFacade serviceTypePersistenceFacade;
    @EJB
    private ResellerFetcher resellerFetcher;

    public static class PaymentTypeDTO {
        public int id;
        public String name;

        PaymentTypeDTO() {
        }

        PaymentTypeDTO(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @GET
    @Path("payment_methods")
    @JWTTokenNeeded
    public List<PaymentTypeDTO> getPaymentMethods() {
        List<PaymentTypeDTO> paymentTypeList = new ArrayList<>();
        for (final PaymentTypes paymentType : PaymentTypes.values()) {
            paymentTypeList.add(new PaymentTypeDTO(paymentType.ordinal(), paymentType.name()));
        }
        return paymentTypeList;
    }



    @GET
    @Path("services")
    @JWTTokenNeeded
    public List<ServiceDTO> getServices(@Context HttpServletRequest request) {
        Reseller reseller = resellerFetcher.getReseller(request);

        List<ServiceDTO> services = new ArrayList<>();

        log.debug("reseller provider "+ reseller.getProvider());

        for (final Service service : servicePersistenceFacade.findAll(reseller.getProvider().getId())) {
            if (service.getProvider().getId() == reseller.getProvider().getId()) {
                services.add(new ServiceDTO(service.getId(), service.getName(), service.getServicePrice()));
            }
        }

        return services;
    }

    public static class ZoneDTO {
        public long id;
        public String name;

        ZoneDTO() {
        }

        ZoneDTO(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @GET
    @Path("zones")
    @JWTTokenNeeded
    public List<ZoneDTO> getZones() {
        List<ZoneDTO> zones = new ArrayList<>();
        for (final Zone zone : zonePersistenceFacade.findAll()) {
            zones.add(new ZoneDTO(zone.getId(), zone.getName()));
        }
        return zones;
    }

    public static class ServiceTypeDTO {
        public long id;
        public String name;

        ServiceTypeDTO() {
        }

        ServiceTypeDTO(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @GET
    @Path("service_types")
    @JWTTokenNeeded
    public List<ServiceTypeDTO> getServiceTypes(@Context HttpServletRequest request) {
        Reseller reseller = resellerFetcher.getReseller(request);

        List<ServiceTypeDTO> serviceTypes = new ArrayList<>();
        for (final SubscriptionServiceType serviceType : serviceTypePersistenceFacade.findAllforReseller()) {
            if (serviceType.getProvider() == null ||
                    reseller.getProvider().getId() == serviceType.getProvider().getId()) {
                serviceTypes.add(new ServiceTypeDTO(serviceType.getId(), serviceType.getName()));
            }
        }
        return serviceTypes;
    }

    public static class RegionDTO {
        public long id;
        public String name;
        public String ip;

        RegionDTO() {
        }

        RegionDTO(long id, String name, String ip) {
            this.id = id;
            this.name = name;
            this.ip = ip;
        }
    }






    @GET
    @Path("regions")
    @JWTTokenNeeded
    public List<RegionDTO> getRegions(@Context HttpServletRequest request) {
        List<RegionDTO> regions = new ArrayList<>();

        Reseller reseller = resellerFetcher.getReseller(request);


        for (final MiniPop miniPop : reseller.getMinipops()) {
            regions.add(new RegionDTO(miniPop.getId(), miniPop.getSwitch_id(), miniPop.getIp()));
        }
        return regions;
    }
}
