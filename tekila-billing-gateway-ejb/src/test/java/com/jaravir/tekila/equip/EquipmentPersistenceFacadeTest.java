package com.jaravir.tekila.equip;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerInvoice;
import com.jaravir.tekila.module.accounting.manager.SalesPartnerChargePersitenceFacade;
import com.jaravir.tekila.module.accounting.manager.SalesPartnerInvoicePersistenceFacade;
import com.jaravir.tekila.module.sales.persistence.entity.SalesPartner;
import com.jaravir.tekila.module.store.SalesPartnerStore;
import com.jaravir.tekila.module.store.SalesPartnerStorePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.equip.price.EquipmentPrice;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import javax.ejb.EJBContext;
import javax.persistence.EntityManager;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EquipmentPersistenceFacadeTest {
    @Mock private EJBContext ctx;
    @Mock private SalesPartnerChargePersitenceFacade partnerChargeFacade;
    @Mock private SalesPartnerInvoicePersistenceFacade partnerInvoiceFacade;
    @Mock private UserPersistenceFacade userFacade;
    @Mock private SystemLogger systemLogger;
    @Mock private EntityManager entityManager;
    @Mock private SalesPartnerStorePersistenceFacade partnerStoreFacade;

    @InjectMocks private EquipmentPersistenceFacade equipmentFacade;
    private List<Equipment> equipmentList;

    @Before
    public void prepare () {
        when(ctx.getCallerPrincipal()).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "system";
            }
        });

        when(userFacade.findByUserName(anyString())).thenReturn(new User());
        when(entityManager.merge(any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return args[0];
            }
        });

        Equipment equipment= null;

        EquipmentPrice price = new EquipmentPrice();
        price.setPrice(100);

        equipmentList = new ArrayList<>();

        for (int i = 0;i < 2; i++) {
            equipment = new Equipment();
            equipment.setId(10L);
            equipment.setPartNumber(DateTime.now().toString(DateTimeFormat.forPattern("yyyMMddHHmmss")));
            equipment.setPrice(price);
            equipment.setStatus(EquipmentStatus.AVAILABLE);

            equipmentList.add(equipment);
        }
    }

    @Test
    public void testTransfer() throws Exception {
        SalesPartner salesPartner = new SalesPartner();
        salesPartner.setId(1L);
        SalesPartnerStore partnerStore = new SalesPartnerStore();
        partnerStore.setName("test store");
        partnerStore.setOwner(salesPartner);
        Double transferCharge = 23.00;

        when(partnerStoreFacade.update(any(SalesPartnerStore.class))).thenAnswer(new Answer<SalesPartnerStore>() {
            @Override
            public SalesPartnerStore answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (SalesPartnerStore) args[0];
            }
        });

        SalesPartnerInvoice invoice = equipmentFacade.transfer(equipmentList, partnerStore, transferCharge);

        assertTrue(invoice.getCharges().size() > 0);
        assertTrue(invoice.getBalance() < 0);
        assertEquals(InvoiceState.OPEN, invoice.getState());
        assertNotNull(invoice.getPartner());
        assertEquals(salesPartner.getId(), invoice.getPartner().getId());
        assertTrue(partnerStore.getEquipmentList().size() > 0);
        assertEquals(2, partnerStore.getEquipmentList().size());

        for (Equipment equip : equipmentList) {
            assertEquals(EquipmentStatus.TRANSFERED, equip.getStatus());
        }

        verify(systemLogger, atLeast(3)).success(any(SystemEvent.class), any(Subscription.class), anyString());
    }

    @After
    public void tearUp () {
        equipmentList.clear();
    }
}