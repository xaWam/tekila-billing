package com.jaravir.tekila.test.prepare;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import org.joda.time.DateTime;

import java.util.Arrays;

/**
 * Created by sajabrayilov on 11/7/2015.
 */
public class InternetProducer {
    private Subscription subscription;
    private MiniPop miniPop;
    private Port port;

    public InternetProducer () {
        ServiceProvider provider = new ServiceProvider();
        provider.setId(454100);

        /*
        * Rate rate = new Rate();
        rate.setName("test rate");
        rate.setPrice(10);
        rate.setActiveFrom(DateTime.now().minusYears(5));
        rate.setActiveTill(DateTime.now().plusYears(5));
        rate.setIsUsePromoResources(true);

        RateProfile profile = new RateProfile();
        profile.setName("test profile");
        profile.setId(1);
        profile.setRateList(Arrays.asList(rate));
        * */

        ServiceSetting passwordSetting = new ServiceSetting();
        passwordSetting.setServiceType(ServiceType.BROADBAND);
        passwordSetting.setType(ServiceSettingType.PASSWORD);
        passwordSetting.setProvider(provider);
        passwordSetting.setTitle("Password");

        ServiceSetting usernameSetting = new ServiceSetting();
        usernameSetting.setServiceType(ServiceType.BROADBAND);
        usernameSetting.setType(ServiceSettingType.USERNAME);
        usernameSetting.setProvider(provider);
        usernameSetting.setTitle("Username");

        ServiceSetting switchSetting = new ServiceSetting();
        switchSetting.setServiceType(ServiceType.BROADBAND);
        switchSetting.setType(ServiceSettingType.BROADBAND_SWITCH);
        switchSetting.setProvider(provider);
        switchSetting.setTitle("Switch");

        ServiceSetting portSetting = new ServiceSetting();
        portSetting.setServiceType(ServiceType.BROADBAND);
        portSetting.setType(ServiceSettingType.BROADBAND_SWITCH_PORT);
        portSetting.setProvider(provider);
        portSetting.setTitle("Port");

        ResourceBucket upBucket = new ResourceBucket();
        upBucket.setId(1);
        upBucket.setType(ResourceBucketType.INTERNET_UP);
        upBucket.setCapacity("10");

        ResourceBucket downBucket = new ResourceBucket();
        downBucket.setId(2);
        downBucket.setType(ResourceBucketType.INTERNET_DOWN);
        downBucket.setCapacity("10");

        Resource resource = new Resource();
        resource.setName("test resource");
        resource.setId(1);
        resource.setExpirationDate(DateTime.now().plusYears(5));
        resource.setBucketList(Arrays.asList(downBucket,upBucket));

        Service service = new Service();
        service.setName("Test internet");
        service.setServiceType(ServiceType.BROADBAND);
        service.setProvider(provider);
//        service.setRateProfile(profile);
        service.setSettings(Arrays.asList(usernameSetting, passwordSetting, switchSetting, portSetting));
        service.setResourceList(Arrays.asList(resource));
        service.setId(1);

        Subscriber subscriber = new Subscriber();
        subscriber.setId(1);
        subscriber.setCreationDate(DateTime.now().minusYears(5).toDate());
        subscriber.setFnCategory(SubscriberFunctionalCategory.TEST);

        Balance balance = new Balance();
        balance.setId(1);
        balance.setLastUpdateDate();

        port = new Port();
        port.setId(1);
        port.setNumber(1);
        port.setIsOccupied(true);

        miniPop = new MiniPop();
        miniPop.setId(1);
        miniPop.setDeviceStatus(DeviceStatus.ACTIVE);
        miniPop.setNumberOfPorts(24);
        miniPop.setMac("test:test:test:test");
        miniPop.setReservedPortList(Arrays.asList(port));

        subscription = new Subscription();
        subscription.setAgreement("91000001");
        subscription.setService(service);
        subscription.setSubscriber(subscriber);
        subscription.setActivationDate(DateTime.now().minusMonths(3));
        subscription.setCreationDate(subscriber.getCreationDate());
        subscription.setBilledUpToDate(DateTime.now().plusMonths(1));
        subscription.setExpirationDate(subscription.getBilledUpToDate());
        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate());
        subscription.copySettingsFromService(service.getSettings());
        subscription.setResources(Arrays.asList(new SubscriptionResource(resource)));
        subscription.setBalance(balance);
        subscription.setSettingByType(ServiceSettingType.BROADBAND_SWITCH, miniPop.getMac());
        subscription.setSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT, String.valueOf(port.getNumber()));
        subscription.setSettingByType(ServiceSettingType.USERNAME,
                String.format("Ethernet0/0/%d:%s", port.getNumber(), miniPop.getMac()));
        subscription.setSettingByType(ServiceSettingType.PASSWORD, "-");
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public MiniPop getMiniPop() {
        return miniPop;
    }

    public void setMiniPop(MiniPop miniPop) {
        this.miniPop = miniPop;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }
}
