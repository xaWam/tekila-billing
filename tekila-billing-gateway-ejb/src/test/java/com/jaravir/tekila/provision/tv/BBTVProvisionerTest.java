package com.jaravir.tekila.provision.tv;

import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BBTVProvisionerTest {
    private static BBTVProvisioner provisioner;
    private static String partNumber;
    private static Subscription subscription;

    @BeforeClass
    public static void init () {
        provisioner = new BBTVProvisioner();
        //partNumber = "N 8060 0009 94 0";

        //partNumber = "N 8060 0013 24 9";
        //partNumber = "N 8060 0005 82 3";
        //partNumber = "N 8060 0010 50 0";
        //partNumber = "8060001328";
        //partNumber = "N 8060 0013 28 0";
        //partNumber = "N 8060 0003 92 7";
        //partNumber = "N 8060 0010 12 0";
        partNumber = "N 8060 0008 41 3";
        ServiceSetting srvSetting = new ServiceSetting();
        srvSetting.setServiceType(ServiceType.TV);
        srvSetting.setType(ServiceSettingType.TV_EQUIPMENT);

        SubscriptionSetting sbnSetting = new SubscriptionSetting();
        sbnSetting.setProperties(srvSetting);
        sbnSetting.setValue(partNumber);

        subscription = new Subscription();
        //subscription.setAgreement("987654321");
        //subscription.setAgreement("100000000");
        //subscription.setAgreement("100000001");
        //subscription.setAgreement("1111111111");
        //subscription.setAgreement("92000038");
        subscription.setAgreement("92000030");
        subscription.addSetting(sbnSetting);
    }

    @Ignore
    @Test
    public void testProcessPartNumber() throws Exception {
        String res = provisioner.processPartNumber(partNumber);
        assertTrue("actual length: " + res.length(), res.length() == 10);
        assertEquals("8060000994", res);
    }

    @Ignore
    @Test
    public void testInitService () {
        boolean res = provisioner.initService(subscription);
        assertTrue(res);
    }

    @Ignore
    @Test
    public void testAddEntitlement () {
        boolean res = provisioner.openService(subscription);
        assertTrue(res);
    }

    @Ignore
    @Test
    public void testRemoveEntitlement () {
        boolean res = provisioner.closeService(subscription);
        assertTrue(res);
    }

    @Ignore
    @Test
    public void testCollectExternalStatusInformation () {
        ExternalStatusInformation stats = provisioner.collectExternalStatusInformation(subscription);
        assertNotNull(stats);
    }

    @Ignore
    @Test
    public void testClose() {
        try (BufferedReader br = new BufferedReader(new FileReader("D:/eq.txt"))) {
            String line = null;
            List<Subscription> sbnList = new ArrayList<>();
            String[] strAr = null;
            boolean res = false;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                strAr = line.split("\\s");
                assertNotNull(strAr);
                assertEquals(2, strAr.length);
                System.out.println("Parsed line: " + Arrays.toString(strAr));

                subscription.setAgreement(strAr[0]);
                subscription.setSettingByType(ServiceSettingType.TV_EQUIPMENT, strAr[1]);
                res = provisioner.closeService(subscription);
                System.out.println(String.format("agreement=%s, equipment=%s, result=%s",
                        subscription.getAgreement(), subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue(), res));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}