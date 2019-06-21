package com.jaravir.tekila.engines;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SIPStatusesVM;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class SipChargesFetcher {
    private static final Logger log = LoggerFactory.getLogger(SipChargesFetcher.class);

    @Resource(name = "jdbc/SipChargesPool")
    private DataSource sipChargesDataSource;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<SipCharge> getSipChargesForYesterday() {
        Connection sipDbConnection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            sipDbConnection = sipChargesDataSource.getConnection();

            DateTime yesterday = DateTime.now().minusDays(1).withHourOfDay(0).withMinuteOfHour(0);
            DateTime today = DateTime.now().withHourOfDay(0).withMinuteOfHour(0);

            log.debug("yesterday = " + new Timestamp(yesterday.toDate().getTime()));
            log.debug("today = " + new Timestamp(today.toDate().getTime()));

            statement = sipDbConnection.prepareStatement("select * from detail_con where CallDate >= ? and CallDate <= ?");
            statement.setTimestamp(1, new Timestamp(yesterday.toDate().getTime()));
            statement.setTimestamp(2, new Timestamp(today.toDate().getTime()));

            rs = statement.executeQuery();
            List<SipCharge> sipCharges = new ArrayList<>();
            while (rs.next()) {
                sipCharges.add(new SipCharge(
                                        rs.getString(1),
                                        rs.getString(2),
                                        rs.getTimestamp(3),
                                        rs.getInt(4),
                                        rs.getDouble(5)));
            }
            return sipCharges;
        } catch (SQLException ex) {
            log.error("Exception on SipChargesFetcher.getSipChargesForYesterday", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (sipDbConnection != null) {
                    sipDbConnection.close();
                }
            } catch (SQLException e) {
                log.error(" Error while closing connections on SipChargesFetcher.getSipChargesForYesterday.", e);
            }
        }
        return null;
    }
}
