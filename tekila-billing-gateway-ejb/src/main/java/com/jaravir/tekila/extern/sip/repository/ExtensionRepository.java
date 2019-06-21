package com.jaravir.tekila.extern.sip.repository;

import com.jaravir.tekila.extern.sip.domain.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SIPStatusesVM;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author MusaAl
 * @date 8/29/2018 : 4:14 PM
 */
@Stateless
public class ExtensionRepository {

    private static final Logger log = LoggerFactory.getLogger(ExtensionRepository.class);

    @Resource(name = "jdbc/SipChargesPool")
    private DataSource sipDataSource;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Extension findOneBySipNumber(SIPStatusesVM vm){
        Connection sipDbConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Extension r = null;
        try {
            sipDbConnection = sipDataSource.getConnection();
            ps = sipDbConnection.prepareStatement("SELECT * FROM extension WHERE extension_no=? ");

            ps.setInt(1, vm.getExtensionNo());

            rs = ps.executeQuery();
            log.debug("SIP SELECT rs is :{}", rs);
            if(rs.next()){
                r = processRow(rs);
            }
        } catch (SQLException ex) {
            log.error("Exception on SipChargesFetcher.findOneBySipNumber", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (sipDbConnection != null) {
                    sipDbConnection.close();
                }
            } catch (SQLException e) {
                log.error(" Error while closing connections on SipChargesFetcher.findOneBySipNumber.", e);
            }
        }
        return r;
    }

    /**
     *   Update subscription status on SIP db -> it will sinch data between sip billing, and sip billing will update status with sip radius.
     *
     *

     15 Forw_serv 0 Forwarding Service Blocked
     14 CLIR_serv 1 CLIR Service
     13 CLIR_serv 0 CLIR Service Blocked
     12 BlackList_serv 1 Black List service
     11 BlackList_serv 0 Black List service blocked
     10 ActiveDeactive_serv 1 Incoming Call Barring Service
     9 ActiveDeactive_serv 0 Incoming Call Barring Service Blocked
     8 outg_status 4 International outgoing
     7 outg_status 3 National outgoing (Azerbaijan)
     6 outg_status 2 Local outgoing (Baku)
     5 outg_status 1 blocked number (outgoing)
     4 outg_status 0 unallocated number (outgoing)
     3 incom_status 2 normal (incoming)
     2 incom_status 1 blocked number (incoming)
     1 incom_status 0 unallocated number (incoming)
     16 Forw_serv 1 Forwarding Service
     17 CallBarr_serv 0 Call Barring Service blocked (outgoing)
     18 CallBarr_serv 1 Call Barring Service (outgoing)
     19 ClosedGroup 0 Without Closed Group
     20 ClosedGroup 255 AZRT Staff  Group Number

     *
     *
     *
     * */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void udateSipSubscriptionStatus(SIPStatusesVM vm){
        Connection sipDbConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            sipDbConnection = sipDataSource.getConnection();

            ps = sipDbConnection.prepareStatement("UPDATE EXTENSION SET incom_status=?, outg_status=?," +
                    " ActiveDeactive_serv=?, BlackList_serv=?, CLIR_serv=?, FORW_serv=?," +
                    " CallBarr_serv=?, ClosedGroup=? WHERE extension_no=? ");
            ps.setInt(1, vm.getIncomeStatus());
            ps.setInt(2, vm.getOutgStatus());
            ps.setInt(3, vm.getActiveDeactiveServ());
            ps.setInt(4, vm.getBlackListServ());
            ps.setInt(5, vm.getClirServ());
            ps.setInt(6, vm.getForwServ());
            ps.setInt(7, vm.getCallBarrServ());
            ps.setInt(8, vm.getClosedGroup());
            ps.setInt(9, vm.getExtensionNo());
            rs = ps.executeQuery();
            log.debug("SIP update subscription status rs is :{}", rs);
        } catch (SQLException ex) {
            log.error("Exception on SipChargesFetcher.udateSipSubscriptionStatus", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (sipDbConnection != null) {
                    sipDbConnection.close();
                }
            } catch (SQLException e) {
                log.error(" Error while closing connections on SipChargesFetcher.udateSipSubscriptionStatus.", e);
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void udateSipSubscriptionOutgoingStatus(SIPStatusesVM vm){
        Connection sipDbConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            sipDbConnection = sipDataSource.getConnection();
            ps = sipDbConnection.prepareStatement("UPDATE EXTENSION SET outg_status=? WHERE extension_no=? ");
            ps.setInt(1, vm.getOutgStatus());
            ps.setInt(2, vm.getExtensionNo());
            rs = ps.executeQuery();
            log.debug("SIP udateSipSubscriptionOutgoingStatus rs is :{}", rs);
        } catch (SQLException ex) {
            log.error("Exception on SipChargesFetcher.udateSipSubscriptionOutgoingStatus", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (sipDbConnection != null) {
                    sipDbConnection.close();
                }
            } catch (SQLException e) {
                log.error(" Error while closing connections on SipChargesFetcher.udateSipSubscriptionOutgoingStatus.", e);
            }
        }
    }

    public Extension processRow(ResultSet rs) throws SQLException {
        Extension r = new Extension();
        r.setExtensionNo(rs.getInt("extension_no"));
        r.setIncomeStatus(rs.getInt("income_status"));
        r.setOutgStatus(rs.getInt("outg_status"));
        r.setActiveDeactiveServ(rs.getInt("activeDeactive_serv"));
        r.setBlackListServ(rs.getInt("blackList_serv"));
        r.setClirServ(rs.getInt("clir_serv"));
        r.setForwServ(rs.getInt("forw_serv"));
        r.setCallBarrServ(rs.getInt("callBarr_serv"));
        r.setClosedGroup(rs.getInt("closedGroup"));
        return r;
    }

}
