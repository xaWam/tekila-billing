package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.entity.Util;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.manager.OfflineStatsPersistenceFacade;
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElementType;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import oracle.jdbc.OracleTypes;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by shnovruzov on 5/24/2016.
 */
@Stateless(name = "UninetProvisioner", mappedName = "UninetProvisioner")
@Local(ProvisioningEngine.class)
public class UninetProvisioner implements ProvisioningEngine, Serializable {

    @Resource(name = "jdbc/Uninet.Radius")
    private DataSource radiusDataSource;

    private static final Logger log = Logger.getLogger(UninetProvisioner.class);

    @EJB
    private SystemLogger systemLog;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private Register register;
    @EJB
    private SubscriptionSettingPersistenceFacade subSettingsFacade;
    @EJB
    private SubscriptionSettingPersistenceFacade settingFacade;
    @EJB
    private MiniPopPersistenceFacade miniPopPersistenceFacade;
    @EJB
    private CitynetCommonEngine citynetCommonEngine;
    @PersistenceContext
    private EntityManager em;


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean ping() {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean initService(Subscription subscription) //throws ProvisioningException
    {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.add_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setString(2, subscription.getSettingByType(ServiceSettingType.PASSWORD).getValue());
            //callableStatement.setString(3, subscription.getSettingByType(ServiceSettingType.MAC_ADDRESS).getValue());
            callableStatement.setString(3, null);

            //callableStatement.setInt(4, Integer.parseInt(subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_SLOT).getValue()));
            callableStatement.setNull(4, Types.INTEGER);

            //callableStatement.setInt(5, Integer.parseInt(subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).getValue()));
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.VARCHAR); //persist empty static ip during subscription creation.

            //callableStatement.setString(7, subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_IP).getValue());
            callableStatement.setString(7, null);

            callableStatement.setTimestamp(8, null);
            callableStatement.setTimestamp(9, new Timestamp(DateTime.now().plusDays(7).withTime(23, 59, 59, 999).toDate().getTime()));
            callableStatement.setInt(10, 1);    //persist status ACTIVE on radius db
            callableStatement.registerOutParameter(11, Types.INTEGER);
            callableStatement.registerOutParameter(12, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(11);
            msg = callableStatement.getString(12);
            log.info(String.format("Init service of subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during initService of subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during initService of subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription) //throws ProvisioningException;
    {
        return openService(subscription, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expDate) //throws ProvisioningException;
    {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;
        int res = 0;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setNull(2, Types.VARCHAR);
            callableStatement.setNull(3, Types.VARCHAR);
            callableStatement.setNull(4, Types.INTEGER);
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.VARCHAR);
            callableStatement.setNull(7, Types.VARCHAR);
            callableStatement.setNull(8, Types.TIMESTAMP);

            if (expDate == null) {
                if (subscription.getExpirationDateWithGracePeriod() != null) {
                    expDate = subscription.getExpirationDateWithGracePeriod();
                } else {
                    expDate = subscription.getExpirationDate();
                }

                expDate = expDate.plusMonths(1).withTime(23, 59, 59, 999);
            }

            if (expDate != null) {
                callableStatement.setTimestamp(9, new Timestamp(expDate.toDate().getTime()));
            } else {
                callableStatement.setNull(9, Types.TIMESTAMP);
            }

            callableStatement.setInt(10, 1);

            callableStatement.registerOutParameter(11, Types.INTEGER);
            callableStatement.registerOutParameter(12, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(11);
            String msg = callableStatement.getString(12);
            log.info(String.format("Open service for subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during open service for subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during open service for subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeService(Subscription subscription) //throws ProvisioningException;
    {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;
        int res = 0;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setNull(2, Types.VARCHAR);
            callableStatement.setNull(3, Types.VARCHAR);
            callableStatement.setNull(4, Types.INTEGER);
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.VARCHAR);
            callableStatement.setNull(7, Types.VARCHAR);
            callableStatement.setNull(8, Types.TIMESTAMP);
            callableStatement.setNull(9, Types.TIMESTAMP);
            callableStatement.setInt(10, 0);

            callableStatement.registerOutParameter(11, Types.INTEGER);
            callableStatement.registerOutParameter(12, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(11);
            String msg = callableStatement.getString(12);
            log.info(String.format("Close service for subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during close service for subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during close service for subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OnlineBroadbandStats collectOnlineStats(Subscription subscription) //throws ProvisioningException;
    {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        OnlineBroadbandStats stats = null;

        try {
            String username = subscription.getAgreement();
            radiusConnection = radiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("select S.start_time, S.dslam, S.ip, S.nas, S.input_octets, S.output_octets " +
                    "from sessions S where S.username = ?");

            st.setString(1, username);
            rs = st.executeQuery();

            log.debug("RESULT: " + rs);

            if (rs.next()) {
                stats = new OnlineBroadbandStats();
                stats.setStartTime(rs.getDate(1));

                String dslamIpAddress = rs.getString(2);
                dslamIpAddress = dslamIpAddress.substring(dslamIpAddress.indexOf("//") + "//".length(), dslamIpAddress.indexOf("atm") - 1);
                stats.setDslamIpAddress(dslamIpAddress);

                stats.setFramedAddress(rs.getString(3));
                stats.setNasIpAddress(rs.getString(4));
                stats.setDown(String.format("%.3f", 1.0 * rs.getLong(5) / 1024 / 1024));
                stats.setUp(String.format("%.3f", 1.0 * rs.getLong(6) / 1024 / 1024));


            } else {
                return null;
            }
        } catch (Exception ex) {
            log.error(String.format("Error during collect online stast on radius for agreement=%s", subscription.getAgreement()), ex);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception ex) {
                log.error(String.format("Error during collectOnlineStats() for agreement=%s", subscription.getAgreement()), ex);
            }
        }

        return stats;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end) //throws ProvisioningException;
    {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public TechnicalStatus getTechnicalStatus(Subscription subscription) {

        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        TechnicalStatus technicalStatus = null;
        String logPrefix = "Getting technical status for Uninet subscription agreement id = " + subscription.getAgreement();
        try {
            log.debug(logPrefix);
            radiusConnection = radiusDataSource.getConnection();
            st = radiusConnection.prepareStatement("select AC.username,AC.STATUS,AC.TD " +
                    "FROM accounts AC WHERE AC.username = ?");
            st.setString(1, subscription.getAgreement());
            rs = st.executeQuery();
            if (rs.next()) {
                boolean activeUser = false;
                technicalStatus = new TechnicalStatus();
                Integer status = rs.getInt(2);
                Date tillDate = rs.getDate(3);
                if (status == 1) { //status is active
                    activeUser = true;
                }
                String active = activeUser ? "AVAILABLE" : "NOT AVAILABLE";
                String redirect = (activeUser == false) ? "REDIRECT" : "NOT RESTRICTED";
                technicalStatus.addElement(StatusElementType.BROADBAND_ACTIVE, active);
                technicalStatus.addElement(StatusElementType.BROADBAND_REDIRECT, redirect);
            }
            log.debug("Finished get technical status");
        } catch (SQLException e) {
            log.error(logPrefix + " error while fetching data.", e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(logPrefix + " error while closing connections.", e);
            }
        }

        return technicalStatus;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> getAuthRejectionReasons(Subscription subscription) {
        Connection radiusConnection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<String> rejectionReasons = new ArrayList<>();
        try {
            String query = "SELECT * FROM AUTH_REJECTS WHERE USERNAME = ? ";
            radiusConnection = radiusDataSource.getConnection();
            statement = radiusConnection.prepareStatement(query);
            statement.setString(1, subscription.getAgreement());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                rejectionReasons.add(resultSet.getString(3));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            log.info(rejectionReasons);
        }
        return rejectionReasons;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.delete_static_ip(?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.registerOutParameter(2, Types.INTEGER);
            callableStatement.registerOutParameter(3, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(2);
            msg = callableStatement.getString(3);
            log.info(String.format("closeVAS of subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during closeVAS of subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during closeVAS of subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        return reprovision(subscription);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) throws ProvisioningException {
        return "OFFLINE";
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    private String getSettingValue(Subscription subscription, ServiceSettingType settingType) {
        SubscriptionSetting setting = subscription.getSettingByType(settingType);
        if (setting != null) {
            return setting.getValue();
        } else {
            return null;
        }
    }

    private boolean reprovisionWithMergedProcedure(Subscription subscription, DateTime expirationWithGrace) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setString(2, getSettingValue(subscription, ServiceSettingType.PASSWORD));

            String settingValue = getSettingValue(subscription, ServiceSettingType.MAC_ADDRESS);
            callableStatement.setString(3, (settingValue != null && Util.isLegalMacAddress(settingValue)) ?
                    settingValue : null);

            settingValue = getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH_SLOT);
            if (settingValue != null) {
                callableStatement.setInt(4, Integer.parseInt(settingValue));
            } else {
                callableStatement.setNull(4, Types.INTEGER);
            }

            settingValue = getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH_PORT);
            if (settingValue != null) {
                callableStatement.setInt(5, Integer.parseInt(settingValue));
            } else {
                callableStatement.setNull(5, Types.INTEGER);
            }

            if ((settingValue = getSettingValue(subscription, ServiceSettingType.IP_ADDRESS)) != null) {
                callableStatement.setString(6, settingValue); //persist static ip.
            } else {
                callableStatement.setString(6, ""); //persist static ip.
            }
            callableStatement.setString(7, getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH_IP));
            if (subscription.getActivationDate() != null) {
                callableStatement.setTimestamp(8, new Timestamp(subscription.getActivationDate().toDate().getTime()));
            } else {
                callableStatement.setTimestamp(8, null);
            }

            if (expirationWithGrace != null) {
                callableStatement.setTimestamp(9, new Timestamp(expirationWithGrace.toDate().getTime()));
            } else if (subscription.getExpirationDateWithGracePeriod() != null) {
                callableStatement.setTimestamp(9, new Timestamp(subscription.getExpirationDateWithGracePeriod().toDate().getTime()));
            } else {
                callableStatement.setTimestamp(9, null);
            }

            if (subscription.getStatus().equals(SubscriptionStatus.ACTIVE) ||
                    subscription.getStatus().equals(SubscriptionStatus.PARTIALLY_BLOCKED) ||
                    subscription.getStatus().equals(SubscriptionStatus.INITIAL)) {
                callableStatement.setInt(10, 1);    //persist status ACTIVE on radius db
            } else {
                callableStatement.setInt(10, 0);    //persist status NOT_ACTIVE on radius db
            }
            callableStatement.registerOutParameter(11, Types.INTEGER);
            callableStatement.registerOutParameter(12, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(11);
            msg = callableStatement.getString(12);
            log.info(String.format("Reprovisioning of subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during reprovision of subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during reprovision of subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {
        return reprovisionWithMergedProcedure(subscription, subscription.getExpirationDateWithGracePeriod());
    }

    //used only during restore
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        boolean res = reprovisionWithMergedProcedure(subscription, endDate);
        if (res && subscription.getActivationDate() == null) {
            res = resetActivationDate(subscription);
        }
        return res;
    }

    private boolean resetActivationDate(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.reset_activation_date(?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.registerOutParameter(2, Types.INTEGER);
            callableStatement.registerOutParameter(3, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(2);
            msg = callableStatement.getString(3);
            log.info(String.format("resetActivationDate of subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during resetActivationDate of subscription id = %d", subscription.getId()), e);
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {
                log.error(String.format("Exception occurred during resetActivationDate of subscription id = %d.", subscription.getId()), e);
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createNas(Nas nas) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateNas(Nas nas) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createAttribute(Attribute attribute, Long nasId) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAttribute(Attribute attribute) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<OfflineBroadbandStats> stats = new ArrayList<>();

        int res = -1;

        try {

            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getAgreement();
            if (filters.get(OfflineStatsPersistenceFacade.Filter.USERNAME) != null)
                username = (String) filters.get(OfflineStatsPersistenceFacade.Filter.USERNAME);
            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();

//            log.debug(String.format("HERE: collectOfflineStats: user=%s, tariff=%s", username, tariff));

            radiusConnection = radiusDataSource.getConnection();

            String where = "SUBSTR(RA.USERNAME,21) = ?";
            where += " and RA.ACCTSTARTTIME >= ?";
            where += " and RA.ACCTSTOPTIME <= ?";

            if (filters.get(OfflineStatsPersistenceFacade.Filter.FRAMED_ADDRESS) != null)
                where += " and RA.FRAMEDIPADDRESS like ?";
            if (filters.get(OfflineStatsPersistenceFacade.Filter.TERMINATION_CAUSE) != null)
                where += " and RA.ACCTTERMINATECAUSE like ?";
            st = radiusConnection.prepareStatement("select RA.ACCTSESSIONID,RA.ACCTINPUTOCTETS,RA.ACCTOUTPUTOCTETS,RA.ACCTSTARTTIME,RA.ACCTSESSIONTIME,RA.ACCTTERMINATECAUSE,RA.NASIPADDRESS,RA.FRAMEDIPADDRESS,RA.CALLINGSTATIONID,RA.SERVICE" +
                    " from ncn_radacct_history RA where " + where);

            boolean flag = false;
            st.setString(1, username);
            st.setDate(2, new Date(((java.util.Date) filters.get(OfflineStatsPersistenceFacade.Filter.START_DATE)).getTime()));
            st.setDate(3, new Date(((java.util.Date) filters.get(OfflineStatsPersistenceFacade.Filter.END_TIME)).getTime()));
            if (filters.get(OfflineStatsPersistenceFacade.Filter.FRAMED_ADDRESS) != null) {
                st.setString(4, (String) filters.get(OfflineStatsPersistenceFacade.Filter.FRAMED_ADDRESS));
                flag = true;
            }
            if (filters.get(OfflineStatsPersistenceFacade.Filter.TERMINATION_CAUSE) != null)
                if (flag)
                    st.setString(5, (String) filters.get(OfflineStatsPersistenceFacade.Filter.TERMINATION_CAUSE));
                else st.setString(4, (String) filters.get(OfflineStatsPersistenceFacade.Filter.TERMINATION_CAUSE));

            rs = st.executeQuery();

            while (rs.next()) {
                OfflineBroadbandStats offlineStats = new OfflineBroadbandStats();
                offlineStats.setAccountID(subscription.getAgreement());
                offlineStats.setAccountSessionID(rs.getString(1));
                offlineStats.setUser(username);
                double download = rs.getLong(2) / 1024d / 1024d;
                double upload = rs.getLong(3) / 1024d / 1024d;
                offlineStats.setDown(String.format("%.3f", upload));
                offlineStats.setUp(String.format("%.3f", download));
                offlineStats.setStartTime(rs.getTime(4));
                offlineStats.setSessionDuration(String.valueOf(rs.getLong(5)));
                offlineStats.setTerminationCause(rs.getString(6));
                offlineStats.setNasIpAddress(rs.getString(7));
                offlineStats.setFramedAddress(rs.getString(8));
                offlineStats.setCallingStationID(rs.getString(9));
                offlineStats.setService(rs.getString(10));
                stats.add(offlineStats);
            }
        } catch (SQLException ex) {
            log.error(String.format("Error during collecting offline stats on radius for agreement=%s", subscription.getAgreement()), ex);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during collecting offline stats on radius for agreement=%s", subscription.getAgreement()), ex);
            }
        }

        return stats;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(Service service) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateByService(Service service) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DateTime getActivationDate(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        ResultSet prs = null;
        long fd = 0;

        try {
            radiusConnection = radiusDataSource.getConnection();

            String sqlQuery = "SELECT FD FROM ACCOUNTS WHERE USERNAME = '" + subscription.getAgreement() + "'";
            pr = radiusConnection.prepareCall(sqlQuery);
            prs = pr.executeQuery();
            prs.next();
            fd = prs.getTimestamp(1).getTime();

            log.debug("getActivationDate: " + fd);

            return new DateTime(fd);

        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during getting activation date: " + subscription.getAgreement() + " " + ex);
            }
        }
    }

    public boolean removeAccount(Subscription subscription) {
        return false;
    }


    public boolean removeFD(Subscription subscription) {
            return false;
        }






    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Usage getUsage(Subscription subscription, java.util.Date startDate, java.util.Date endDate) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean provisionIptv(Subscription subscription) {
        return true;
    }

    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        return null;
    }

    @Override
    public void provisionNewAgreements(String oldAgreement, String newAgreement) {

    }
}
