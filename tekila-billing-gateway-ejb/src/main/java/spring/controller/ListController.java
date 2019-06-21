package spring.controller;

import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.subscription.persistence.entity.PaymentTypes;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import spring.dto.*;
import spring.service.ListService;

import javax.faces.model.SelectItem;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by KamranMa, MusaAl on 13.12.2017.
 */
@RestController
public class ListController {

    private static final Logger log = LoggerFactory.getLogger(ListController.class);

    private final ListService listService;

    public ListController(ListService listService) {
        this.listService = listService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list/status")
    public List<SubscriptionStatusResponse> getStatusList() {
        return listService.getStatusList();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list/provider")
    public List<ServiceProviderResponse> getProviderList() {
        return listService.getProviderList();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list/billing-principles")
    public List<BillingPrinciplesResponse> getBillingPrinclipesList() {
        return listService.getBillingPrinclipesList();
    }

    @RequestMapping(method = RequestMethod.GET,value = "/list/account-categories")
    public List<AccountingCategoryDTO> getAccountingCategories() {
        return listService.getAccountingCategories();
    }


    @RequestMapping(method = RequestMethod.GET,value = "/list/services")
    public List<ServiceSelectDTO> getServices() {
        return listService.getServices();
    }

    @RequestMapping(method = RequestMethod.GET,value = "/list/providers/{provider_id}/services")
    public List<ServiceSelectDTO> getServices(@PathVariable("provider_id") Long provider_id) {
        return listService.getServices(provider_id);
    }

    @RequestMapping(method = RequestMethod.GET,value = "/list/service-types")
    public List<ServiceTypeSelectDTO> getServiceTypes() {
        return listService.getServiceTypes();
    }

    @RequestMapping(method = RequestMethod.GET,value = "/list/zones")
    public List<ZoneSelectDTO> getZones() {
        return listService.getZones();
    }

    @GetMapping("/list/services/{service_id}/zones")
    public List<ZoneSelectDTO> getServiceZones(@PathVariable("service_id") Long serviceId) {
        return listService.getServiceZones(serviceId);
    }
    @GetMapping("/list/services2/{service_id}/zones")
    public List<ZoneSelectDTO> getServiceZones2(@PathVariable("service_id") Long serviceId) {
        return listService.getServiceZones2(serviceId);
    }
    @GetMapping("/list/services3/{service_id}/zones")
    public List<ZoneSelectDTO> getServiceZones3(@PathVariable("service_id") Long serviceId) {
        return listService.getServiceZones3(serviceId);
    }
    @GetMapping("/list/services4/{service_id}/zones")
    public List<ZoneSelectDTO> getServiceZones4(@PathVariable("service_id") Long serviceId) {
        return listService.getServiceZones4(serviceId);
    }

    @GetMapping("/list/ats")
    public List<AtsDTO> getAllAts() {
        return listService.getAllAts();
    }

    @GetMapping("/list/providers/{providerId}/ats")
    public List<AtsDTO> getAllAtsByProvider(@PathVariable Long providerId) {
        log.info("Rest request for retrieving Ats by provider id : {}", providerId);
        return listService.getAllAtsByProvider(providerId);
    }

    /**
     *  useful for CityNet... because of data inconsistency this is not clear what kind of
     *  ATSes fetch for CityNet.
     * */
    @GetMapping("/list/providers/dandig/ats")
    public List<AtsDTO> getAllAtsByDandigCriteria(){
        log.info("Rest request for retrieving Ats by dandig criteria...");
        return listService.getAllAtsByDandigCase();
    }

    @GetMapping("/list/ats/{ats_id}/minipops")
    public List<MinipopDropdownDTO> getAtsMinipops(@PathVariable("ats_id") Long atsId){
        return listService.getAtsMinipops(atsId);
    }

    @GetMapping("/list/minipops/{minipop_id}/ports")
    public List<PortDTO> getMinipopPorts(@PathVariable("minipop_id") Long minipopId){
        return listService.getMinipopPorts(minipopId);
    }

    @GetMapping("/list/resellers")
    public List<ResellerDropdownDTO> getAllResellers(){
        return listService.findAllReseller();
    }

    @GetMapping("/list/resellers/{provider_id}")
    public List<ResellerDropdownDTO> getAllResellersByProvider(@PathVariable("provider_id") Long providerId){
        return listService.findAllResellerByProvider(providerId);
    }

    @GetMapping("/list/vasTypeList")
    public List<SelectItem> getSubscVASTypeList(){
        return listService.getSbnVASTypeList();
    }

    @GetMapping("/list/vasStatusList")
    public List<SelectItem> getSubscVasStatusList(){
        return listService.getSbnVasStatusList();
    }

//    @RequestMapping(method = RequestMethod.GET,value = "/list/payment-types")
//    public List<PaymentTypesResponse> getPaymentTypes() {
//        return listService.getPaymentTypes();
//    }

    @RequestMapping(method = RequestMethod.GET,value = "/list/payment-types")
    public List<PaymentTypes> getPaymentTypes() {
        return listService.getPaymentTypes();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list/billing-principles2")
    public List<BillingPrinciple> getBillingPrinclipesList2() {
        return listService.getBillingPrinclipesList2();
    }

    @GetMapping("/list/dealers")
    public List<DealerDropdownDTO> getAllDealers(){
        return listService.findAllDealer();
    }


//    @GetMapping("/list/allowedVasList/{subscriptionID}")
//    public List<ValueAddedService> getSubcAllowedVasList(@PathVariable Long subscriptionID){
//        return listService.getAllowedVasList(subscriptionID);
//    }

//    @GetMapping("/list/ats/{atsIndex}/streets")
//    public List<StreetDTO> getStreetsByAts(@PathVariable String atsIndex){
//        log.info("Rest request for retrieving Streets by ATS, ats index : {}", atsIndex);
//        return listService.getStreetsByAts(atsIndex);
//    }

    @GetMapping("/list/ats/{atsIndex}/streets")
    public List<StreetDTO> getStreetsByAtsId(@PathVariable Long atsIndex){
        log.info("Rest request for retrieving Streets by ATS, ats index : {}", atsIndex);
        return listService.getStreetsByAts(atsIndex);
    }



}
