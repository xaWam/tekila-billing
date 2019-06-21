package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.tv.BBTVProvisioner;
import com.jaravir.tekila.test.prepare.InternetProducer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionPersistenceFacadeTest {
    @InjectMocks
    private SubscriptionPersistenceFacade sbnFacade;
    @Mock
    private SystemLogger systemLogger;
    @Mock
    private EquipmentPersistenceFacade equipmentFacade;
    @Mock
    private EngineFactory provisioningFactory;
    @Mock
    private MiniPopPersistenceFacade minipopFacade;

    private String partNumber;
    private BBTVProvisioner provisioner;
    private Subscription subscription;
    private Equipment equipment;
    private InternetProducer internetPreTest;

    @Before
    public void init() {
        internetPreTest = new InternetProducer();

        provisioner = mock(BBTVProvisioner.class);
        //partNumber = "N 8060 0009 94 0";

        //partNumber = "N 8060 0013 24 9";
        //partNumber = "N 8060 0005 82 3";
        //partNumber = "N 8060 0013 28 0";
        ServiceSetting srvSetting = new ServiceSetting();
        srvSetting.setServiceType(ServiceType.TV);
        srvSetting.setType(ServiceSettingType.TV_EQUIPMENT);

        SubscriptionSetting sbnSetting = new SubscriptionSetting();
        sbnSetting.setProperties(srvSetting);
        sbnSetting.setValue(partNumber);

        ServiceProvider provider = new ServiceProvider();
        provider.setId(454101);
        provider.setName("test provider");

        Service service = new Service();
        service.setName("test bbtv");
        service.setId(1);
        service.setProvider(provider);

        subscription = new Subscription();
        //subscription.setAgreement("987654321");
        //subscription.setAgreement("100000000");
        //subscription.setAgreement("1111111111");
        subscription.addSetting(sbnSetting);
        subscription.setId(1);
        subscription.setService(service);

        equipment = new Equipment();
        equipment.setId(1);
        equipment.setPartNumber(partNumber);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
    }

    @Ignore
    @Test
    public void testChangeEquipment() throws Exception {
        Equipment oldEquipment = new Equipment();
        oldEquipment.setId(5);
        oldEquipment.setPartNumber("9999999999");
        oldEquipment.setStatus(EquipmentStatus.RESERVED);

        when(provisioner.openService(subscription)).thenCallRealMethod();
        when(provisioner.initService(subscription)).thenCallRealMethod();
        when(provisioner.processPartNumber(anyString())).thenCallRealMethod();
        when(provisioner.initService(subscription)).thenCallRealMethod();

        when(provisioningFactory.getProvisioningEngine(subscription)).thenReturn(provisioner);
        when(equipmentFacade.findByPartNumber(anyString())).thenReturn(equipment);

        assertNotNull(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT));
        assertNotNull(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue());
        assertNotNull(equipment);
        assertNotNull(equipment.getPartNumber());
        sbnFacade.changeEquipmentForTV(subscription, equipment);

        verify(provisioner).changeEquipment(eq(subscription), anyString());
        verify(provisioner).openService(subscription);
        verify(systemLogger).success(eq(SystemEvent.PROVISIONING), eq(subscription), anyString());
        assertTrue(subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue().equals(equipment.getPartNumber()));
        assertEquals(equipment.getStatus(), EquipmentStatus.RESERVED);
        //assertEquals(oldEquipment.getStatus(), EquipmentStatus.AVAILABLE);
    }

    @Ignore
    @Test
    public void testChangeMinipop() throws Exception {
        Port port = new Port();
        port.setId(1);
        port.setNumber(1);

        MiniPop newMiniPop = new MiniPop();
        newMiniPop.setId(20);
        newMiniPop.setDeviceStatus(DeviceStatus.ACTIVE);
        newMiniPop.setNumberOfPorts(24);
        newMiniPop.setMac("test:test:test:test");

        subscription = internetPreTest.getSubscription();
        when(minipopFacade.getAvailablePort(any(MiniPop.class))).thenReturn(port);
        when(minipopFacade.find(anyLong())).thenReturn(newMiniPop);

        ProvisioningEngine internetProvisioner = mock(ProvisioningEngine.class);
        when(provisioningFactory.getProvisioningEngine(any(Subscription.class))).thenReturn(internetProvisioner);

        sbnFacade.changeMinipop(subscription, newMiniPop);
        assertEquals(String.valueOf(newMiniPop.getId()), subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue());
        assertEquals(String.valueOf(port.getNumber()), subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).getValue());
//        assertEquals(String.format("Ethernet0/0/%d:%s", port.getNumber(), newMiniPop.getMac()), subscription.getSettingByType(ServiceSettingType.USERNAME).getValue());
        verify(internetProvisioner).changeEquipment(eq(subscription), anyString());
    }

    @Ignore
    @Test
    public void testChangeMinipopNoSwitch() throws Exception {
        Port port = new Port();
        port.setId(1);
        port.setNumber(1);

        MiniPop newMiniPop = new MiniPop();
        newMiniPop.setId(20);
        newMiniPop.setDeviceStatus(DeviceStatus.ACTIVE);
        newMiniPop.setNumberOfPorts(24);
        newMiniPop.setMac("test:test:test:test");

        subscription = internetPreTest.getSubscription();
        when(minipopFacade.getAvailablePort(any(MiniPop.class))).thenReturn(port);
        when(minipopFacade.find(anyLong())).thenReturn(newMiniPop);

        ProvisioningEngine internetProvisioner = mock(ProvisioningEngine.class);
        when(provisioningFactory.getProvisioningEngine(any(Subscription.class))).thenReturn(internetProvisioner);

        subscription.setSettingByType(ServiceSettingType.BROADBAND_SWITCH, "-");
        sbnFacade.changeMinipop(subscription, newMiniPop);
        assertEquals(String.valueOf(newMiniPop.getId()), subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue());
        assertEquals(String.valueOf(port.getNumber()), subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).getValue());
//        assertEquals(String.format("Ethernet0/0/%d:%s", port.getNumber(), newMiniPop.getMac()), subscription.getSettingByType(ServiceSettingType.USERNAME).getValue());
        verify(internetProvisioner).changeEquipment(eq(subscription), anyString());
    }

    @Test
    public void synchronizeSubscription() {
        System.out.println("Starting to test synchronizeSubscription method..");
        //It is impossible to test according to persistence...
//        BackProvisionDetails bpd = new BackProvisionDetails("switchname", 100L, 3131L, "testmac", "123456789");
////        Subscription sub = sbnFacade.find(25436780);
//        Subscription sub = new Subscription();
//        Subscriber subscriber = new Subscriber();
//        sub.setSubscriber(subscriber);
//        System.out.println("Test before synchronize -> "+sub);
//        sbnFacade.synchronizeSubscription(bpd, sub);
//        System.out.println("Test after synchronize -> "+sub);

    }
}