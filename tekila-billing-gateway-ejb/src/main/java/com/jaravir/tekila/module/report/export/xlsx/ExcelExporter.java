package com.jaravir.tekila.module.report.export.xlsx;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.exception.NoPrivilegeFoundException;
import com.jaravir.tekila.base.auth.persistence.manager.*;
import com.jaravir.tekila.base.auth.persistence.manager.SecurityManager;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberFunctionalCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by sajabrayilov on 27.01.2015.
 */
@Stateless
public class ExcelExporter {
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private SecurityManager securityManager;
    @EJB
    private PaymentOptionsPersistenceFacade paymentOptionsPersistenceFacade;
    @Resource
    private SessionContext ctx;

    private final static Logger log = Logger.getLogger(ExcelExporter.class);
    private final static Map<String, Class> paymentReportHeaders = new LinkedHashMap<>();

    static {
        paymentReportHeaders.put("Id", Long.class);
        paymentReportHeaders.put("Date", String.class);
        paymentReportHeaders.put("Cheque No.", String.class);
        paymentReportHeaders.put("RRN", String.class);
        paymentReportHeaders.put("Agreement", String.class);
        paymentReportHeaders.put("Name", String.class);
        paymentReportHeaders.put("Amount", Double.class);
        paymentReportHeaders.put("Service", String.class);
        paymentReportHeaders.put("Method", String.class);
        paymentReportHeaders.put("Purpose", String.class);
        paymentReportHeaders.put("Source", String.class);
        paymentReportHeaders.put("User", String.class);
        paymentReportHeaders.put("Desc", String.class);
    }

    public void generatePaymentReport(
            OutputStream outputStream,
            Date fromDate,
            Date toDate,
            String providerId,
            String externalUserName) throws IOException, NoPrivilegeFoundException {
        if (!securityManager.checkUIPermissions("PaymentReport", Privilege.READ)
                && !securityManager.checkUIPermissions("OwnPaymentReport", Privilege.READ)
                ) {
            log.error("Not enough privileges to generate report");
            throw new NoPrivilegeFoundException();
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        Cell cell = null;
        Class cl = null;
        int cellType = 0;
        int cellCounter = 0;
        int rowCounter = 1;
        int cellNumber = paymentReportHeaders.entrySet().size();

        sheet.setDefaultColumnWidth(10);
        Font headerFont = workbook.createFont();
        CellStyle headerStyle = workbook.createCellStyle();

        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(headerFont);

        for (Map.Entry<String, Class> header : paymentReportHeaders.entrySet()) {
            cl = header.getValue();

            cell = row.createCell(cellCounter, Cell.CELL_TYPE_STRING);
            cell.setCellValue(header.getKey());
            cell.setCellStyle(headerStyle);
            cellCounter++;
        }
        cellCounter = 0;

        paymentFacade.clearFilters();
        paymentFacade.setPredicateJoinOperation(AbstractPersistenceFacade.PredicateJoinOperation.AND);
        PaymentPersistenceFacade.Filter dateFilter = PaymentPersistenceFacade.Filter.DATE;
        dateFilter.setOperation(MatchingOperation.BETWEEN);
        Map<String, Date> dateMap = new HashMap<>();
        dateMap.put("from", new DateTime(fromDate).toDate());
        dateMap.put("to", new DateTime(toDate).toDate());
        paymentFacade.addFilter(dateFilter, dateMap);

        if (providerId != null && !providerId.equals("-1")) {
            PaymentPersistenceFacade.Filter providerIdFilter = PaymentPersistenceFacade.Filter.PROVIDER_ID;
            providerIdFilter.setOperation(MatchingOperation.EQUALS);
            paymentFacade.addFilter(providerIdFilter, providerId);
        }

        if (externalUserName != null && !externalUserName.equals("-1")) {
            PaymentPersistenceFacade.Filter externalUserNameFilter = PaymentPersistenceFacade.Filter.EXT_USER;
            externalUserNameFilter.setOperation(MatchingOperation.EQUALS);
            paymentFacade.addFilter(externalUserNameFilter, externalUserName);
        }

        PaymentPersistenceFacade.Filter statusFilter = PaymentPersistenceFacade.Filter.STATUS;
        statusFilter.setOperation(MatchingOperation.NOT_EQUALS);
        paymentFacade.addFilter(statusFilter, -1);

        PaymentPersistenceFacade.Filter testFilter = PaymentPersistenceFacade.Filter.SHOW_TEST;
        testFilter.setOperation(MatchingOperation.NOT_EQUALS);
        paymentFacade.addFilter(testFilter, SubscriberFunctionalCategory.TEST);
        long rowCount = paymentFacade.count();

        log.info("Total rows: " + rowCount);
        int pageSize = 0;

        if (rowCount <= 20000) {
            pageSize = (int) rowCount;
        }
        log.debug("BEFORE PAYMENTS TO REPORT");
        List<Payment> paymentList = paymentFacade.findAllPaginated(0, pageSize);
        //List<Payment> paymentList = paymentFacade.findAllByDates(fromDate,toDate);
        log.debug("PAYMENTS TO REPORT: " + paymentList.size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        for (Payment payment : paymentList) {
            row = sheet.createRow(rowCounter);

            for (int i = 0; i < cellNumber; i++) {
                cell = row.createCell(0, Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(payment.getId());

                cell = row.createCell(1, Cell.CELL_TYPE_STRING);
                cell.setCellValue(dateFormat.format(payment.getFd()));

                if (payment.getChequeID() != null) {
                    cell = row.createCell(2, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getChequeID());
                }

                if (payment.getRrn() != null) {
                    cell = row.createCell(3, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getRrn());
                }

                if (payment.getContract() != null) {
                    cell = row.createCell(4, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getContract());
                }

                SubscriberDetails details = payment.getAccount().getSubscriber().getDetails();

                if (details.getType() == SubscriberType.INDV) {
                    cell = row.createCell(5, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(details.getFirstName() + " " + details.getMiddleName() + " " + details.getSurname());
                }
                else {
                    cell = row.createCell(5, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(details.getCompanyName());
                }

                cell = row.createCell(6, Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(payment.getAmount());

                cell = row.createCell(7, Cell.CELL_TYPE_STRING);
                cell.setCellValue(payment.getAccount().getService().getName());

                PaymentOption option = payment.getMethod();
                if (option != null) {
                    cell = row.createCell(8, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(option.getName());
                }

                if (payment.getPurpose() != null) {
                    cell = row.createCell(9, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getPurpose().toString());
                }

                if (payment.getExtUser() != null) {
                    cell = row.createCell(10, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getExtUser().getUsername());
                }

                if (payment.getUser() != null) {
                    cell = row.createCell(11, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getUser().getUserName());
                }

                if (payment.getDsc() != null) {
                    cell = row.createCell(12, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(payment.getDsc());
                }
            }
            rowCounter++;
        }

        double sum = paymentFacade.sumWithFilters();

        row = sheet.createRow(rowCounter);
        cell = row.createCell(0, Cell.CELL_TYPE_STRING);
        cell.setCellValue("Total: ");
        cell.setCellStyle(headerStyle);

        cell = row.createCell(5, Cell.CELL_TYPE_NUMERIC);
        cell.setCellValue(sum);
        cell.setCellStyle(headerStyle);
        rowCounter++;
        workbook.write(outputStream);
    }
}
