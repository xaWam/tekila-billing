package com.jaravir.tekila.module.report.export.pdf;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.exception.NoPrivilegeFoundException;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by sajabrayilov on 28.01.2015.
 */
@Stateless
public class PDFExporter {
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB private com.jaravir.tekila.base.auth.persistence.manager.SecurityManager securityManager;
    @Resource
    private SessionContext ctx;

    private final static Logger log = Logger.getLogger(PDFExporter.class);
    private final static Map<String, Class> paymentReportHeaders = new LinkedHashMap<>();

    static {
        //paymentReportHeaders.put("Id", Long.class);
        paymentReportHeaders.put("Date", String.class);
        paymentReportHeaders.put("Cheque No.", String.class);
        paymentReportHeaders.put("RRN", String.class);
        paymentReportHeaders.put("Agreement", String.class);
        paymentReportHeaders.put("Amount", Double.class);
        paymentReportHeaders.put("Service", String.class);
        //paymentReportHeaders.put("Method", String.class);
        //paymentReportHeaders.put("Purpose", String.class);
        paymentReportHeaders.put("Source", String.class);
        paymentReportHeaders.put("User", String.class);
        //paymentReportHeaders.put("Desc", String.class);
    }

    public void generatePaymentReport (OutputStream outputStream, Date fromDate, Date toDate) throws IOException, NoPrivilegeFoundException {
        if (!securityManager.checkUIPermissions("PaymentReport", Privilege.READ)
                && !securityManager.checkUIPermissions("OwnPaymentReport", Privilege.READ)
                ) {
            log.error("Not enough privileges to generate report");
            throw new NoPrivilegeFoundException();
        }

        Document pdfDocument = new Document(PageSize.A4.rotate(), 10, 10, 36, 36);

        ByteArrayOutputStream byteSteam = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDocument, byteSteam);
            pdfDocument.open();
            pdfDocument.add(new Chunk(""));

            PdfPTable table = new PdfPTable(paymentReportHeaders.size());
            PdfPCell cell = null;

            Class cl = null;
            int cellType = 0;
            int cellCounter = 0;
            int rowCounter = 1;
            int cellNumber = paymentReportHeaders.entrySet().size();

            Font headerFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
            Font cellFont = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL);

            for (Map.Entry<String, Class> header : paymentReportHeaders.entrySet()) {
                cl = header.getValue();

                //text = new Phrase(header.getKey(), headerFont);

                table.addCell(new PdfPCell(new Phrase(header.getKey(), headerFont)));
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
            long rowCount = paymentFacade.count();

            log.info("Total rows: " + rowCount);
            int pageSize = 0;

            if (rowCount <= 10000) {
                pageSize = (int) rowCount;
            }

            List<Payment> paymentList = paymentFacade.findAllPaginated(0, pageSize);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            for (Payment payment : paymentList) {
                //cell.setCellValue(payment.getId());
                table.addCell(new Phrase(dateFormat.format(payment.getFd()), cellFont));

               // if (payment.getChequeID() != null) {
                    table.addCell(new Phrase(payment.getChequeID(), cellFont));
               // }

               // if (payment.getRrn() != null) {
                    table.addCell(new Phrase(payment.getRrn(), cellFont));
               // }

               // if (payment.getContract() != null) {
                    table.addCell(new Phrase(payment.getContract(), cellFont));
               // }

                table.addCell(new Phrase(String.format("%.2f", payment.getAmount()), cellFont));

                table.addCell(new Phrase(payment.getAccount().getService().getName(), cellFont));

                /*if (payment.getMethodForUI() != null) {
                    table.addCell(new Phrase(payment.getMethodForUI(), cellFont));
                }

                if (payment.getPurpose() != null) {
                    table.addCell(new Phrase(payment.getPurpose().toString(), cellFont));
                }
                */

                table.addCell(new Phrase(payment.getExtUser() != null ? payment.getExtUser().getUsername() : "", cellFont));

                table.addCell(new Phrase(payment.getUser() != null ? payment.getUser().getUserName() : "", cellFont));

                /*if (payment.getDsc() != null) {
                    table.addCell(new Phrase(payment.getDsc(), cellFont));
                    }*/

            }

            double sum = paymentFacade.sumWithFilters();

            cell = new PdfPCell(new Phrase("Total: ", headerFont));
            cell.setColspan(4);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(String.format("%.2f", sum), headerFont));
            cell.setColspan(4);
            table.addCell(cell);

            pdfDocument.add(table);

            pdfDocument.close();
            outputStream.write(byteSteam.toByteArray());
        }
        catch (DocumentException ex) {
            log.error("Cannot create document.", ex);
            throw new IOException(ex);
        }
    }
}
