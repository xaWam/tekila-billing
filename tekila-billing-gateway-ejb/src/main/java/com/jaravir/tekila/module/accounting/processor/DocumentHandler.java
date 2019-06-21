/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.processor;

import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.UserRow;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.engines.CommonOperationsEngine;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.equip.EquipmentBrandPersistenceFacade;
import com.jaravir.tekila.equip.EquipmentModelPersistenceFacade;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.equip.EquipmentTypePersistenceFacade;
import com.jaravir.tekila.equip.price.EquipmentPricePersistenceFactory;
import com.jaravir.tekila.module.accounting.AccountingCategoryType;
import com.jaravir.tekila.module.accounting.AccountingTransactionType;
import com.jaravir.tekila.module.accounting.PaymentPurpose;
import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.AccountingTransaction;
import com.jaravir.tekila.module.accounting.entity.Bank;
import com.jaravir.tekila.module.accounting.entity.Operation;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.BankPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.OperationPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.record.PaymentDocumentRecord;
import com.jaravir.tekila.module.auth.GroupPersistenceFacade;
import com.jaravir.tekila.module.auth.security.PasswordGenerator;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.store.equip.*;
import com.jaravir.tekila.module.store.equip.price.EquipmentPrice;
import com.jaravir.tekila.module.store.mobile.*;
import com.jaravir.tekila.module.store.mobile.record.ImsiDocumentRecord;
import com.jaravir.tekila.module.store.mobile.record.MsisdnDocumentRecord;
import com.jaravir.tekila.module.store.status.StoreItemStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.ejb.*;

import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.MinipopDocumentRecord;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.exception.DuplicateEntityException;
import com.jaravir.tekila.tools.WrapperAgreementChangeBatch;
import com.jaravir.tekila.tools.WrapperBatch;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author sajabrayilov
 */
@Stateless
public class DocumentHandler {
    @Resource
    private SessionContext ctx;
//    private EJBContext ctx;

    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private BankPersistenceFacade bankFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private AccountingCategoryPersistenceFacade accCatFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB
    private OperationPersistenceFacade operationFacade;
    @EJB
    private EquipmentPersistenceFacade equipmentFacade;
    @EJB
    private EquipmentModelPersistenceFacade equipmentModelPersistenceFacade;
    @EJB
    private EquipmentBrandPersistenceFacade equipmentBrandPersistenceFacade;
    @EJB
    private EquipmentTypePersistenceFacade equipmentTypePersistenceFacade;
    @EJB
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;
    @EJB
    private EquipmentPricePersistenceFactory pricePersistenceFactory;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private GroupPersistenceFacade groupFacade;
    @EJB
    private PasswordGenerator paswordGenerator;
    @EJB
    private ImsiPersistenceFacade imsiFacade;
    @EJB
    private MsidnPersistenceFacade msisdnFacade;
    @EJB
    private CommercialCategoryPersistenceFacade comCategoryFacade;
    @EJB
    private MiniPopPersistenceFacade minipopFacade;
    @EJB
    private PaymentOptionsPersistenceFacade paymentOptionsPersistenceFacade;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    private ProvisioningEngine provisioner;

    private static final Logger log = Logger.getLogger(DocumentHandler.class);

    public void parsePaymentFile(InputStream inputStream, User user, PaymentPurpose purpose, String dsc) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<PaymentDocumentRecord> recordList = new ArrayList<>();
            PaymentDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new PaymentDocumentRecord();


                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();

                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                currentRecord.setBankID(Double.valueOf(cell.getNumericCellValue()).longValue());
                                log.debug(String.format("{%s} ", currentRecord.getBankID()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setAgreement(cell.getStringCellValue());
                            else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                                currentRecord.setAgreement(String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue()));
                            System.out.print(String.format("{%s} ", currentRecord.getAgreement()));
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                currentRecord.setDoubleAmount(cell.getNumericCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getLongAmount()));
                            }
                            break;
                    }
                    /*
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:    
                            currentRecord.setAgreement(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getAgreement()));
                        break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (columnCounter == 1) {
                                currentRecord.setBankID(Double.valueOf(cell.getNumericCellValue()).longValue());
                                log.debug(String.format("{%s} ", currentRecord.getBankID()));                                
                            }
                            else if (columnCounter == 3) {
                                currentRecord.setDoubleAmount(cell.getNumericCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getLongAmount()));                                
                            }
                        break;
                        default:
                            log.debug("N/A");
                    }
                    */
                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                recordList.add(currentRecord);
                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS     

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doBatchPayments(recordList, purpose, user, dsc);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    public void parseAdjustmentFile(InputStream inputStream, User user, TransactionType transactionType, String desc) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<PaymentDocumentRecord> recordList = new ArrayList<>();
            PaymentDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();

                currentRecord = new PaymentDocumentRecord();

                cellIterator = row.iterator();

                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                currentRecord.setBankID(Double.valueOf(cell.getNumericCellValue()).longValue());
                                log.debug(String.format("{%s} ", currentRecord.getBankID()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setAgreement(cell.getStringCellValue());
                            else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                                currentRecord.setAgreement(String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue()));
                            System.out.print(String.format("{%s} ", currentRecord.getAgreement()));
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                currentRecord.setAmount(cell.getNumericCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getLongAmount()));
                            }
                            break;

                        default:
                            log.debug("N/A");
                    }
                    /*switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            currentRecord.setAgreement(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getAgreement()));
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (columnCounter == 1) {
                                currentRecord.setBankID(Double.valueOf(cell.getNumericCellValue()).longValue());
                                log.debug(String.format("{%s} ", currentRecord.getBankID()));
                            }
                            else if (columnCounter == 3) {
                                currentRecord.setAmount(cell.getNumericCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getLongAmount()));
                            }
                            break;
                        default:
                            log.debug("N/A");
                    }*/

                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                recordList.add(currentRecord);
                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doBatchAdjustments(recordList, transactionType, user, desc);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    private void doBatchPayments(List<PaymentDocumentRecord> paymentRecordList, PaymentPurpose purpose, User user, String desc) {
        if (paymentRecordList == null || paymentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        List<Payment> paymentList = new ArrayList<>();
        Payment payment = null;
        Bank bank = null;
        Subscription subscription = null;

        AccountingCategory cat = accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT);
        AccountingTransaction accTrans = new AccountingTransaction();
        accTrans.setDsc(desc);
        AccountingTransactionType accTransType = purpose == PaymentPurpose.VAT ? AccountingTransactionType.BATCH_PAYMENT_VAT : AccountingTransactionType.BATCH_PAYMENT;
        accTrans.setType(accTransType);
        accTrans.setUser(user);

        Operation operation = null;
        Transaction trans = null;

        for (PaymentDocumentRecord record : paymentRecordList) {
            try {
                bank = bankFacade.find(record.getBankID());
                /* TODO: add unprocessed records if bank or payment is incorrect
                 * AccoutingTransaction: List<UnprocessedRecords>
                 */
                subscription = subscriptionFacade.findByCustomerIdentifier(record.getAgreement());

                payment = new Payment();
                payment.setAccount(subscription);
                payment.setServiceId(subscription.getService());
                payment.setSubscriber_id(subscription.getSubscriber().getId());
                payment.setAmount(record.getDoubleAmount());
                payment.setContract(record.getAgreement());
                payment.setCurrency("AZN");
                payment.setUser(user);
                payment.setMethod(paymentOptionsPersistenceFacade.getOptionByName("BANK"));
                payment.setPurpose(purpose);
                paymentList.add(payment);
                paymentFacade.save(payment);

                log.debug("Payment: " + payment);

                operation = new Operation();
                operation.setBank(bank);
                operation.setAmount(payment.getAmountAsLong());
                operation.setCategory(cat);
                operation.setUser(user);
                operation.setSubscription(subscription);
                operation.setAccTransaction(accTrans);
                operation.setProvider(subscription.getService().getProvider());

                log.debug("Operation: " + operation);
                TransactionType transactionType = null;
                if (purpose == PaymentPurpose.VAT)
                    transactionType = TransactionType.PAYMENT_VAT;
                else
                    transactionType = TransactionType.PAYMENT;

                trans = transFacade.createTransation(
                        transactionType,
                        subscription,
                        operation.getAmount(),
                        String.format("%s adjustment for agreement %s, bank %s", transactionType, subscription.getAgreement(), bank.getFullName())
                );

                operation.setTransaction(trans);
                operationFacade.save(operation);

                payment.setProcessed(1);
                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create payment from record %s: ", record), ex);
            }

            operation = null;
            trans = null;
            subscription = null;
            payment = null;
            bank = null;
            subscription = null;
        }//END record iteration     

        accTransFacade.save(accTrans);
        log.debug(String.format("AccountingTransaction: %s", accTrans));
    }

    private void doBatchAdjustments(List<PaymentDocumentRecord> paymentRecordList, TransactionType transactionType, User user, String dsc) {
        if (paymentRecordList == null || paymentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        List<Payment> paymentList = new ArrayList<>();
        Bank bank = null;
        Subscription subscription = null;

        AccountingCategory cat = accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT);
        AccountingTransaction accTrans = new AccountingTransaction();
        accTrans.setDsc(dsc);
        accTrans.setType(AccountingTransactionType.BATCH_ADJUSTMENT);
        accTrans.setUser(user);

        Operation operation = null;
        Transaction trans = null;

        for (PaymentDocumentRecord record : paymentRecordList) {
            try {
                subscription = subscriptionFacade.findByCustomerIdentifier(record.getAgreement());
                operation = new Operation();

                if (record.getBankID() != null) {
                    bank = bankFacade.find(record.getBankID());
                    operation.setBank(bank);
                }

                operation.setAmount(record.getLongAmount());
                operation.setCategory(cat);
                operation.setUser(user);
                operation.setSubscription(subscription);
                operation.setAccTransaction(accTrans);
                operation.setProvider(subscription.getService().getProvider());

                log.debug("Operation: " + operation);

                StringBuilder sb = new StringBuilder().append(transactionType).append(" adjustment for agreement ")
                        .append(subscription.getAgreement());

                if (bank != null) {
                    sb.append("bank ").append(bank.getFullName());
                }

                trans = transFacade.createTransation(
                        transactionType,
                        subscription,
                        operation.getAmount(),
                        sb.toString()
                );

                operation.setTransaction(trans);
                operationFacade.save(operation);

                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create payment from record %s: ", record), ex);
            }

            operation = null;
            trans = null;
            subscription = null;
            bank = null;
            subscription = null;
        }//END record iteration

        accTransFacade.save(accTrans);
        log.debug(String.format("AccountingTransaction: %s", accTrans));
    }

    public void parseEquipmentFile(InputStream inputStream, User user) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<EquipmentDocumentRecord> recordList = new ArrayList<>();
            EquipmentDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new EquipmentDocumentRecord();

                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    log.debug("Column counter = " + columnCounter);
                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {//partNumber
                                currentRecord.setPartNumber(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getPartNumber()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// model
                                currentRecord.setModelName(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getPartNumber()));
                            }
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// brand
                                currentRecord.setBrandName(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getBrandName()));
                            }
                            break;
                        case 3:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// type
                                currentRecord.setTypeName(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getTypeName()));
                            }
                            break;
                        case 4:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// macAddress
                                currentRecord.setMacAddress(cell.getStringCellValue());
                                System.out.print(String.format("MacAddress: {%s} ", currentRecord.getMacAddress()));
                            }
                            break;
                        case 5:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// price
                                Double price = cell.getNumericCellValue();
                                currentRecord.setPrice(price);
                                log.debug(String.format("Parsed price=%f", currentRecord.getPrice()));
                            }
                            break;
                        case 6:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// discount
                                Double discount = cell.getNumericCellValue();
                                currentRecord.setDiscount(discount);
                                log.debug(String.format("Parsed discount=%f", currentRecord.getDiscount()));
                            } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                                currentRecord.setDiscount(0d);
                            }
                            break;
                        case 7:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// provider
                                log.debug("Provider raw: " + cell.getNumericCellValue());
                                currentRecord.setProvider(String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue()));
                                log.debug(String.format("Provider: {%s} ", currentRecord.getProvider()));
                            }
                            break;
                        default:
                            log.debug("N/A");
                    }

                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                recordList.add(currentRecord);
                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doImportEquipment(recordList, user);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    public void parseMinipopFile(InputStream inputStream, User user) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<MinipopDocumentRecord> recordList = new ArrayList<>();
            MinipopDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new MinipopDocumentRecord();

                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    log.debug("Column counter = " + columnCounter);
                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {//MAC
                                currentRecord.setMacAddress(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getMacAddress()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// SWITCH
                                currentRecord.setSwitchID(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getSwitchID()));
                            }
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {// Address
                                currentRecord.setAddress(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getAddress()));
                            }
                            break;
                        case 3:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// PortNUmber
                                currentRecord.setPortsNumber(Double.valueOf(cell.getNumericCellValue()).intValue());
                                System.out.print(String.format("{%d} ", currentRecord.getPortsNumber()));
                            }
                            break;
                        case 4:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) { //status
                                String deviceStatus = cell.getStringCellValue();
                                log.debug("Status raw: " + deviceStatus);
                                currentRecord.setStatus(deviceStatus);
                            }
                        default:
                            log.debug("N/A");
                    }

                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                recordList.add(currentRecord);
                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doImportMinipop(recordList, user);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }


    public void parseImsiFile(InputStream inputStream, User user) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<ImsiDocumentRecord> recordList = new ArrayList<>();
            ImsiDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new ImsiDocumentRecord();
                long providerID = 0;
                int catCode = 0;

                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    log.debug("Column counter = " + columnCounter);
                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {//iccid
                                currentRecord.setIccid(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getIccid()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {//imsi
                                currentRecord.setIdentififer(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getIdentififer()));
                            }
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// category
                                catCode = Double.valueOf(cell.getNumericCellValue()).intValue();
                                currentRecord.setCategory(catCode);
                                System.out.print(String.format("{%s} ", currentRecord.getCategory()));
                            }
                            break;
                        case 3:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// provider
                                providerID = Double.valueOf(cell.getNumericCellValue()).longValue();
                                currentRecord.setProvider(providerID);
                                System.out.print(String.format("{%s} ", currentRecord.getProvider()));
                            }
                            break;
                        default:
                            log.debug("N/A");
                    }

                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                if (currentRecord.getIccid() != null && !currentRecord.getIccid().isEmpty()
                        && currentRecord.getIdentififer() != null && !currentRecord.getIdentififer().isEmpty()
                        && currentRecord.getCategory() != null && currentRecord.getProvider() != null
                        )
                    recordList.add(currentRecord);

                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doImportImsi(recordList, user);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    private void doImportImsi(List<ImsiDocumentRecord> equipmentDocumentRecordList, User user) {
        if (equipmentDocumentRecordList == null || equipmentDocumentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        Imsi imsi = null;

        ServiceProvider provider = null;

        for (ImsiDocumentRecord record : equipmentDocumentRecordList) {
            try {
                imsi = record.getImsi();
                imsi.setUser(user);
                imsi.setStatus(StoreItemStatus.AVAILABLE);

                provider = serviceProviderPersistenceFacade.find(record.getProvider());

                if (provider != null)
                    imsi.setProvider(provider);

                if (imsi.getIdentifier() != null && !imsi.getIdentifier().isEmpty())
                    imsiFacade.save(imsi);
                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create equipment from record %s: ", record), ex);
            }
        }//END record iteration
    }


    public void parseMsisdnFile(InputStream inputStream, User user) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<MsisdnDocumentRecord> recordList = new ArrayList<>();
            MsisdnDocumentRecord currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new MsisdnDocumentRecord();
                long providerID = 0;
                int catCode = 0;

                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    log.debug("Column counter = " + columnCounter);
                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {//msisdn
                                currentRecord.setValue(cell.getStringCellValue());
                                System.out.print(String.format("{%s} ", currentRecord.getValue()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// category
                                catCode = Double.valueOf(cell.getNumericCellValue()).intValue();
                                currentRecord.setCategory(catCode);
                                System.out.print(String.format("{%s} ", currentRecord.getCategory()));
                            }
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {// provider
                                providerID = Double.valueOf(cell.getNumericCellValue()).longValue();
                                currentRecord.setProvider(providerID);
                                System.out.print(String.format("{%s} ", currentRecord.getProvider()));
                            }
                            break;
                        case 3:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) { //commercial category
                                currentRecord.setCommercialCategory(Double.valueOf(cell.getNumericCellValue()).longValue());
                            }
                            break;
                        case 4:
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) { //price
                                currentRecord.setPrice(cell.getNumericCellValue());
                            }
                        default:
                            log.debug("N/A");
                    }

                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                if (currentRecord.getValue() != null && !currentRecord.getValue().isEmpty()
                        && currentRecord.getCategory() != null && currentRecord.getProvider() != null
                        )
                    recordList.add(currentRecord);

                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doImportMsisdn(recordList, user);
            }
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    private void doImportMsisdn(List<MsisdnDocumentRecord> equipmentDocumentRecordList, User user) {
        if (equipmentDocumentRecordList == null || equipmentDocumentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        Msisdn msisdn = null;

        ServiceProvider provider = null;
        CommercialCategory comCat = null;

        for (MsisdnDocumentRecord record : equipmentDocumentRecordList) {
            try {
                msisdn = record.getMsisdn();
                msisdn.setUser(user);
                msisdn.setStatus(StoreItemStatus.AVAILABLE);

                provider = serviceProviderPersistenceFacade.find(record.getProvider());

                if (provider != null)
                    msisdn.setProvider(provider);

                if (msisdn.getValue() != null)
                    msisdnFacade.save(msisdn);

                if (record.getCommercialCategory() != null) {
                    comCat = comCategoryFacade.find(record.getCommercialCategory());
                    if (comCat != null)
                        msisdn.setCommercialCategory(comCat);
                }
                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create equipment from record %s: ", record), ex);
            }
        }//END record iteration
    }


    private void doImportEquipment(List<EquipmentDocumentRecord> equipmentDocumentRecordList, User user) {
        if (equipmentDocumentRecordList == null || equipmentDocumentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        for (EquipmentDocumentRecord record : equipmentDocumentRecordList) {
            try {
                EquipmentModel model = equipmentModelPersistenceFacade.findByName(record.getModelName());

                if (model == null) {
                    model = new EquipmentModel();
                    model.setName(record.getModelName());
                    equipmentModelPersistenceFacade.save(model);
                }

                EquipmentType type = equipmentTypePersistenceFacade.findByName(record.getTypeName());

                if (type == null) {
                    type = new EquipmentType();
                    type.setName(record.getTypeName());
                    equipmentTypePersistenceFacade.save(type);
                }

                EquipmentBrand brand = equipmentBrandPersistenceFacade.findByName(record.getBrandName());

                if (brand == null) {
                    brand = new EquipmentBrand();
                    brand.setName(record.getBrandName());
                    equipmentBrandPersistenceFacade.save(brand);
                }

                ServiceProvider provider = serviceProviderPersistenceFacade.find(Long.valueOf(record.getProvider()));

                if (provider == null) {
                    throw new Exception("Provider not found");
                }

                EquipmentPrice price = new EquipmentPrice();

                Float intermPrice = record.getPrice().floatValue() * 100000;
                price.setPrice(intermPrice.longValue());
                price.setDiscount(record.getDiscount());

                pricePersistenceFactory.save(price);

                Equipment equipment = new Equipment();
                equipment.setModel(model);
                equipment.setBrand(brand);
                equipment.setType(type);
                equipment.setPartNumber(record.getPartNumber());
                equipment.setMacAddress(record.getMacAddress());
                equipment.setStatus(EquipmentStatus.AVAILABLE);
                equipment.setProvider(provider);
                equipment.setPrice(price);

                equipmentFacade.save(equipment);
                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create equipment from record %s: ", record), ex);
            }
        }//END record iteration
    }

    private void doImportMinipop(List<MinipopDocumentRecord> minipopDocumentRecordList, User user) {
        if (minipopDocumentRecordList == null || minipopDocumentRecordList.isEmpty())
            throw new IllegalArgumentException("recordList must not be null or empty");

        for (MinipopDocumentRecord record : minipopDocumentRecordList) {
            try {
                if (minipopFacade.isDuplicate(record.getMacAddress())) {
                    throw new DuplicateEntityException(String.format("Minipop with mac address %s already exists.Skipping", record.getMacAddress()));
                }

                MiniPop miniPop = new MiniPop();
                miniPop.setMac(record.getMacAddress());
                miniPop.setAddress(record.getAddress());
                miniPop.setSwitch_id(record.getSwitchID());
                miniPop.setNumberOfPorts(record.getPortsNumber());

                if (record.getStatus() != null) {
                    miniPop.setDeviceStatus(DeviceStatus.convertFromCode(record.getStatus()));
                } else {
                    miniPop.setDeviceStatus(DeviceStatus.ACTIVE);
                }
                minipopFacade.save(miniPop);
                //accTrans.addOperation(operation);
            } catch (Exception ex) {
                log.error(String.format("Cannot create equipment from record %s: ", record), ex);
            }
        }//END record iteration
    }

    @Asynchronous
    public Future<String> parseUsersFile(InputStream inputStream, User user) {
        String status = null;
        log.info("Starting parsing users file....");
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<UserRow> recordList = new ArrayList<>();
            UserRow currentRecord = null;

            int columnCounter = 1;

            rowIterator.next();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cellIterator = row.iterator();
                currentRecord = new UserRow();


                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();

                    switch (cell.getColumnIndex()) {
                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                currentRecord.setFirstName(cell.getStringCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getFirstName()));
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setLastName(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getLastName()));
                            break;
                        case 2:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setUserName(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getUserName()));
                            break;
                        case 7:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setGroupName(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getGroupName()));
                            break;
                        case 6:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                currentRecord.setEmail(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getEmail()));
                            break;
                    }
                    /*
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            currentRecord.setAgreement(cell.getStringCellValue());
                            System.out.print(String.format("{%s} ", currentRecord.getAgreement()));
                        break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (columnCounter == 1) {
                                currentRecord.setBankID(Double.valueOf(cell.getNumericCellValue()).longValue());
                                log.debug(String.format("{%s} ", currentRecord.getBankID()));
                            }
                            else if (columnCounter == 3) {
                                currentRecord.setDoubleAmount(cell.getNumericCellValue());
                                log.debug(String.format("{%s} ", currentRecord.getLongAmount()));
                            }
                        break;
                        default:
                            log.debug("N/A");
                    }
                    */
                    cell = null;

                    columnCounter++;
                } // cell iteration ENDS

                //System.out.println();
                recordList.add(currentRecord);
                cellIterator = null;
                row = null;
                currentRecord = null;

                columnCounter = 1;
            } // row iteration ENDS

            log.debug(String.format("Recordlist: %s", recordList));

            if (!recordList.isEmpty()) {
                doBatchUserImport(recordList);
            }

            status = "SUCCESS";
            log.info("Users file parsing finished. Result: " + status);
            return new AsyncResult<String>(status);
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            status = "ERROR: " + ex.getCause();
            log.info("Users file parsing finished. Result: " + status);
            return new AsyncResult<String>(status);
        }
    }

    public void doBatchUserImport(List<UserRow> userRows) throws Exception {
        if (userRows != null && !userRows.isEmpty()) {
            log.debug("UserRows: " + userRows);

            for (UserRow row : userRows) {
                User user = null;
                /*try {
                    user = userFacade.findByUserName(row.getUserName());
                }
                catch (Exception ex) {
                    log.debug(ex);
                }
                if (user != null) {
                    log.error(String.format("User %s already exists. Skipping...", user));
                    continue;
                }
*/
                user = new User();
                user.setFirstName(row.getFirstName());
                user.setSurname(row.getLastName());
                user.setUserName(row.getUserName());
                user.setEmail(row.getEmail());

                Group group = groupFacade.findByName(row.getGroupName());
                if (group == null) {
                    throw new Exception("Cannot find group " + row.getGroupName());
                }

                user.setGroup(group);
                char[] rawPass = paswordGenerator.generatePassword();
                user.setPassword(paswordGenerator.encodePassword(rawPass));
                userFacade.save(user, String.copyValueOf(rawPass), false);
                Thread.sleep(20000);
                log.debug("Created user: " + user + ", rawPass=" + String.copyValueOf(rawPass));
            }
        }
    }

    public Set<WrapperAgreementChangeBatch> parseAgreementChangeFile(InputStream inputStream) throws Exception {

        Set<WrapperAgreementChangeBatch> batches = new HashSet<>();

        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet datatypeSheet = workbook.getSheetAt(0);

        Iterator<Row> iterator = datatypeSheet.iterator();

        iterator.next();//skip header row

        while (iterator.hasNext()) {

            Row currentRow = iterator.next();


            Iterator<Cell> cellIterator = currentRow.iterator();

            WrapperAgreementChangeBatch wrapperAgreementChange = new WrapperAgreementChangeBatch();

            try {

                while (cellIterator.hasNext()) {


                    Cell currentCell = cellIterator.next();

                    currentCell.setCellType(Cell.CELL_TYPE_STRING);

                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> TYPE >>>>>>>>>>>>>> " + currentCell.getCellType());
                    if (currentCell.getCellType() == Cell.CELL_TYPE_STRING) {
                        switch (currentCell.getColumnIndex()) {
                            case 0:
                                wrapperAgreementChange.setOldAgreement(convertNumberCellToTextual(currentCell));
                                break;
                            case 1:
                                wrapperAgreementChange.setAgreement(convertNumberCellToTextual(currentCell));
                                break;
                            case 2:
                                wrapperAgreementChange.setServiceName(convertNumberCellToTextual(currentCell));
                                break;
                            case 3:
                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> Promo value >>>>>>>>>>>>>> " + currentCell.getStringCellValue());
                                wrapperAgreementChange.setHasPromo(Integer.parseInt(currentCell.getStringCellValue()) == 1);
                                break;
                            case 4:
                                wrapperAgreementChange.setName(convertNumberCellToTextual(currentCell));
                                break;
                            case 5:
                                wrapperAgreementChange.setSurname(convertNumberCellToTextual(currentCell));
                                break;
                            case 6:
                                wrapperAgreementChange.setFatherName(convertNumberCellToTextual(currentCell));
                                break;
                            case 7:
                                wrapperAgreementChange.setPinCode(convertNumberCellToTextual(currentCell));
                                break;
                            case 8:
                                wrapperAgreementChange.setIdSerialNumber(convertNumberCellToTextual(currentCell));
                                break;
                            case 9:
                                wrapperAgreementChange.setHomeNumber(convertNumberCellToTextual(currentCell));
                                break;
                            case 10:
                                wrapperAgreementChange.setMobile(convertNumberCellToTextual(currentCell));
                                break;
                            case 11:
                                wrapperAgreementChange.setAddress(convertNumberCellToTextual(currentCell));
                                break;
                        }

                    }

                }
            } catch (Exception e) {
                wrapperAgreementChange.setMessage("can not process this row because of data type");
            }

            batches.add(wrapperAgreementChange);

        }
        batches
                .removeIf(data ->
                        data.getAgreement() == null || data.getAgreement().isEmpty()
                                || data.getOldAgreement() == null || data.getAgreement().isEmpty());
        return batches;
    }

    String convertNumberCellToTextual(Cell cell) {
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else {
            return cell.getStringCellValue();
        }
    }

    public List<WrapperBatch> parseStatusChangeFile(InputStream inputStream, User user) throws Exception {
        log.info("Batch operation for status change starts..");
        long start = System.currentTimeMillis();

        try {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator = null;

            Row row = null;
            Cell cell = null;

            List<WrapperBatch> wrapperBatchList = new ArrayList<>();
            List<WrapperBatch> batchListWithErrors = new ArrayList<>();


            rowIterator.next(); // it will ignore file Column Header = first row

            while (rowIterator.hasNext()) {

                WrapperBatch wrapperBatch = new WrapperBatch();
                Subscription subscription = null;

                String batchAgreement = null;
                Integer subscriptionStatus = null;
                String batchMessage = null;
                int batchStatus = 0;

                row = rowIterator.next();
                cellIterator = row.iterator();
                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
//                    log.debug("row num is ==> "+row.getRowNum()+" column index is => "+cell.getColumnIndex()+" type => "+cell.getCellType());
                    switch (cell.getColumnIndex()) {

                        case 0:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) { // agreement
                                batchAgreement = cell.getStringCellValue();
                                subscription = subscriptionFacade.findByAgreementOrdinary(batchAgreement);
                                if (subscription != null) {
                                    batchStatus = 1;
                                }
                            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                batchAgreement = String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue());
                                subscription = subscriptionFacade.findByAgreementOrdinary(batchAgreement);
                                if (subscription != null) {
                                    batchStatus = 1;
                                }
                            }
                            break;
                        case 1:
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) // to status
                            {
                                subscriptionStatus = Integer.parseInt(cell.getStringCellValue());
                            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                subscriptionStatus = Double.valueOf(cell.getNumericCellValue()).intValue();
                            }

                            break;
                        default:
                            log.debug("N/A ====>   Additional field which did not considered");
                    }
                    cell = null;
                }
                cellIterator = null;
                row = null;
                wrapperBatch.setBatchAgreement(batchAgreement);
                wrapperBatch.setStatus(batchStatus);
                wrapperBatch.setBatchStatus(subscriptionStatus);
                if (batchStatus == 0) {
                    wrapperBatch.setMsg("Subscription not found!");
                }
                wrapperBatch.setSubscription(subscription);
                wrapperBatchList.add(wrapperBatch);
            }

            log.info(wrapperBatchList.size() + " subscriptions are ready for changing status in batch operation");

            for (WrapperBatch wr : wrapperBatchList) {
                if (wr.getSubscription() != null) {


//                    try{
                    Subscription statusChangedSubscription = changeStatusInTransactionalContext(wr.getSubscription(), SubscriptionStatus.convertFromCode(Integer.valueOf(wr.getBatchStatus())));

                    if (statusChangedSubscription == null) {
                        wr.setMsg("Exception on status change. Pls contact with Billing Team " + wr.getBatchAgreement());
                        wr.setStatus(2);
                        batchListWithErrors.add(wr);
                    }

//                    } catch(Exception exc) {
//                        wr.setMsg("Exception on status change. Pls contact with Billing Team "+wr.getBatchAgreement());
//                        wr.setStatus(2);
//                        batchListWithErrors.add(wr);
//                        continue;
//                    }

                } else {
                    batchListWithErrors.add(wr);
                }
//                log.info("wrapperBatch result is aggr = " + wr.getBatchAgreement() + "  status = " + wr.getStatus() + " msg " + wr.getMsg());
                log.info(String.format("wrapperBatch result is aggr = %s, batch status = %s, operation status = %s, msg = %s", wr.getBatchAgreement(), wr.getBatchStatus(), wr.getStatus(), wr.getMsg()));
            }

            log.info("Batch operation for status change finished [elapsed time: " + (System.currentTimeMillis()-start)/1000. + " seconds]");

            return batchListWithErrors;
        } catch (Exception ex) {
            log.error("Cannot process file: ", ex);
            throw new Exception("Cannot process file", ex);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public Subscription changeStatusInTransactionalContext(Subscription subscription, SubscriptionStatus newStatus) {
        try {
            return commonOperationsEngine.changeStatus(subscription, newStatus);
        } catch (Exception ex) {
            log.error("Exception occurs in batch operation while changing status of " + subscription.getAgreement(), ex);
            log.debug("Exception message in batch operation while changing status of" + subscription.getAgreement()+": "+ex.getMessage());
            ctx.setRollbackOnly();
            return null;
        }
    }
}
