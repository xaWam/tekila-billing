package spring.controller;

import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import spring.Filters;
import spring.dto.EquipmentResponse;
import spring.dto.IpResponse;
import spring.dto.MinipopResponse;

import javax.ejb.EJB;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * Created by KamranMa on 25.12.2017.
 */
@RestController
public class StockController {


    @EJB(mappedName = INJECTION_POINT + "EquipmentPersistenceFacade")
    private EquipmentPersistenceFacade equipmentPersistenceFacade;



    private static final Logger log = Logger.getLogger(StockController.class);

    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPopPersistenceFacade;

    @RequestMapping(method = RequestMethod.GET, value = "/search/equipments")
    public List<EquipmentResponse> searchEquipments(
            @RequestParam(required = false) String partNumber,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. partnumber = %s, pageId = %d, rowsPerPage = %d",
                partNumber, pageId, rowsPerPage));

        if (partNumber != null && !partNumber.isEmpty()) {
            SubscriptionPersistenceFacade.Filter equipmentPartNumberFilter = SubscriptionPersistenceFacade.Filter.EQUIPMENT_PARTNUMBER;
            equipmentPartNumberFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(equipmentPartNumberFilter, partNumber);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.SETTING_TYPE, ServiceSettingType.TV_EQUIPMENT);
        }

        log.debug("-----------search end()----------");
        return subscriptionPersistenceFacade.findAllPaginated((pageId-1)*rowsPerPage, rowsPerPage, filters).stream().
                map(EquipmentResponse::from).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/equipments/count")
    public long countEquipments(
            @RequestParam(required = false) String partNumber
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. partnumber = %s",
                partNumber));

        if (partNumber != null && !partNumber.isEmpty()) {
            SubscriptionPersistenceFacade.Filter equipmentPartNumberFilter = SubscriptionPersistenceFacade.Filter.EQUIPMENT_PARTNUMBER;
            equipmentPartNumberFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(equipmentPartNumberFilter, partNumber);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.SETTING_TYPE, ServiceSettingType.TV_EQUIPMENT);
        }

        long cnt = subscriptionPersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }







    @RequestMapping(method = RequestMethod.GET, value = "/search/equipmentsbyprovider")
    public List<Equipment> searchEquipmentsByProvider(
            @RequestParam(required = false) Long providerId,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage,
            @RequestParam(required = false, defaultValue = "") String partName ){
        log.debug("-----------search() start----------");

        List<Equipment> equipments = null;

        if(pageId > 0 & rowsPerPage > 0) {
            equipments = equipmentPersistenceFacade.findByProviderIdWithPage(providerId, (pageId-1) * rowsPerPage+1, pageId * rowsPerPage, partName);

        }else{
            equipments = equipmentPersistenceFacade.findByEquipmentsByProviderId(providerId);

        }
log.debug("Equipments are "+equipments);

return equipments;
    }




    @RequestMapping(method = RequestMethod.GET, value = "/search/equipmentsbyproviderCount")
    public BigDecimal searchEquipmentsByProviderC(
            @RequestParam(required = false) Long providerId) {


        BigDecimal equipmentCount = equipmentPersistenceFacade.findByProviderId(providerId);

        log.debug("Equipments size "+equipmentCount);

        return equipmentCount;
    }



    @RequestMapping(method = RequestMethod.GET, value = "/search/ips")
    public List<IpResponse> searchIPs(
            @RequestParam(required = false) String ip,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. ip = %s, pageId = %d, rowsPerPage = %d",
                ip, pageId, rowsPerPage));

        List<IpResponse> ipList;
        if (ip != null && !ip.isEmpty()) {
            SubscriptionPersistenceFacade.Filter ipFilter = SubscriptionPersistenceFacade.Filter.IP;
            ipFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(ipFilter, ip);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.BUCKET_TYPE, ResourceBucketType.INTERNET_IP_ADDRESS);
            String sqlQuery = subscriptionPersistenceFacade.findSubscriptionByIp(filters.getFilters());
            ipList = subscriptionPersistenceFacade.findAllPaginatedDynamic((pageId-1)*rowsPerPage, rowsPerPage, sqlQuery, filters).stream().
                    map(s -> IpResponse.from(s, ip)).collect(Collectors.toList());
        } else {
            ipList = subscriptionPersistenceFacade.findAllPaginated((pageId-1)*rowsPerPage, rowsPerPage, new Filters()).stream().
                    map(s -> IpResponse.from(s, "")).collect(Collectors.toList());
        }

        log.debug("-----------search end()----------");
        return ipList;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/ips/count")
    public long countIPs(
            @RequestParam(required = false) String ip
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. ip = %s",
                ip));

        String sqlQuery;
        long cnt;
        if (ip != null && !ip.isEmpty()) {
            SubscriptionPersistenceFacade.Filter ipFilter = SubscriptionPersistenceFacade.Filter.IP;
            ipFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(ipFilter, ip);
            filters.addFilter(SubscriptionPersistenceFacade.Filter.BUCKET_TYPE, ResourceBucketType.INTERNET_IP_ADDRESS);
            sqlQuery = subscriptionPersistenceFacade.findSubscriptionByIp(filters.getFilters());
            sqlQuery = sqlQuery.replaceAll("distinct vas.subscription", "count(distinct vas.subscription.id)");
            cnt = subscriptionPersistenceFacade.countDynamic(sqlQuery, filters);
        } else {
            cnt = subscriptionPersistenceFacade.count(new Filters());
        }

        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/minipops")
    public List<MinipopResponse> searchMinipops(
            @RequestParam(required = false) String macAddress,
            @RequestParam(required = false) String minipopAddress,
            @RequestParam(required = false) String minipopSwitchName,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. macAddress = %s, minipopAddress = %s, minipopSwitchName = %s, pageId = %d, rowsPerPage = %d",
                macAddress, minipopAddress, minipopSwitchName, pageId, rowsPerPage));

        if (macAddress != null && !macAddress.isEmpty()) {
            MiniPopPersistenceFacade.Filter macAddressFilter = MiniPopPersistenceFacade.Filter.MAC_ADDRESS;
            macAddressFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(macAddressFilter, macAddress);
        }

        if (minipopAddress != null && !minipopAddress.isEmpty()) {
            MiniPopPersistenceFacade.Filter addressFilter = MiniPopPersistenceFacade.Filter.ADDRESS;
            addressFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(addressFilter, minipopAddress);
        }

        if (minipopSwitchName != null && !minipopSwitchName.isEmpty()) {
            MiniPopPersistenceFacade.Filter switchIdFilter = MiniPopPersistenceFacade.Filter.SWITCH_ID;
            switchIdFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(switchIdFilter, minipopSwitchName);
        }

        log.debug("-----------search end()----------");
        return miniPopPersistenceFacade.findAllPaginated((pageId-1)*rowsPerPage, rowsPerPage, filters).stream().
                map(MinipopResponse::from).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/minipops/count")
    public long countMinipops(
            @RequestParam(required = false) String macAddress,
            @RequestParam(required = false) String minipopAddress,
            @RequestParam(required = false) String minipopSwitchName
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. macAddress = %s, minipopAddress = %s, minipopSwitchName = %s",
                macAddress, minipopAddress, minipopSwitchName));

        if (macAddress != null && !macAddress.isEmpty()) {
            MiniPopPersistenceFacade.Filter macAddressFilter = MiniPopPersistenceFacade.Filter.MAC_ADDRESS;
            macAddressFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(macAddressFilter, macAddress);
        }

        if (minipopAddress != null && !minipopAddress.isEmpty()) {
            MiniPopPersistenceFacade.Filter addressFilter = MiniPopPersistenceFacade.Filter.ADDRESS;
            addressFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(addressFilter, minipopAddress);
        }

        if (minipopSwitchName != null && !minipopSwitchName.isEmpty()) {
            MiniPopPersistenceFacade.Filter switchIdFilter = MiniPopPersistenceFacade.Filter.SWITCH_ID;
            switchIdFilter.setOperation(MatchingOperation.LIKE);
            filters.addFilter(switchIdFilter, minipopSwitchName);
        }

        long cnt = miniPopPersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }
}
