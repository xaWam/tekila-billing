package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
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
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import oracle.jdbc.*;

/**
 * Created by shnovruzov on 5/24/2016.
 */
@Stateless(name = "CitynetProvisioner", mappedName = "CitynetProvisioner")
@Local(ProvisioningEngine.class)
public class CitynetProvisioner implements ProvisioningEngine, Serializable {

    @Resource(name = "jdbc/Citynet.Radius")
    private DataSource radiusDataSource;
    @Resource
    private SessionContext ctx;
    private static final Logger log = Logger.getLogger(CitynetProvisioner.class);
    private final static int GENERAL_SERVICE_GROUP = 2;
    private final static int STATIC_IP_SERVICE_GROUP = 1;

    @EJB
    private SystemLogger systemLog;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private Register register;

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
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {

            log.debug("INIT SERVICE");
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

            radiusConnection = radiusDataSource.getConnection();

            Long serviceID = subscription.getService().getId();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            MiniPop miniPop = subscriptionFacade.findMinipop(subscription);
//            log.debug("MINIPOP: " + miniPop);
            Long nasId = miniPop != null ? miniPop.getNas().getId() : -1;

            log.debug("NAS: " + nasId);

            Service service = subscription.getService();

            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();

            log.debug("up=" + up + ", down=" + down + " upUnit=" + upUnit + " downUnit=" + downUnit);

            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();

            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId);
            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

            String lupStr = lup != null && lupUnit != null ? lup + lupUnit : null;
            String ldownStr = ldown != null && ldownUnit != null ? ldown + ldownUnit : null;

            log.debug("DATE:" + new java.sql.Timestamp(register.getEndOfDay().toDate().getTime()));

            pr = radiusConnection.prepareCall("begin radius_core.add_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setString(1, subscription.getAgreement());
            pr.setString(2, username);
            pr.setTimestamp(3, null);
            pr.setTimestamp(4, new java.sql.Timestamp(DateTime.now().plusDays(4).withTime(23, 59, 59, 999).toDate().getTime()));
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName());
            pr.setString(7, up + upUnit);
            pr.setString(8, down + downUnit);
            pr.setString(9, lupStr);
            pr.setString(10, ldownStr);
            pr.setLong(11, nasId);
            pr.setInt(12, 1);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.info(String.format("INIT SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during INIT SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            log.debug(msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during INIT SERVICE for agreement=%s", subscription.getAgreement()), ex);
                return false;
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription) //throws ProvisioningException;
    {
        return openServiceForSubscription(subscription, null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expDate) //throws ProvisioningException;
    {
        return openServiceForSubscription(subscription, expDate, null);
    }

    private boolean openServiceForSubscription(Subscription subscription, DateTime expirationDate, Integer serviceGroupID) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");


            Long serviceID = subscription.getService().getId();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            MiniPop miniPop = subscriptionFacade.findMinipop(subscription);
//            log.debug("MINIPOP: " + miniPop);
            Long nasId = miniPop != null ? miniPop.getNas().getId() : -1;

            Service service = subscription.getService();

            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();

            log.debug("up=" + up + ", down=" + down + " upUnit=" + upUnit + " downUnit=" + downUnit);

            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();

            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId + "minipop: " + miniPop);
            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

            String lupStr = (lup != null && lupUnit != null) ? lup + lupUnit : null;
            String ldownStr = (ldown != null && ldownUnit != null) ? ldown + ldownUnit : null;


            DateTime expDateParameter = null;

            if (expirationDate == null) {
                //expDateParameter =  DateTime.now().plusMonths(2).withTime(23, 59, 59, 999);
                if (subscription.getExpirationDateWithGracePeriod() != null) {
                    expDateParameter = subscription.getExpirationDateWithGracePeriod();
                } else {
                    expDateParameter = subscription.getExpirationDate();
                }

                expDateParameter = expDateParameter.plusMonths(1).withTime(23, 59, 59, 999);
            } else {
                expDateParameter = expirationDate;
            }

            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setString(1, subscription.getAgreement());
            pr.setNull(2, Types.VARCHAR);
            pr.setTimestamp(3, new java.sql.Timestamp(DateTime.now().toDate().getTime()));
            pr.setTimestamp(4, new java.sql.Timestamp(expDateParameter.toDate().getTime()));
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName());
            pr.setString(7, up + upUnit);
            pr.setString(8, down + downUnit);
            pr.setString(9, lupStr);
            pr.setString(10, ldownStr);
            pr.setLong(11, nasId);
            pr.setInt(12, 1);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.debug("MSG: " + msg);
            log.info(String.format("OPEN SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during OPEN SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during OPEN SERVICE for agreement=%s", subscription.getAgreement()), ex);
                return false;
            }
        }
        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeService(Subscription subscription) //throws ProvisioningException;
    {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

            Long serviceID = subscription.getService().getId();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            MiniPop miniPop = subscriptionFacade.findMinipop(subscription);
//            log.debug("MINIPOP: " + miniPop);
            Long nasId = miniPop != null ? miniPop.getNas().getId() : -1;

            Service service = subscription.getService();

//            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
//            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
//            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
//            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();

//            log.debug("up=" + up + ", down=" + down + " upUnit=" + upUnit + " downUnit=" + downUnit);

//            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
//            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
//            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
//            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();

//            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId + "minipop: " + miniPop);
//            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

//            String lupStr = (lup != null && lupUnit != null) ? lup + lupUnit : null;
//            String ldownStr = (ldown != null && ldownUnit != null) ? ldown + ldownUnit : null;


            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setString(1, subscription.getAgreement());
            pr.setNull(2, Types.VARCHAR);
            pr.setString(3, null);
            pr.setTimestamp(4, new java.sql.Timestamp(DateTime.now().toDate().getTime()));
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName());
//            pr.setString(7, up + upUnit);
//            pr.setString(8, down + downUnit);
            pr.setNull(7, Types.VARCHAR);
            pr.setNull(8, Types.VARCHAR);
//            pr.setString(9, lupStr);
//            pr.setString(10, ldownStr);
            pr.setNull(9, Types.VARCHAR);
            pr.setNull(10, Types.VARCHAR);
            pr.setLong(11, nasId);
            pr.setInt(12, 0);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);

            log.info(String.format("CLOSE SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during CLOSE SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during OPEN SERVICE for agreement=%s", subscription.getAgreement()), ex);
                return false;
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

        int res = -1;

        try {

            SubscriptionResource resource = subscription.getActiveResource();
            log.debug("ACTIVE RESOURCE: " + resource);
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
//            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();

//            log.debug(String.format("HERE: collectOnlineStats: user=%s, tariff=%s", username, tariff));

            radiusConnection = radiusDataSource.getConnection();

            Date expirationDate = new Date(DateTime.now().toDate().getTime());
            st = radiusConnection.prepareStatement("select AC.ACC_ID,AC.USERNAME,AC.STATUS, RA.ACCTINPUTOCTETS,RA.ACCTOUTPUTOCTETS,RA.ACCTSTARTTIME,RA.NASIPADDRESS,RA.FRAMEDIPADDRESS,RA.CALLINGSTATIONID,RA.SERVICE,RA.RADACCTID " +
                    "from ncn_accounts ac left join ncn_radacct ra on AC.USERNAME = SUBSTR(RA.USERNAME,21) " +
                    "WHERE AC.ACC_ID = ?");


            st.setString(1, subscription.getAgreement());
            rs = st.executeQuery();

            log.debug("RESULT: " + rs);

            if (rs.next()) {

//                long actualId = -1;
//                do {
//                    if (rs.getString(10) != null && !rs.getString(10).isEmpty()) {
//                        actualId = rs.getLong(11);
//                        break;
//                    }
//                } while (rs.next());
//
//                if (actualId == -1) {
//                    rs.beforeFirst();
//                    rs.next();
//                }

                stats = new OnlineBroadbandStats();
                SimpleDateFormat frm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                stats.setUser(username);
                double upload = rs.getLong(5) / 1024d / 1024d;
                double download = rs.getLong(4) / 1024d / 1024d;
                stats.setDown(String.format("%.3f", upload));
                stats.setUp(String.format("%.3f", download));
                stats.setNasIpAddress(rs.getString(7));
                stats.setFramedAddress(rs.getString(8));
                stats.setCallingStationID(rs.getString(9));
                stats.setService(rs.getString(10));

                try {
                    String startTime = rs.getString(6);

                    if (startTime != null) {
                        stats.setStartTime(frm.parse(startTime));
                    }
                } catch (ParseException ex) {
                    log.error("Cannot parse startdate", ex);
                }
            } else {
                return null;
            }
        } catch (Exception ex) {
            log.error(String.format("Error during collect online stast on radius for agreement=%s", subscription.getAgreement()), ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
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
                log.error(String.format("Error during CLOSE SERVICE for agreement=%s", subscription.getAgreement()), ex);
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
        String logPrefix = "Getting technical status for CityNet subscription agreement id = " + subscription.getAgreement();
        //status = 1 td > this date active
        //eks halda redirect
        try {
            log.debug(logPrefix);
            radiusConnection = radiusDataSource.getConnection();
            st = radiusConnection.prepareStatement("select AC.ACC_ID,AC.STATUS,AC.TD " +
                    "FROM ncn_accounts AC WHERE AC.ACC_ID = ?");
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
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {

        return manageVAS(subscription, vas, null, SubscriptionStatus.ACTIVE);

    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return manageVAS(subscription, vas, sbnVas, SubscriptionStatus.ACTIVE);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas) {
        return manageVAS(subscription, vas, sbnVas, SubscriptionStatus.BLOCKED);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeVAS(Subscription subscription, ValueAddedService vas) {
        return manageVAS(subscription, vas, null, SubscriptionStatus.BLOCKED);
    }

    private boolean manageVAS(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVas, SubscriptionStatus status) {
        switch (vas.getCode().getType()) {
            case PERIODIC_STATIC:
                return manageStaticIP(subscription, vas, sbnVas, status);
            default:
                return false;
        }
    }

    private boolean manageStaticIP(Subscription subscription, ValueAddedService vas, SubscriptionVAS sbnVAS, SubscriptionStatus status) {
        SystemEvent ev = null;
        if (sbnVAS == null) sbnVAS = subscription.getVASByServiceId(vas.getId());
        SubscriptionResource resource = null;
        SubscriptionResourceBucket ipBucket = null;
        //SubscriptionResourceBucket subnetBucket = null;
        String message = null;
        int res = -1;

        if (sbnVAS == null) {
            message = String.format("Subscription to vas not found,  subscription id=%d, vas id=%d, status=%s",
                    subscription.getId(), vas.getId(), status);
            log.error(message);
            throw new IllegalArgumentException(message);
        } else if (vas.getCode().getType() != ValueAddedServiceType.PERIODIC_STATIC
                || (resource = sbnVAS.getResource()) == null
                || (ipBucket = resource.getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS)) == null
                || ipBucket.getCapacity() == null //|| (subnetBucket = resource.getBucketByType(ResourceBucketType.INTERNET_SUBNET)) == null
            //|| subnetBucket == null
                ) {
            message = String.format("VAS lacks required attributes (type, resource or buckets),  subscription id=%d, vas id=%d, status=%s",
                    subscription.getId(), vas.getId(), status);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (status == SubscriptionStatus.BLOCKED) {
            ev = SystemEvent.VAS_STATUS_BLOCK;
        } else if (status == SubscriptionStatus.ACTIVE) {
            ev = SystemEvent.VAS_STATUS_ACTIVE;
        } else {
            message = String.format("status unknown,  subscription id=%d, vas id=%d, status=%s",
                    subscription.getId(), vas.getId(), status);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        log.debug("IP ADDRES: " + ipBucket.getCapacity());

        try {
            if (status == SubscriptionStatus.ACTIVE) {
                log.info("manageVAS: starting ACTIVATE STATIC IP request.....");
                res = activateStaticIP(subscription, ipBucket.getCapacity(), sbnVAS);
                log.info(String.format("manageVAS: ACTIVATE STATIC IP request finished. ACTIVATE STATIC IP returned result %d", res));

                if (res == 1) {
                    log.info("manageVAS: starting OPEN SERVICE request.....");
                    openServiceForSubscription(subscription, subscription.getExpirationDate(), STATIC_IP_SERVICE_GROUP);
                    log.info("manageVAS: OPEN SERVICE request finished");
                }
            } else if (status == SubscriptionStatus.BLOCKED) {
                res = blockStaticIP(subscription, ipBucket.getCapacity(), sbnVAS);
                if (res == 1) {
                    openServiceForSubscription(subscription, subscription.getExpirationDate(), GENERAL_SERVICE_GROUP);
                }
            }

            message = String.format("MANAGE VAS returned result %d,  subscription id=%d, vas id=%d, status=%s",
                    res, subscription.getId(), vas.getId(), status);

            log.info(message);

            if (res == 1) {
                sbnVAS.setStatus(SubscriptionStatus.ACTIVE);
            }
        } catch (SQLException ex) {
            log.error(String.format("Cannot manage STATIC IP address: subscription id=%d, agreement=%s, status=%s, ip address=%s",
                    subscription.getId(), subscription.getAgreement(), status, ipBucket.getCapacity()), ex);

            return false;
        }

        log.debug("FINISHED");
        return res == 1;
    }

    private int activateStaticIP(Subscription subscription, String ipAddress, SubscriptionVAS vas) throws SQLException {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.add_vas (?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setLong(1, vas != null ? vas.getId() : 0);
            pr.setString(2, subscription.getAgreement());
            pr.setString(3, "Framed-IP-Address");
            pr.setString(4, ipAddress);
            pr.setDate(5, new Date(DateTime.now().toDate().getTime()));
            pr.setDate(6, new Date(register.getDefaultExpirationDate().toDate().getTime()));
            pr.registerOutParameter(7, OracleTypes.INTEGER);
            pr.registerOutParameter(8, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(7);
            msg = pr.getString(8);

            log.debug(String.format("activateStaticIP: request returned %d %s", res, msg));
            return res;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resources", ex);
            }
        }
    }

    private int blockStaticIP(Subscription subscription, String ipAddress, SubscriptionVAS vas) throws SQLException {

        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        log.debug("Block Vas: " + vas);

        try {
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.update_vas (?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setLong(1, vas.getId());
            pr.setString(2, subscription.getAgreement());
            pr.setString(3, "Framed-IP-Address");
            pr.setString(4, "-1");
            pr.setDate(5, new Date(DateTime.now().toDate().getTime()));
            pr.setDate(6, new Date(DateTime.now().toDate().getTime()));
            pr.registerOutParameter(7, OracleTypes.INTEGER);
            pr.registerOutParameter(8, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(7);
            msg = pr.getString(8);

            log.debug(String.format("block Static IP: request returned %d %s", res, msg));
            return res;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resources", ex);
            }
        }

    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            MiniPop miniPop = subscriptionFacade.findMinipop(subscription);

            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");

            log.debug("Change minipop: new username: " + newValue);

            pr.setString(1, subscription.getAgreement());
            pr.setString(2, newValue);
            pr.setDate(3, null);
            pr.setDate(4, null);
            pr.setNull(5, Types.INTEGER);
            pr.setNull(6, Types.NVARCHAR);
            pr.setNull(7, Types.NVARCHAR);
            pr.setNull(8, Types.NVARCHAR);
            pr.setNull(9, Types.NVARCHAR);
            pr.setNull(10, Types.NVARCHAR);
            pr.setLong(11, miniPop.getNas().getId());
            pr.setNull(12, Types.INTEGER);
            pr.setNull(13, Types.INTEGER);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.debug("MSG: " + msg);
            log.info(String.format("Change minipop for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during minipop service on radius for agreement=%s", subscription.getAgreement()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during change service for agreement=%s", subscription.getAgreement()), ex);
                return false;
            }
        }
        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        int res = -1;
        try {
            log.debug("prepare disconnect call to radius for subscription " + subscription.getAgreement());
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.disconnect (?, ?, ?); end;");
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
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) throws ProvisioningException {
        return "OFFLINE";
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            Long serviceID = subscription.getService().getId();
            Service service = subscription.getService();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();
            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();
            Long nasId = subscriptionFacade.findMinipop(subscription).getNas().getId();

            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId);
            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");

            log.debug("CONN SUCCESS");

            String lupStr = lup != null && lupUnit != null ? lup + lupUnit : null;
            String ldownStr = ldown != null && ldownUnit != null ? ldown + ldownUnit : null;

            pr.setString(1, subscription.getAgreement());
            pr.setString(2, username);
            pr.setDate(3, null);
            pr.setDate(4, null);
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName());
            pr.setString(7, up + upUnit);
            pr.setString(8, down + downUnit);
            pr.setString(9, lupStr);
            pr.setString(10, ldownStr);
            pr.setLong(11, nasId);
            pr.setObject(12, null);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.debug("MSG: " + msg);
            log.info(String.format("Change service for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during change service on radius for agreement=%s", subscription.getAgreement()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during change service for agreement=%s", subscription.getAgreement()), ex);
                return false;
            }
        }
        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    private boolean reprovisionWithMergedProcedure(Subscription subscription, DateTime expirationWithGrace) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            Long serviceID = subscription.getService().getId();
            Service service = subscription.getService();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();
            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();
            Long nasId = subscriptionFacade.findMinipop(subscription).getNas().getId();

            log.debug("provisioning started for agreement id " + subscription.getAgreement());
            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId);
            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

            pr = radiusConnection.prepareCall("begin radius_core.SYNCHRONIZE_ACCOUNTS (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");

            log.debug("Radius Connection successful");

            int status;

            if (subscription.getStatus() == SubscriptionStatus.BLOCKED ||
                    subscription.getStatus() == SubscriptionStatus.FINAL) {
                status = 0;
            } else {
                status = 1;
            }

            pr.setString(1, subscription.getAgreement());
            pr.setString(2, username);

            java.sql.Timestamp fDate = null;
            DateTime activationDate;
            if ((activationDate = subscription.getActivationDate()) != null) {
                fDate = new java.sql.Timestamp(activationDate.getMillis());
            }
            pr.setTimestamp(3, fDate);

            java.sql.Timestamp tDate = null;
            if (expirationWithGrace != null) {
                tDate = new java.sql.Timestamp(expirationWithGrace.getMillis());
            }
            pr.setTimestamp(4, tDate);
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName()); //this will not be used if subscription exists in NCN_ACCOUNTS
            pr.setString(7, up + upUnit);
            pr.setString(8, down + downUnit);
            pr.setString(9, lup + lupUnit);
            pr.setString(10, ldown + ldownUnit);
            pr.setLong(11, nasId);
            pr.setInt(12, status);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.debug("MSG: " + msg);
            log.info(String.format("Update account for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during Update account on radius for agreement=%s, subscription=%s", subscription.getAgreement(), subscription.getId()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during Update account for agreement=%s, subscription=%s", subscription.getAgreement(), subscription.getId()), ex);
                return false;
            }
        }
        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {
        return reprovisionWithMergedProcedure(subscription, subscription.getExpirationDateWithGracePeriod());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return reprovisionWithMergedProcedure(subscription, endDate);
    }

    private boolean checkAccount(Subscription subscription) {

        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            String sqlQuery = "SELECT COUNT(*) as total FROM NCN_ACCOUNTS WHERE ACC_ID = " + subscription.getAgreement();
            pr = radiusConnection.prepareCall(sqlQuery);
            ResultSet rs = pr.executeQuery();
            rs.next();

            if (rs.getInt("total") > 0)
                return true;
            return false;

        } catch (Exception ex) {
            log.debug("cannot reprovise sbn: " + subscription.getId() + " " + ex);
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during reprovisioning subscription: " + subscription.getId() + " " + ex);
                return false;
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createNas(Nas nas) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            String query = "begin radius_core.add_nas (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;";
            pr = radiusConnection.prepareCall(query);
            pr.setLong(1, nas.getId());
            pr.setString(2, null);
            pr.setString(3, nas.getDesc());
            pr.setString(4, nas.getIP());
            pr.setInt(5, 0);
            pr.setString(6, nas.getSecretKey());
            pr.setString(7, null);
            pr.setString(8, nas.getName());
            pr.setString(9, null);
            pr.registerOutParameter(10, Types.INTEGER);
            pr.registerOutParameter(11, Types.VARCHAR);
            pr.execute();
            res = pr.getInt(10);
            msg = pr.getString(11);

            log.debug("Create nas result: " + res + " msg: " + msg);

            if (res == 1) {
                for (Attribute attribute : nas.getAttributeList())
                    createAttribute(attribute, nas.getId());
            }

        } catch (Exception ex) {
            log.debug("cannot add nas: " + nas.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning create nas: " + nas.getId() + " " + ex);
                return false;
            }
        }
        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateNas(Nas nas) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            log.debug("nas=" + nas);

            String query = "begin radius_core.update_nas (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;";
            pr = radiusConnection.prepareCall(query);
            pr.setLong(1, nas.getId());
            pr.setString(2, null);
            pr.setString(3, nas.getDesc());
            pr.setString(4, nas.getIP());
            pr.setInt(5, 0);
            pr.setString(6, nas.getSecretKey());
            pr.setString(7, null);
            pr.setString(8, nas.getName());
            pr.setString(9, null);
            pr.registerOutParameter(10, Types.INTEGER);
            pr.registerOutParameter(11, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(10);
            msg = pr.getString(11);

            log.debug("Update nas result: " + res + " msg: " + msg);

            boolean isDeleted = false;
            String sqlQuery = "delete from NCN_NASPROFILES WHERE NASID = " + nas.getId();
            pr = radiusConnection.prepareCall(sqlQuery);

            try {
                pr.execute();
                isDeleted = true;
                log.debug("Deleted attributes");
            } catch (Exception ex) {
                log.debug("Cannot delete attributes for nas: " + nas.getId());
            }

            if (res == 1 && isDeleted) {
                for (Attribute attribute : nas.getAttributeList()) {
                    log.debug(attribute.getName() + " " + nas.getId());
                    createAttribute(attribute, nas.getId());
                }
            }

            log.debug("Created attributes");

        } catch (Exception ex) {
            log.debug("cannot update nas: " + nas.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning update nas: " + nas.getId() + " " + ex);
                return false;
            }
        }
        return res == 1;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createAttribute(Attribute attribute, Long nasId) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {

            radiusConnection = radiusDataSource.getConnection();

            String proc = "begin radius_core.add_attribute (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;";
            pr = radiusConnection.prepareCall(proc);
            pr.setLong(1, attribute.getId());
            pr.setLong(2, nasId);
            pr.setString(3, attribute.getName());
            pr.setString(4, attribute.getValue());
            pr.setString(5, attribute.getDescription());
            pr.setInt(6, attribute.getTag());
            pr.setDate(7, new Date(DateTime.now().toDate().getTime()));
            pr.setDate(8, new Date(DateTime.now().plusYears(50).toDate().getTime()));
            pr.setInt(9, attribute.getStatus());
            pr.registerOutParameter(10, OracleTypes.INTEGER);
            pr.registerOutParameter(11, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(10);

        } catch (Exception ex) {
            log.debug("cannot create attribute: " + attribute.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning create attribute: " + attribute + " " + ex);
                return false;
            }
            return res == 1;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAttribute(Attribute attribute) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.update_attribute (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setLong(1, attribute.getId());
            pr.setObject(2, null);
            pr.setString(3, attribute.getName());
            pr.setString(4, attribute.getValue());
            pr.setString(5, attribute.getDescription());
            pr.setInt(6, attribute.getTag());
            pr.setDate(7, new Date(DateTime.now().toDate().getTime()));
            pr.setDate(8, new Date(DateTime.now().plusYears(50).toDate().getTime()));
            pr.setInt(9, attribute.getStatus());
            pr.registerOutParameter(10, OracleTypes.INTEGER);
            pr.registerOutParameter(11, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(10);
            msg = pr.getString(11);

            log.debug("Update attribute provisioner result: " + res + " msg: " + msg);

        } catch (Exception ex) {
            log.debug("cannot update attribute: " + attribute.getId() + " msg: " + msg + "exp: " + ex);
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning update attribute: " + attribute.getId() + " " + ex);
            }
            return res == 1;
        }
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
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
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
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            Long serviceID = subscription.getService().getId();
            Service service = subscription.getService();
            SubscriptionResource resource = subscription.getActiveResource();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            String up = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String lup = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String upUnit = resource.getBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();
            String lupUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = resource.getBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();
            Long nasId = subscriptionFacade.findMinipop(subscription).getNas().getId();

            log.debug("service=" + service + " user=" + username + ", tariff=" + down + " nasid= " + nasId);
            log.debug("up=" + up + ", down=" + down + " lup= " + lup + " ldown=" + ldown + " upUnit=" + upUnit + " downUnit=" + downUnit + " lupUnit=" + lupUnit + " ldownUnit=" + ldownUnit);

            String lupStr = lup != null && lupUnit != null ? lup + lupUnit : null;
            String ldownStr = ldown != null && ldownUnit != null ? ldown + ldownUnit : null;

            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); end;");

            log.debug("CONN SUCCESS");

            int status;

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE)
                status = 2;
            else if (subscription.getStatus() == SubscriptionStatus.INITIAL)
                status = 1;
            else status = 0;

            pr.setString(1, subscription.getAgreement());
            pr.setString(2, username);
            pr.setDate(3, null);
            pr.setDate(4, null);
            pr.setLong(5, subscription.getService().getProvider().getId());
            pr.setString(6, ctx.getCallerPrincipal().getName());
            pr.setString(7, up + upUnit);
            pr.setString(8, down + downUnit);
            pr.setString(9, lupStr);
            pr.setString(10, ldownStr);
            pr.setLong(11, nasId);
            pr.setInt(12, status);
            pr.setLong(13, serviceID);
            pr.registerOutParameter(14, Types.INTEGER);
            pr.registerOutParameter(15, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(14);
            msg = pr.getString(15);

            log.debug("MSG: " + msg);
            log.info(String.format("Update account for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
            //systemLog.success(SystemEvent.SERVICE_INITIALIZED, subscription, msg);

        } catch (Exception ex) {
            log.error(String.format("Error during Update account on radius for agreement=%s", subscription.getAgreement()), ex);
            //systemLog.error(SystemEvent.SERVICE_INITIALIZED, subscription, msg);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during Update account for agreement=%s", subscription.getAgreement()), ex);
                return false;
            }
        }
        return res == 1;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(Service service) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            for (ServiceProfile serviceProfile : service.getServiceProfileList()) {
                try {
                    pr = radiusConnection.prepareCall("begin radius_core.add_service_profile (?, ?, ?, ?, ?, ?, ?, ?); end;");
                    pr.setLong(1, serviceProfile.getId());
                    pr.setString(2, serviceProfile.getFrom());
                    pr.setString(3, serviceProfile.getTo());
                    pr.setDouble(4, serviceProfile.getUp());
                    pr.setDouble(5, serviceProfile.getDown());
                    pr.setLong(6, serviceProfile.getService().getId());
                    pr.registerOutParameter(7, OracleTypes.INTEGER);
                    pr.registerOutParameter(8, OracleTypes.VARCHAR);
                    pr.execute();
                    res = pr.getInt(7);
                } catch (Exception ex) {
                    log.debug("cannot create service profileID: " + serviceProfile.getId() + " " + ex);
                    return false;
                }
            }

        } catch (Exception ex) {
            log.debug("cannot create serviceID: " + service.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning create serviceID: " + service.getId() + " " + ex);
            }
            return res == 1;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.add_service_profile (?, ?, ?, ?, ?, ?, ?, ?); end;");
            pr.setLong(1, serviceProfile.getId());
            pr.setString(2, serviceProfile.getFrom());
            pr.setString(3, serviceProfile.getTo());
            pr.setDouble(4, serviceProfile.getUp());
            pr.setDouble(5, serviceProfile.getDown());
            pr.setLong(6, serviceProfile.getService().getId());
            pr.registerOutParameter(7, OracleTypes.INTEGER);
            pr.registerOutParameter(8, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(7);

        } catch (Exception ex) {
            log.debug("cannot create serviceProfile: " + serviceProfile.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning create serviceProfileID: " + serviceProfile.getId() + " " + ex);
            }
            return res == 1;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            if (oper == 0) {
                pr = radiusConnection.prepareCall("begin radius_core.update_service_profile (?, ?, ?, ?, ?, ?, ?, ?); end;");
                pr.setLong(1, serviceProfile.getId());
                pr.setString(2, serviceProfile.getFrom());
                pr.setString(3, serviceProfile.getTo());
                pr.setDouble(4, serviceProfile.getUp());
                pr.setDouble(5, serviceProfile.getDown());
                pr.setLong(6, serviceProfile.getService().getId());
                pr.registerOutParameter(7, OracleTypes.INTEGER);
                pr.registerOutParameter(8, OracleTypes.VARCHAR);
                pr.execute();
                res = pr.getInt(7);
            } else {
                String sqlQuery = "delete from NCN_SERVICE_PROFILE WHERE ID = " + serviceProfile.getId();
                pr = radiusConnection.prepareCall(sqlQuery);
                pr.execute();
                res = 1;
            }
        } catch (Exception ex) {
            log.debug("cannot update service profile: " + serviceProfile.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning update service profile: " + serviceProfile.getId() + " " + ex);
                return false;
            }
            return res == 1;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateByService(Service service) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        PreparedStatement pst = null;
        ResultSet prs = null;
        int res = -1;
        String msg = null;

        try {
            radiusConnection = radiusDataSource.getConnection();
            String up = service.getResourceBucketByType(ResourceBucketType.INTERNET_UP).getCapacity();
            String down = service.getResourceBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            String lup = service.getResourceBucketByType(ResourceBucketType.INTERNET_LUP).getCapacity();
            String ldown = service.getResourceBucketByType(ResourceBucketType.INTERNET_LDOWN).getCapacity();
            String upUnit = service.getResourceBucketByType(ResourceBucketType.INTERNET_UP).getUnit();
            String downUnit = service.getResourceBucketByType(ResourceBucketType.INTERNET_DOWN).getUnit();
            String lupUnit = service.getResourceBucketByType(ResourceBucketType.INTERNET_LUP).getUnit();
            String ldownUnit = service.getResourceBucketByType(ResourceBucketType.INTERNET_LDOWN).getUnit();

            String lupStr = lup != null && lupUnit != null ? lup + lupUnit : null;
            String ldownStr = ldown != null && ldownUnit != null ? ldown + ldownUnit : null;

            log.debug(up + " " + down + " " + lup + " " + ldown);

            pr = radiusConnection.prepareCall("begin radius_core.update_by_service (?, ?, ?, ?, ?, ?, ?); end;");
            pr.setLong(1, service.getId());
            pr.setString(2, up + upUnit);
            pr.setString(3, down + downUnit);
            pr.setString(4, lupStr);
            pr.setString(5, ldownStr);
            pr.registerOutParameter(6, OracleTypes.INTEGER);
            pr.registerOutParameter(7, OracleTypes.VARCHAR);
            pr.execute();
            res = pr.getInt(6);

        } catch (Exception ex) {
            log.debug("cannot update service: " + service.getId() + " " + ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (pst != null) {
                    pst.close();
                }

                if (prs != null) {
                    prs.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during provisioning update service: " + service.getId() + " " + ex);
                return false;
            }
            return res == 1;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DateTime getActivationDate(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        ResultSet prs = null;
        long fd = 0;

        try {
            radiusConnection = radiusDataSource.getConnection();

            String sqlQuery = "SELECT FD FROM NCN_ACCOUNTS WHERE ACC_ID = '" + subscription.getAgreement() + "'";
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
        Connection radiusConnection = null;
        CallableStatement st = null;
        try {
            radiusConnection = radiusDataSource.getConnection();
            String sqlQuery = "begin radius_core.REMOVE_ACCOUNT(?, ?, ?); end;";
            st = radiusConnection.prepareCall(sqlQuery);
            st.setString(1, subscription.getAgreement());
            st.registerOutParameter(2, OracleTypes.INTEGER);
            st.registerOutParameter(3, OracleTypes.VARCHAR);
            st.execute();
            int retval = st.getInt(2);
            String retmsg = st.getString(3);

            log.info(retmsg);

            return retval == 1;
        } catch (SQLException ex) {
            log.error(ex);
            return false;
        } finally {
            try {
                if (st != null) {
                    st.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during remove account on citynet radius: " + subscription.getAgreement() + " " + ex);
            }
        }
    }

    public boolean removeFD(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement st = null;
        try {
            radiusConnection = radiusDataSource.getConnection();
            String sqlQuery = "begin radius_core.REMOVE_FD(?, ?, ?); end;";
            st = radiusConnection.prepareCall(sqlQuery);
            st.setString(1, subscription.getAgreement());
            st.registerOutParameter(2, OracleTypes.INTEGER);
            st.registerOutParameter(3, OracleTypes.VARCHAR);
            st.execute();
            int retval = st.getInt(2);
            String retmsg = st.getString(3);

            log.info(retmsg);

            return retval == 1;
        } catch (SQLException ex) {
            log.error(ex);
            return false;
        } finally {
            try {
                if (st != null) {
                    st.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during remove account on citynet radius: " + subscription.getAgreement() + " " + ex);
            }
        }
    }



    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Usage getUsage(Subscription subscription, java.util.Date startDate, java.util.Date endDate) {
        Connection radiusConnection = null;
        CallableStatement st = null;
        try {
            radiusConnection = radiusDataSource.getConnection();
            String sqlQuery = "begin NCNRADIUS.get_usage(?, ?, ?, ?, ?,?,?); end;";
            st = radiusConnection.prepareCall(sqlQuery);
            st.setString(1, subscription.getAgreement());
            st.setTimestamp(2, new java.sql.Timestamp(startDate.getTime()));
            st.setTimestamp(3, new java.sql.Timestamp(endDate.getTime()));
            st.registerOutParameter(4, OracleTypes.INTEGER);
            st.registerOutParameter(5, OracleTypes.INTEGER);
            st.registerOutParameter(6, OracleTypes.VARCHAR);
            st.registerOutParameter(7, OracleTypes.VARCHAR);
            st.execute();

            return new Usage(st.getInt(4), st.getInt(5), st.getString(6), st.getString(7));

        } catch (SQLException ex) {
            log.error(ex);
            return null;
        } finally {
            try {
                if (st != null) {
                    st.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error("Exception occured during getUsage on citynet radius: " + subscription.getAgreement() + " " + ex);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> getAuthRejectionReasons(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean provisionIptv(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement pr = null;
        int res = -1;
        String msg;

        try {
            radiusConnection = radiusDataSource.getConnection();

            pr = radiusConnection.prepareCall("begin radius_core.update_account (?, ?, ?, ?); end;");

            log.debug("CONN SUCCESS");

            pr.setString(1, subscription.getAgreement());
            pr.setLong(2, subscription.isIptvOwner() ? 1L : 0L);
            pr.registerOutParameter(3, Types.INTEGER);
            pr.registerOutParameter(4, Types.VARCHAR);
            pr.execute();

            res = pr.getInt(3);
            msg = pr.getString(4);

            log.debug("MSG: " + msg);
            log.info(String.format("provisionIptv for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));

        } catch (Exception ex) {
            log.error(String.format("Error during provisionIptv on radius for agreement=%s", subscription.getAgreement()), ex);
            return false;
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }

                if (radiusConnection != null) {
                    radiusConnection.close();
                }
            } catch (SQLException ex) {
                log.error(String.format("Error during provisionIptv for agreement=%s", subscription.getAgreement()), ex);
            }
        }
        return res == 1;
    }

    @Override
    public BackProvisionDetails getBackProvisionDetails(Subscription subscription) {
        return null;
    }

    @Override
    public void provisionNewAgreements(String oldAgreement, String newAgreement) {

    }
}
