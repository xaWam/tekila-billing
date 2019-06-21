package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.stats.external.ExternalStatusInformation;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElementType;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Stateless(name = "DataplusProvisioner", mappedName = "DataplusProvisioner")
@Local(ProvisioningEngine.class)
public class DataplusProvisioner implements ProvisioningEngine, Serializable {

    private static final Logger log = Logger.getLogger(DataplusProvisioner.class);

    @Resource(name = "jdbc/Dataplus.Radius")
    private DataSource radiusDataSource;

    @EJB
    private ServicePropertyPersistenceFacade propertyPersistenceFacade;

    @EJB
    private SubscriptionServiceTypePersistenceFacade subscriptionServiceTypePersistenceFacade;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private MiniPopPersistenceFacade miniPopPersistenceFacade;


    @Override
    public boolean ping() {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean initService(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.add_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setString(2, subscription.getSettingByType(ServiceSettingType.USERNAME).getValue());
            callableStatement.setString(3, subscription.getSettingByType(ServiceSettingType.PASSWORD).getValue());
            callableStatement.setNull(4, Types.VARCHAR);
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.INTEGER);
            callableStatement.setNull(7, Types.VARCHAR); //IP
            callableStatement.setNull(8, Types.VARCHAR);

//            callableStatement.setString(9, propertyPersistenceFacade.find(
//                    subscription.getService(),
//                    subscription.getSettingByType(ServiceSettingType.ZONE),
//                    subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE)
//            ).getProfileValue());


            callableStatement.setString(9, subscription.getService().getName());


            SubscriptionServiceType subscriptionServiceType = subscriptionServiceTypePersistenceFacade.find
                    (Long.parseLong(subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE).getValue()));


            callableStatement.setString(10, subscriptionServiceType.getProfile().getName().toLowerCase());

            callableStatement.setTimestamp(11, null);
            callableStatement.setTimestamp(12, new Timestamp(DateTime.now().toDate().getTime()));
            callableStatement.setInt(13, 1);    //persist status ACTIVE on radius db

            callableStatement.registerOutParameter(14, Types.INTEGER);
            callableStatement.registerOutParameter(15, Types.VARCHAR);

            callableStatement.executeUpdate();

            res = callableStatement.getInt(14);
            msg = callableStatement.getString(15);
            log.info(String.format("Init service of subscription id = %d. Res = %d. Msg = %s", subscription.getId(), res, msg));
        } catch (SQLException e) {
            log.error(String.format("Exception occurred during initService of subscription id = "+ subscription.getId()) + "  =>  "+ e);
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
    public boolean openService(Subscription subscription) {
        return openService(subscription, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expirationDate) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setNull(2, Types.VARCHAR);
            callableStatement.setNull(3, Types.VARCHAR);
            callableStatement.setNull(4, Types.VARCHAR);
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.INTEGER);
            callableStatement.setNull(7, Types.VARCHAR); //IP
            callableStatement.setNull(8, Types.VARCHAR);
            callableStatement.setNull(9, Types.VARCHAR);
            callableStatement.setNull(10, Types.VARCHAR);
            callableStatement.setNull(11, Types.TIMESTAMP);


            DateTime expDateParameter;
            log.debug("expirationDate:" + expirationDate);
            if (expirationDate == null) {
                //expDateParameter =  DateTime.now().plusMonths(2).withTime(23, 59, 59, 999);
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                        && subscription.getExpirationDateWithGracePeriod() != null) {
                    expDateParameter = subscription.getExpirationDateWithGracePeriod();
                    log.debug("GARCE: " + subscription.getExpirationDateWithGracePeriod());
                } else {
                    expDateParameter = DateTime.now();
                    log.debug("GRACE NOW: " + expDateParameter);
                }

                expDateParameter = expDateParameter.withTime(23, 59, 59, 999).plusDays(
                        billSettings.getSettings().getMaximumPrepaidlifeCycleLength()
                );
                log.debug("EXP AFTER PLUS: " + expDateParameter);
            } else {
                log.debug("ELSE");
                expDateParameter = expirationDate;
            }
            callableStatement.setTimestamp(12, new Timestamp(expDateParameter.toDate().getTime()));
            callableStatement.setInt(13, 1);

            callableStatement.registerOutParameter(14, Types.INTEGER);
            callableStatement.registerOutParameter(15, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(14);
            msg = callableStatement.getString(15);
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
    public boolean closeService(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;
        int res = 0;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setNull(2, Types.VARCHAR);
            callableStatement.setNull(3, Types.VARCHAR);
            callableStatement.setNull(4, Types.VARCHAR);
            callableStatement.setNull(5, Types.INTEGER);
            callableStatement.setNull(6, Types.INTEGER);
            callableStatement.setNull(7, Types.VARCHAR);
            callableStatement.setNull(8, Types.VARCHAR);
            callableStatement.setNull(9, Types.VARCHAR);
            callableStatement.setNull(10, Types.VARCHAR);
            callableStatement.setNull(11, Types.TIMESTAMP);
            callableStatement.setNull(12, Types.TIMESTAMP);
            callableStatement.setInt(13, 0); //go offline

            callableStatement.registerOutParameter(14, Types.INTEGER);
            callableStatement.registerOutParameter(15, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(14);
            String msg = callableStatement.getString(15);
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
    public OnlineBroadbandStats collectOnlineStats(Subscription subscription) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        OnlineBroadbandStats stats = null;

        try {
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            log.debug("onine session username:" + username + ";");

            radiusConnection = radiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("SELECT S.start_time, S.dslam, S.ip, S.nas, S.int_octets, S.out_octets, s.called_station_id " +
                    "FROM sessions S WHERE S.username = ?");

            st.setString(1, username);
            rs = st.executeQuery();

            //log.debug("RESULT: " + rs);

            if (rs.next()) {
                stats = new OnlineBroadbandStats();
                stats.setStartTime(rs.getDate(1));

                String dslamIpAddress = rs.getString(2);
//                if (dslamIpAddress != null)
//                    dslamIpAddress = dslamIpAddress.substring(dslamIpAddress.indexOf("//") + "//".length(), dslamIpAddress.indexOf("atm") - 1);

                stats.setDslamIpAddress(dslamIpAddress);

                stats.setFramedAddress(rs.getString(3));
                stats.setNasIpAddress(rs.getString(4));
                stats.setDown(String.format("%.3f", 1.0 * rs.getLong(5) / 1024 / 1024));
                stats.setUp(String.format("%.3f", 1.0 * rs.getLong(6) / 1024 / 1024));
                stats.setCallingStationID(rs.getString(7));
                log.debug("stat.getDown " + stats.getDown());
                log.debug("stat.getUp " + stats.getUp());
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


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OnlineBroadbandStats> collectOnlineStatswithPage(int start, int end) {
        ArrayList<OnlineBroadbandStats> onlineBroadbandStatss = new ArrayList<>();
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        OnlineBroadbandStats stats = null;


        log.debug("start " + start);
        log.debug("end " + end);
        String sql = "select * from (\n" +
                "select rownum r, d.* from (\n" +
                "select s.username, S.start_time, S.dslam, S.ip, S.nas, S.int_octets, S.out_octets, s.called_station_id from sessions S \n" +
                "order by start_time desc) d\n" +
                ") where r between " + start + " and " + end;

        log.debug("sql :" + sql);
        try {
            radiusConnection = radiusDataSource.getConnection();


            st = radiusConnection.prepareStatement(sql);

            rs = st.executeQuery();

            //log.debug("RESULT: " + rs.toString());


            while (rs.next()) {
                stats = new OnlineBroadbandStats();
                stats.setUser(rs.getString(2));
                stats.setStartTime(rs.getDate(3));
                log.debug("rs.getDate(3):" + rs.getDate(3));
                String dslamIpAddress = rs.getString(4);
//                if (dslamIpAddress != null)
//                    dslamIpAddress = dslamIpAddress.substring(dslamIpAddress.indexOf("//") + "//".length(), dslamIpAddress.indexOf("atm") - 1);

                stats.setDslamIpAddress(dslamIpAddress);

                stats.setFramedAddress(rs.getString(5));
                stats.setNasIpAddress(rs.getString(6));
                stats.setDown(String.format("%.3f", 1.0 * rs.getLong(7) / 1024 / 1024));
                stats.setUp(String.format("%.3f", 1.0 * rs.getLong(8) / 1024 / 1024));
                stats.setCallingStationID(rs.getString(9));
                onlineBroadbandStatss.add(stats);
                log.debug("stat.getDown " + stats.getDown());
                log.debug("stat.getUp " + stats.getUp());
            }
        } catch (Exception ex) {
            log.error(String.format("Error during collect All online stast on radius:", ex));
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
                log.error(String.format("Error during collectOnlineStatswithPages() :", ex));
            }
        }

        return onlineBroadbandStatss;

    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        String ipAddress = "";

        try {
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            log.debug("onine session username " + username);

            radiusConnection = radiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("SELECT S.nas streams FROM sessions S WHERE S.username = ?");

            st.setString(1, username);
            rs = st.executeQuery();

            //log.debug("RESULT: " + rs);

            if (rs.next()) {
                ipAddress = rs.getString(1);
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

        return ipAddress;

    }


    @Override
    public TechnicalStatus getTechnicalStatus(Subscription subscription) {

        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        TechnicalStatus technicalStatus = null;
        String logPrefix = "Getting technical status for Uninet subscription agreement id = " + subscription.getAgreement();
        try {
            log.debug(logPrefix);
            radiusConnection = radiusDataSource.getConnection();
            st = radiusConnection.prepareStatement("SELECT AC.username,AC.STATUS,AC.STOP_DATE " +
                    "FROM accounts AC WHERE AC.account = ?");
            st.setString(1, subscription.getAgreement());
            rs = st.executeQuery();
            if (rs.next()) {
                boolean activeUser = false;
                technicalStatus = new TechnicalStatus();
                Integer status = rs.getInt(2);
                java.sql.Date tillDate = rs.getDate(3);
                if (status == 1) { //status is active
                    activeUser = true;
                }
                String active = activeUser ? "AVAILABLE" : "NOT AVAILABLE";
                String redirect = (activeUser == false) ? "REDIRECT" : "NOT RESTRICTED";
                log.debug("active and redirect -" + active + " " + redirect);
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
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return false;
    }

    @Override
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return false;
    }

    @Override
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
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        return false;
    }

    @Override
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        int res = -1;
        try {
            log.debug("prepare disconnect call to radius for subscription " + subscription.getAgreement());
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("BEGIN radius_dataplus.crud.disconnect2 (?, ?, ?); END;");
            pr.setString(1, subscription.getAgreement());
            pr.registerOutParameter(2, Types.INTEGER);
            pr.registerOutParameter(3, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(2);
            String msg = pr.getString(3);
            log.debug("disconnect for subscription = " + subscription.getAgreement() + ", res = " + res +
                    ", msg = " + msg);
        } catch (SQLException e) {
            e.printStackTrace();
            log.debug("exception occurred during disconnect for subscription " + subscription.getAgreement(), e);
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception ex) {
                log.error("Cannot disconnect", ex);
            }
        }
        return res == 1;


    }

    @Override
    public boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException {
        return false;
    }

    @Override
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {
        return reprovisionWithMergedProcedure(subscription, subscription.getExpirationDateWithGracePeriod());
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

            callableStatement = radiusConnection.prepareCall("{call crud.update_account(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            callableStatement.setString(1, subscription.getAgreement());
            callableStatement.setString(2, subscription.getSettingByType(ServiceSettingType.USERNAME).getValue());
            callableStatement.setString(3, subscription.getSettingByType(ServiceSettingType.PASSWORD).getValue());
            callableStatement.setNull(4, Types.VARCHAR);

            String slot = getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH_SLOT);
            if (slot != null && !slot.isEmpty()) {
                callableStatement.setInt(5, Integer.parseInt(slot));
            } else {
                callableStatement.setNull(5, Types.INTEGER);
            }//slot

            String port = getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH_PORT);
            if (port != null && !port.isEmpty()) {
                callableStatement.setInt(6, Integer.parseInt(port));
            } else {
                callableStatement.setNull(6, Types.INTEGER);
            }//port

            String settingValue;
            if ((settingValue = getSettingValue(subscription, ServiceSettingType.IP_ADDRESS)) != null) {   //IP - 7
                callableStatement.setString(7, settingValue); //persist static ip.
            } else {
                callableStatement.setString(7, ""); //persist static ip.
            }

            String dslamId = getSettingValue(subscription, ServiceSettingType.BROADBAND_SWITCH);
            if (dslamId != null && !dslamId.isEmpty() && miniPopPersistenceFacade.find(Integer.parseInt(dslamId)) != null) {
                MiniPop miniPop = miniPopPersistenceFacade.find(Integer.parseInt(dslamId));
                callableStatement.setString(8, miniPop.getSwitch_id());
            } else {
                callableStatement.setNull(8, Types.VARCHAR);
            }
//            callableStatement.setString(9, propertyPersistenceFacade.find(
//                    subscription.getService(),
//                    subscription.getSettingByType(ServiceSettingType.ZONE),
//                    subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE)
//            ).getProfileValue());


            callableStatement.setString(9, subscription.getService().getName());


            SubscriptionServiceType subscriptionServiceType = subscriptionServiceTypePersistenceFacade.find
                    (Long.parseLong(subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE).getValue()));


            callableStatement.setString(10, subscriptionServiceType.getProfile().getName().toLowerCase());


            if (subscription.getActivationDate() != null) {
                callableStatement.setTimestamp(11, new Timestamp(subscription.getActivationDate().toDate().getTime()));
            } else {
                callableStatement.setTimestamp(11, null);
            }

            if (expirationWithGrace != null) {
                callableStatement.setTimestamp(12, new Timestamp(expirationWithGrace.toDate().getTime()));
            } else if (subscription.getExpirationDateWithGracePeriod() != null) {
                callableStatement.setTimestamp(12, new Timestamp(subscription.getExpirationDateWithGracePeriod().toDate().getTime()));
            } else {
                callableStatement.setTimestamp(12, null);
            }

            if (subscription.getStatus().equals(SubscriptionStatus.ACTIVE) ||
                    subscription.getStatus().equals(SubscriptionStatus.PARTIALLY_BLOCKED) ||
                    subscription.getStatus().equals(SubscriptionStatus.INITIAL)) {
                callableStatement.setInt(13, 1);    //persist status ACTIVE on radius db
            } else {
                callableStatement.setInt(13, 0);    //persist status NOT_ACTIVE on radius db
            }

            callableStatement.registerOutParameter(14, Types.INTEGER);
            callableStatement.registerOutParameter(15, Types.VARCHAR);
            callableStatement.executeUpdate();

            res = callableStatement.getInt(14);
            msg = callableStatement.getString(15);
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
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return false;
    }

    @Override
    public boolean updateByService(Service service) {
        return false;
    }

    @Override
    public boolean createServiceProfile(Service service) {
        return false;
    }

    @Override
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @Override
    public boolean createNas(Nas nas) {
        return false;
    }

    @Override
    public boolean updateNas(Nas nas) {
        return false;
    }

    @Override
    public boolean updateAttribute(Attribute attribute) {
        return false;
    }

    @Override
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        return null;
    }

    @Override
    public DateTime getActivationDate(Subscription subscription) {
        return null;
    }

    @Override
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @Override
    public boolean removeAccount(Subscription subscription) {
        return false;
    }

    public boolean removeFD(Subscription subscription) {
        return false;
    }

    @Override
    public Usage getUsage(Subscription subscription, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public List<String> getAuthRejectionReasons(Subscription subscription) {

        String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
        log.debug("onine session username " + username);

        Connection radiusConnection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<String> rejectionReasons = new ArrayList<>();
        try {
            String query = "SELECT * FROM AUTH_REJECTS WHERE USERNAME = ? ";
            radiusConnection = radiusDataSource.getConnection();
            statement = radiusConnection.prepareStatement(query);
            statement.setString(1, username);
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
    public boolean provisionIptv(Subscription subscription) {
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        BackProvisionDetails details = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("SELECT dslam, slot, port, mac, password FROM accounts WHERE account = ?");
            st.setString(1, subscription.getAgreement());
            rs = st.executeQuery();

            //log.debug("RESULT: " + rs);

            if (rs.next()) {
                String dslam = rs.getString(1);
                Long slot = rs.getLong(2);
                Long port = rs.getLong(3);
                String mac = rs.getString(4);
                String password = rs.getString(5);
                details = new BackProvisionDetails(dslam, slot, port, mac, password);
            }
            log.info("getBackProvisionDetails method finished successfully for subscription "+subscription.getId()+" -> "+details);

        } catch (Exception ex) {
            log.error(String.format("Error during getBackProvisionDetails on radius for subscription=%s", subscription.getId()), ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception ex) {
                log.error(String.format("Error during getBackProvisionDetails() for subscription=%s", subscription.getId()), ex);
            }
        }

        return details;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void provisionNewAgreements(String oldAgreement, String newAgreement) throws Exception {
        Connection radiusConnection = null;
        CallableStatement callableStatement = null;

        int res = 0;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            callableStatement = radiusConnection.prepareCall("{call crud.change_agreement (?, ?, ?, ?)}");

            callableStatement.setString(1, oldAgreement);
            callableStatement.setString(2, newAgreement);
            callableStatement.registerOutParameter(3, Types.INTEGER);
            callableStatement.registerOutParameter(4, Types.VARCHAR);

            callableStatement.executeUpdate();

            res = callableStatement.getInt(3);
            msg = callableStatement.getString(4);
            log.info(String.format("Subscription agreement change synchronization to radius from = %s. target agr = %s. Msg = %s", oldAgreement, newAgreement, msg));
            if (res != 1) {
                throw new Exception("can not synchronize with radius current agr is " + oldAgreement + " ,message is " + msg);
            }
        } catch (SQLException e) {
            log.error(String.format("Subscription agreement change synchronization to radius from = %s. target agr = %s. Msg = %s, exception: %s", oldAgreement, newAgreement, msg, e.getMessage()), e);
            e.printStackTrace(System.err);
            throw new Exception("could not synchronize , technical problem ," + e.getMessage());
        } finally {
            try {
                if (callableStatement != null) {
                    callableStatement.close();
                }
                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException e) {

            }
        }
    }
}
