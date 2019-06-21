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
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.external.StatusElementType;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.broadband.entity.Usage;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by ShakirG on 10/09/2018.
 */


@Stateless(name = "QutuNarHomeProvisioner", mappedName = "QutuNarHomeProvisioner")
@Local(ProvisioningEngine.class)
public class QutuNarHomeProvisioner implements ProvisioningEngine {

    @EJB
    private SystemLogger systemLog;

    @Resource(name = "jdbc/Azertelekom.Radius")
    private DataSource radiusDataSource;
    @Resource
    private SessionContext ctx;
    private static final Logger log = Logger.getLogger(com.jaravir.tekila.provision.broadband.AzertelekomProvisioner.class);
    private final static int GENERAL_SERVICE_GROUP = 2;
    private final static int STATIC_IP_SERVICE_GROUP = 1;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private Register register;

    /*
    * Pre-creates subscription on the external node in order for the customer to be able to connect
    * to the internet in the limited redirect status (so that he/she can pay online for the service
    * to fully activate it)
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean ping() {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean initService(Subscription subscription) //throws ProvisioningException
    {
        Connection radiusConnection = null;
        CallableStatement st = null;
        PreparedStatement pst = null;
        ResultSet prs = null;

        int res = -1;

        try {
            /* if (subscription.getType() != SubscriptionType.STATIC)
                throw new ProvisioningException("Subscription must be static");
             */
            SubscriptionResource resource = subscription.getActiveResource();
            //String username = resource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).getCapacity();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();

            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();

            log.debug("user=" + username + ", tariff=" + tariff);
            radiusConnection = radiusDataSource.getConnection();

            pst = radiusConnection.prepareStatement("select AccountTypeID from AccountTypes where Value = ? and ServiceGroupID = ?");
            pst.setString(1, tariff);
            pst.setInt(2, GENERAL_SERVICE_GROUP);
            prs = pst.executeQuery();

            Long tarifID = null;

            if (prs.next()) {
                tarifID = prs.getLong(1);
            }

            if (tarifID == null) {
                log.error(String.format("initService: AccontTypeID for tarif %s not found", tariff));
                return false;
            }

            log.debug(String.format("initService: tarif %s corresponds to AccontTypeID %d", tariff, tarifID));
            String expirationDateAsString = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            log.debug("subscription.getIdentifier() "+subscription.getIdentifier()+ " username"+username+" "+tarifID+
                    " expirationDateAsString"+expirationDateAsString+" ctx.getCallerPrincipal().getName()"+ctx.getCallerPrincipal().getName());

            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            st = radiusConnection.prepareCall("{CALL AccountProcess (?, ?, ?, ?, 0, ?, ?, ?)}");
            st.setLong(1, Long.valueOf(subscription.getIdentifier()));
            st.setString(2, username);
            st.setLong(3, tarifID);
            st.setString(4, expirationDateAsString);
            st.setInt(5, 1);
            st.setString(6, ctx.getCallerPrincipal().getName());
            st.registerOutParameter(7, Types.BIGINT);
            st.executeQuery();
            res = (int) st.getLong(7);
            //res = st.executeUpdate();
            /*st = radiusConnection.prepareCall("{CALL rad_user_action(?, 1, ?)}");
            st.setString(1, username);
            st.setLong(2, Long.valueOf(tariff));
            st.execute();
            res = st.getUpdateCount();*/

            log.info(String.format("INIT SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
        } catch (SQLException ex) {
            log.error(String.format("Error during INIT SERVICE on radius for agreement=%s "+ex, subscription.getAgreement()));
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        } finally {
            try {
                if (st != null) {
                    st.close();
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
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription) //throws ProvisioningException
    {
        return openServiceForSubscription(subscription, null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openService(Subscription subscription, DateTime expDate) //throws ProvisioningException
    {
        return openServiceForSubscription(subscription, expDate, null);
    }

    private boolean openServiceForSubscription(Subscription subscription, DateTime expirationDate, Integer serviceGroupID) {
        Connection radiusConnection = null;
        CallableStatement st = null;
        PreparedStatement pst = null;
        ResultSet prs = null;

        int res = -1;

        try {

            log.debug("Staring provisioning Azertelekom, agreement=" + subscription.getAgreement());

            /* if (subscription.getType() != SubscriptionType.STATIC)
                throw new ProvisioningException("Subscription must be static");
             */
            SubscriptionResource resource = subscription.getActiveResource();
            //String username = resource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).getCapacity();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();

            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            int serviceGroupIDParameter = serviceGroupID == null ? getServiceGroup(subscription) : serviceGroupID;

            log.debug("user=" + username + ", tariff=" + tariff + ", serviceGroupID=" + serviceGroupIDParameter);

            radiusConnection = radiusDataSource.getConnection();
            //DateTime expDateParameter = expirationDate == null ? DateTime.now().plusMonths(1).withTime(23, 59, 59, 999) : expirationDate;

            DateTime expDateParameter = null;


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

                expDateParameter = expDateParameter.withTime(23, 59, 59, 999).plusMonths(1).plusHours(2);
                log.debug("EXP AFTER PLUS: " + expDateParameter);
            } else {
                log.debug("ELSE");
                expDateParameter = expirationDate;
            }


            log.debug("RESULT: " + expirationDate);
            String expirationDateAsString = expDateParameter.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");

            pst = radiusConnection.prepareStatement("select AccountTypeID from AccountTypes where Value = ? and ServiceGroupID = ?");
            pst.setString(1, tariff);
            pst.setInt(2, serviceGroupIDParameter);
            prs = pst.executeQuery();

            Long tarifID = null;

            if (prs.next()) {
                tarifID = prs.getLong(1);
            }

            if (tarifID == null) {
                log.error(String.format("openService: AccontTypeID for tarif %s not found", tariff));
                return false;
            }

            log.debug(String.format("openService: tarif %s corresponds to AccontTypeID %d", tariff, tarifID));

            st = radiusConnection.prepareCall("{CALL AccountProcess (?, NULL, ?, ?, 0, ?, ?, ?)}");
            st.setLong(1, Long.valueOf(subscription.getAgreement()));
            //st.setString(2, username);
            st.setLong(2, tarifID);
            st.setString(3, expirationDateAsString);
            st.setInt(4, 1);
            st.setString(5, ctx.getCallerPrincipal().getName());
            st.registerOutParameter(6, Types.BIGINT);
            st.executeQuery();
            res = (int) st.getLong(6);
            //res = st.executeUpdate();
            /*st = radiusConnection.prepareCall("{CALL rad_user_action(?, 1, ?)}");
            st.setString(1, username);
            st.setLong(2, Long.valueOf(tariff));
            st.execute();
            res = st.getUpdateCount();*/
            log.debug(String.format("OPEN SERVICE: agreement=%s, {CALL AccountProcess (%s, NULL, %d, '%s', 0, 1, '%s', @Res)}",
                    subscription.getAgreement(), subscription.getAgreement(), tarifID, expirationDateAsString, ctx.getCallerPrincipal().getName()));
            log.info(String.format("OPEN SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
        } catch (Exception ex) {
            log.error(String.format("Error during OPEN SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        } finally {
            try {
                if (st != null) {
                    st.close();
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
            } catch (Exception ex) {
                log.error(String.format("Error during OPEN SERVICE for agreement=%s", subscription.getAgreement()), ex);
            }
        }

        return res == 1;
    }

    private boolean changeServiceOnRadius(Subscription subscription) {
        Connection radiusConnection = null;
        CallableStatement st = null;
        PreparedStatement pst = null;
        ResultSet prs = null;

        int res = -1;

        try {
            /* if (subscription.getType() != SubscriptionType.STATIC)
                throw new ProvisioningException("Subscription must be static");
             */
            SubscriptionResource resource = subscription.getActiveResource();
            //String username = resource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).getCapacity();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();

            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();
            int serviceGroupIDParameter = getServiceGroup(subscription);

            log.debug("user=" + username + ", tariff=" + tariff + ", serviceGroupID=" + serviceGroupIDParameter);

            radiusConnection = radiusDataSource.getConnection();
            //DateTime expDateParameter = expirationDate == null ? DateTime.now().plusMonths(1).withTime(23, 59, 59, 999) : expirationDate;

            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            pst = radiusConnection.prepareStatement("select AccountTypeID from AccountTypes where Value = ? and ServiceGroupID = ?");
            pst.setString(1, tariff);
            pst.setInt(2, serviceGroupIDParameter);
            prs = pst.executeQuery();

            Long tarifID = null;

            if (prs.next()) {
                tarifID = prs.getLong(1);
            }

            if (tarifID == null) {
                log.error(String.format("openService: AccontTypeID for tarif %s not found", tariff));
                return false;
            }

            log.debug(String.format("openService: tarif %s corresponds to AccontTypeID %d", tariff, tarifID));

            st = radiusConnection.prepareCall("{CALL AccountProcess (?, NULL, ?, (select ExpireDate from Accounts where AccountId = ?), 0, ?, ?, ?)}");
            st.setLong(1, Long.valueOf(subscription.getAgreement()));
            //st.setString(2, username);
            st.setLong(2, tarifID);
            st.setLong(3, Long.valueOf(subscription.getAgreement()));
            st.setInt(4, 1);
            st.setString(5, ctx.getCallerPrincipal().getName());
            st.registerOutParameter(6, Types.BIGINT);
            st.executeQuery();
            res = (int) st.getLong(6);
            //res = st.executeUpdate();
            /*st = radiusConnection.prepareCall("{CALL rad_user_action(?, 1, ?)}");
            st.setString(1, username);
            st.setLong(2, Long.valueOf(tariff));
            st.execute();
            res = st.getUpdateCount();*/
            log.debug(String.format("CHANGE SERVICE: agreement=%s, {CALL AccountProcess (%s, NULL, %d, (select ExpireDate from Accounts where AccountId = %s), 0, 1, '%s', @Res)}",
                    subscription.getAgreement(), subscription.getAgreement(), tarifID, subscription.getAgreement(), ctx.getCallerPrincipal().getName()));
            log.info(String.format("CHANGE SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
        } catch (SQLException ex) {
            log.error(String.format("Error during CHANGE SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        } finally {
            try {
                if (st != null) {
                    st.close();
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
                log.error(String.format("Error during CHANGE SERVICE for agreement=%s", subscription.getAgreement()), ex);
            }
        }

        return res == 1;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean closeService(Subscription subscription) //throws ProvisioningException
    {
        Connection radiusConnection = null;
        CallableStatement st = null;
        PreparedStatement pst = null;
        ResultSet prs = null;

        int res = -1;

        try {
            /* if (subscription.getType() != SubscriptionType.STATIC)
                throw new ProvisioningException("Subscription must be static");
             */
            SubscriptionResource resource = subscription.getActiveResource();
            //String username = resource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).getCapacity();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();

            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();

            log.debug("user=" + username + ", tariff=" + tariff);

            int serviceGroupIDParameter = getServiceGroup(subscription);

            log.debug("user=" + username + ", tariff=" + tariff + ", serviceGroupID=" + serviceGroupIDParameter);

            radiusConnection = radiusDataSource.getConnection();
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");

            pst = radiusConnection.prepareStatement("select AccountTypeID from AccountTypes where Value = ? and ServiceGroupID = ?");
            pst.setString(1, tariff);
            pst.setInt(2, serviceGroupIDParameter);
            prs = pst.executeQuery();

            Long tarifID = null;

            if (prs.next()) {
                tarifID = prs.getLong(1);
            }

            if (tarifID == null) {
                log.error(String.format("closeService: AccontTypeID for tarif %s not found", tariff));
                return false;
            }

            log.debug(String.format("closeService: tarif %s corresponds to AccontTypeID %d", tariff, tarifID));

            radiusConnection = radiusDataSource.getConnection();
            String expirationDateAsString = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            st = radiusConnection.prepareCall("{CALL AccountProcess (?, NULL, ?, ?, 0, ?, ?, ?)}");
            st.setLong(1, Long.valueOf(subscription.getAgreement()));
            //st.setString(2, username);
            st.setLong(2, tarifID);
            st.setString(3, expirationDateAsString);
            st.setInt(4, 1);
            st.setString(5, ctx.getCallerPrincipal().getName());
            st.registerOutParameter(6, Types.BIGINT);
            st.executeQuery();
            res = (int) st.getLong(6);

            log.debug(String.format("CLOSE SERVICE: agreement=%s, {CALL AccountProcess (%s, NULL, %d, '%s', 0, 1, '%s', @Res)}",
                    subscription.getAgreement(), subscription.getAgreement(), tarifID, expirationDateAsString, ctx.getCallerPrincipal().getName()));

            log.info(String.format("CLOSE SERVICE for agreement=%s Provisioning result: %s", subscription.getAgreement(), res));
        } catch (SQLException ex) {
            log.error(String.format("Error during CLOSE SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        } finally {
            try {
                if (st != null) {
                    st.close();
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
                log.error(String.format("Error during CLOSE SERVICE for agreement=%s", subscription.getAgreement()), ex);
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

        int res = -1;

        try {
            /* if (subscription.getType() != SubscriptionType.STATIC)
                throw new ProvisioningException("Subscription must be static");
             */
            SubscriptionResource resource = subscription.getActiveResource();
            //String username = resource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).getCapacity();
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();

            String tariff = resource.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity();

            log.debug(String.format("HERE: collectOnlineStats: user=%s, tariff=%s", username, tariff));

            radiusConnection = radiusDataSource.getConnection();

            log.debug("connected to radius");

            String expirationDateAsString = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            st = radiusConnection.prepareStatement(""
                    + "select ac.AccountID, a.username, a.acctinputoctets, a.acctoutputoctets, a.acctstarttime, a.nasipaddress, a.framedipaddress, a.callingstationid, co.Active, co.Redirect\n"
                    + "from Accounts ac \n"
                    + "left join AccountSessionControl co on co.AccountID = ac.AccountID\n"
                    + "left join (select * from radacct a where a.username = ?) a on a.username = ac.UserName\n"
                    + "where ac.UserName = ?");
            st.setString(1, username);
            st.setString(2, username);
            rs = st.executeQuery();

            if (rs.next()) {
                stats = new OnlineBroadbandStats();
                SimpleDateFormat frm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                stats.setUser(username);
                double upload = rs.getLong(4) / 1024d / 1024d;
                double download = rs.getLong(3) / 1024d / 1024d;
                stats.setDown(String.format("%.3f", upload));
                stats.setUp(String.format("%.3f", download));
                stats.setNasIpAddress(rs.getString(6));
                stats.setFramedAddress(rs.getString(7));
                stats.setCallingStationID(rs.getString(8));

                try {
                    String startTime = rs.getString(5);

                    if (startTime != null) {
                        stats.setStartTime(frm.parse(startTime));
                    }
                } catch (ParseException ex) {
                    log.error("Cannot parse startdate", ex);
                }
            }
        } catch (SQLException ex) {
            log.error(String.format("Error during CLOSE SERVICE on radius for agreement=%s", subscription.getAgreement()), ex);
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
            } catch (SQLException ex) {
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
        TechnicalStatus technicalStatus = null;
        ResultSet rs = null;
        String logPrefix = "Getting technical status for Azertelekom subscription agreement id = " + subscription.getAgreement();

        try {
            log.debug(logPrefix);
            String username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            radiusConnection = radiusDataSource.getConnection();

            st = radiusConnection.prepareStatement(""
                    + "select ac.AccountID, co.Active, co.Redirect\n"
                    + "from Accounts ac \n"
                    + "left join AccountSessionControl co on co.AccountID = ac.AccountID\n"
                    + "where ac.UserName = ?");
            st.setString(1, username);
            st.execute();

            rs = st.executeQuery();
            if (rs.next()) {
                String active = rs.getLong(2) == 1 ? "AVAILABLE" : "DISCONNECT";
                String redirect = rs.getLong(3) == 1 ? "REDIRECT" : "NOT RESTRICTED";
                technicalStatus = new TechnicalStatus();
                technicalStatus.addElement(StatusElementType.BROADBAND_ACTIVE, active);
                technicalStatus.addElement(StatusElementType.BROADBAND_REDIRECT, redirect);
            }
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
            } catch (SQLException ex) {
                log.error(logPrefix + " error while closing connections.", ex);
            }
        }
        return technicalStatus;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean openVAS(Subscription subscription, ValueAddedService vas) {
        return manageVAS(subscription, vas, SubscriptionStatus.ACTIVE);
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
        return manageVAS(subscription, vas, SubscriptionStatus.BLOCKED);
    }

    private boolean manageVAS(Subscription subscription, ValueAddedService vas, SubscriptionStatus status) {
        switch (vas.getCode().getType()) {
            case PERIODIC_STATIC:
                return manageStaticIP(subscription, vas, status);
            default:
                return false;
        }
    }

    private boolean manageVASPeriodicStatic(Subscription subscription, ValueAddedService vas, SubscriptionStatus status) {
        ResourceBucketType type = vas.getResource().getBucketList().get(0).getType();

        if (type == ResourceBucketType.INTERNET_IP_ADDRESS) {
            return manageStaticIP(subscription, vas, status);
        } else {
            return true;
        }
    }

    private boolean manageStaticIP(Subscription subscription, ValueAddedService vas, SubscriptionStatus status) {
        SystemEvent ev = null;
        SubscriptionVAS sbnVAS = subscription.getVASByServiceId(vas.getId());
        SubscriptionResource resource = null;
        SubscriptionResourceBucket ipBucket = null;
        //SubscriptionResourceBucket subnetBucket = null;
        String message = null;
        int res = -1;
        final String defaultSubNet = "255.255.255.0";

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

        try {
            if (status == SubscriptionStatus.ACTIVE) {
                log.info("manageVAS: starting ACTIVATE STATIC IP request.....");
                res = activateStaticIP(subscription, ipBucket.getCapacity(), defaultSubNet);
                log.info(String.format("manageVAS: ACTIVATE STATIC IP request finished. ACTIVATE STATIC IP returned result %d", res));

                if (res == 1) {
                    log.info("manageVAS: starting OPEN SERVICE request.....");
                    openServiceForSubscription(subscription, subscription.getExpirationDate(), STATIC_IP_SERVICE_GROUP);
                    log.info("manageVAS: OPEN SERVICE request finished");
                }
            } else if (status == SubscriptionStatus.BLOCKED) {
                res = blockStaticIP(subscription, ipBucket.getCapacity(), defaultSubNet);
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

        return res == 1;
    }

    private int activateStaticIP(Subscription subscription, String ipAddress, String subnet) throws SQLException {
        CallableStatement ps = null;
        Connection con = null;
        int res = 1;

        try {
            con = radiusDataSource.getConnection();

            log.info(String.format("activateStaticIP: request {CALL Add_IP_Address (%d, %s, %s)}", Long.valueOf(subscription.getAgreement()), ipAddress, subnet));
            ps = con.prepareCall("{CALL Add_IP_Address (?, ?, ?, ?)}");

            ps.setLong(1, Long.valueOf(subscription.getAgreement()));
            ps.setString(2, ipAddress);
            ps.setString(3, subnet);
            ps.registerOutParameter(4, Types.BIGINT);

            ps.execute();

            //int res = (int) ps.getLong(4);
            log.debug(String.format("activateStaticIP: request returned %d", res));
            return res;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }

                if (con != null) {
                    con.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resources", ex);
            }
        }
    }

    private int blockStaticIP(Subscription subscription, String ipAddress, String subnet) throws SQLException {
        CallableStatement ps = null;
        Connection con = null;

        try {
            con = radiusDataSource.getConnection();

            ps = con.prepareCall("{CALL Del_IP_Address (?, ?)}");
            ps.setLong(1, Long.valueOf(subscription.getAgreement()));
            ps.registerOutParameter(2, Types.BIGINT);

            ps.execute();

            return 1;
        } catch (Exception ex) {
            log.error(
                    String.format("blockStaticIP: cannot block static IP, subscription id=%d, agreement=%s, ipAddress=%s",
                            subscription.getId(), subscription.getAgreement(), ipAddress), ex);
            return -1;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }

                if (con != null) {
                    con.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resources", ex);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean changeEquipment(Subscription subscription, String newValue) throws ProvisioningException {
        String header = String.format("changeEquipemt: agreement=%s, newSername=%s", subscription.getAgreement(), newValue);

        log.info(String.format("%s starting", header));
        PreparedStatement pst = null;
        Connection con = null;
        int res = -1;

        try {
            if (!checkIfExists(subscription)) {
                log.info(String.format("%s calling INIT SERVICE", header));
                initService(subscription);
            }
            con = radiusDataSource.getConnection();

            pst = con.prepareStatement("update Accounts set UserName = ? where AccountID = ?");
            pst.setString(1, newValue);
            pst.setString(2, subscription.getAgreement());
            res = pst.executeUpdate();
        } catch (SQLException ex) {
            String msg = String.format("changeEquipment: failed for subscription id=%d, agreement=%s, new username=%s",
                    subscription.getId(), subscription.getAgreement(), newValue);
            log.error(msg, ex);
            throw new ProvisioningException(msg, ex);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                String msg = String.format("changeEquipment: cannot clear resource,subscription id=%d, agreement=%s, new username=%s",
                        subscription.getId(), subscription.getAgreement(), newValue);
                log.error(msg, ex);
                throw new ProvisioningException(msg, ex);
            }
        }

        if (res <= 0) {
            String msg = String.format("changeEquipment: cannot change equipment for subscription id=%d, agreement=%s, new username=%s - result %d",
                    subscription.getId(), subscription.getAgreement(), newValue, res);
            throw new ProvisioningException(msg);
        }
        log.info(String.format("%s finished", header));
        return res > 0;
    }

    private boolean checkIfExists(Subscription subscription) throws ProvisioningException {
        PreparedStatement pst = null;
        Connection con = null;
        ResultSet rs = null;

        int res = -1;
        String header = String.format("checkifExists: agreement=%s", subscription.getAgreement());

        log.info(String.format("%s starting", header));

        try {
            con = radiusDataSource.getConnection();

            pst = con.prepareStatement("select count(*) from Accounts where AccountID = ?");
            pst.setString(1, subscription.getAgreement());
            rs = pst.executeQuery();

            if (rs.next()) {
                long count = rs.getLong(1);
                log.info(String.format("%s count=%d", header, count));
                return count > 0;
            } else {
                log.info(String.format("%s does not exist", header));
                return false;
            }
        } catch (SQLException ex) {
            String msg = String.format("checkIfExists: failed for subscription id=%d, agreement=%s",
                    subscription.getId(), subscription.getAgreement());
            log.error(msg, ex);
            throw new ProvisioningException(msg, ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                String msg = String.format("changeEquipment: cannot clear resource,subscription id=%d, agreement=%s",
                        subscription.getId(), subscription.getAgreement());
                log.error(msg, ex);
                throw new ProvisioningException(msg, ex);
            }
        }

        /*if (res <= 0) {
            String msg = String.format("changeEquipment: cannot change equipment for subscription id=%d, agreement=%s, new username=%s - result %d",
                    subscription.getId(), subscription.getAgreement(), newValue, res);
            throw new ProvisioningException(msg);
        }*/
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean disconnect(Subscription subscription) throws ProvisioningException {
        log.info(String.format("disconnect: subscription id=%d, agreement=%s", subscription.getId(), subscription.getAgreement()));
        CallableStatement cs = null;
        Connection con = null;

        try {
            con = radiusDataSource.getConnection();
            cs = con.prepareCall("{CALL Disconnect_Account_Session (?,?)}");

            cs.setLong(1, Long.valueOf(subscription.getAgreement()));
            cs.setString(2, ctx.getCallerPrincipal().getName());

            cs.execute();
            return true;
        } catch (SQLException ex) {
            String msg = String.format("Cannot disconnect: subscription id=%d, agreement=%s", subscription.getId(), subscription.getAgreement());
            log.error(msg, ex);
            throw new ProvisioningException(msg, ex);
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resource", ex);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String checkRadiusState(Subscription subscription) throws ProvisioningException {
        return "OFFLINE";
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public boolean changeService(Subscription subscription, Service targetService) throws ProvisioningException {
        log.debug(
                String.format("changeService: received subscription id=%d, agreement=%s changed to service id=%d, name=%s",
                        subscription.getId(), subscription.getAgreement(), subscription.getService().getId(), subscription.getService().getName()));

        List<com.jaravir.tekila.module.service.entity.Resource> resList = targetService.getResourceList();
        subscription.getResources().clear();
        if (resList.size() > 0) {
            for (com.jaravir.tekila.module.service.entity.Resource res : resList) {
                SubscriptionResource subResource = new SubscriptionResource(res);
                /*if (subscription.getService().getServiceType().equals(ServiceType.BROADBAND)) {
                         subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH).setCapacity("test switch");
                         subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH_PORT).setCapacity("test port");
                         subResource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).setCapacity("test switch:testport:" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                     }*/
                subscription.addResource(subResource);
            }
        }

        return changeServiceOnRadius(subscription);
    }

    private boolean hasStaticIP(Subscription subscription) {
        List<SubscriptionVAS> vasList = subscription.getVasList(ValueAddedServiceType.PERIODIC_STATIC);
        if (vasList != null && !vasList.isEmpty()) {
            SubscriptionResource resource = null;
            for (SubscriptionVAS sbnVAS : vasList) {
                if ((resource = sbnVAS.getResource()) != null
                        && (resource.getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS) != null)
                        && sbnVAS.getVas().getName().toLowerCase().contains("static")) {
                    return true;
                }
            }
        }

        return false;
    }

    private int getServiceGroup(Subscription subscription) {
        return hasStaticIP(subscription) ? STATIC_IP_SERVICE_GROUP : GENERAL_SERVICE_GROUP;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExternalStatusInformation collectExternalStatusInformation(Subscription subscription) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovision(Subscription subscription) {

        log.debug("Starting reprovisoning on Azertelekom, agreement=" + subscription.getAgreement());

        DateTime expireDate = subscription.getExpirationDate();

        log.debug("expireDate: " + expireDate);

//        if (billingUpToDate.compareTo(DateTime.now()) < 0) {
//            return false;
//        }

        PreparedStatement st = null;
        CallableStatement ps = null;
        Connection con = null;
        ResultSet rs;
        String stringDate;

        try {
            stringDate = expireDate.toString();
            if (stringDate.contains("T")) {
                stringDate = stringDate.substring(0, stringDate.indexOf("T"));
            } else {
                stringDate = stringDate.substring(0, 10);
            }
            stringDate += " 23:59:59";
            log.debug("stringDate: " + stringDate);

            con = radiusDataSource.getConnection();
            log.debug("connected to radius");

            st = con.prepareStatement("select AccountTypeID from Accounts where AccountID = ?");
            st.setInt(1, Integer.valueOf(subscription.getAgreement()));
            rs = st.executeQuery();

            int typeId;
            if (rs.next()) {
                typeId = rs.getInt(1);
            } else {
                log.debug(rs);
                return false;
            }

            log.debug("typeID: " + typeId);
            ps = con.prepareCall("{CALL AccountProcess (?,NULL,?,?,0,1,?,?)}");

            ps.setInt(1, Integer.valueOf(subscription.getAgreement()));
            ps.setInt(2, typeId);
            ps.setString(3, stringDate);
            ps.setString(4, ctx.getCallerPrincipal().getName());
            ps.registerOutParameter(5, Types.INTEGER);

            ps.execute();

            log.debug("Azertelekom reprovisioning success, agreement=" + subscription.getAgreement());

            return true;

        } catch (SQLException ex) {
            String msg = String.format("Provision Failed: subscription id=%d, agreement=%s", subscription.getId(), subscription.getAgreement());
            log.error(msg, ex);
            return false;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }

                if (con != null) {
                    con.close();
                }

                if (st != null) {
                    st.close();
                }
            } catch (Exception ex) {
                log.error("Cannot free resources", ex);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean reprovisionWithEndDate(Subscription subscription, DateTime endDate) {
        return false;
    }

    /*@RolesAllowed("system")
    // @Schedule(hour = "19", minute = "59")
    public void openConnection () throws InterruptedException {
        List<Subscription> sbnList = em.createQuery("select s from Subscription s where s.agreement in ('2529','2299','2108','2078','1958','1795','1500','1824','1851','1852','1854')", Subscription.class).getResultList();
        log.info("SBn LIst: " + sbnList.size());

        for (Subscription sbn : sbnList) {
            log.info("Processing subscription id=" + sbn.getId());
            //initService(sbn);
            if (sbn.getStatus() == SubscriptionStatus.ACTIVE) {
                openService(sbn);
            } else {
                closeService(sbn);
            }

        }
    }*/

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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAttribute(Attribute attribute) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<OfflineBroadbandStats> offlineBroadbandStats(Subscription subscription, Map<Filterable, Object> filters) {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateAccount(Subscription subscription) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DateTime getActivationDate(Subscription subscription) {
        return null;
    }

    @Override
    public boolean updateByService(Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(Service service) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean updateServiceProfile(ServiceProfile serviceProfile, int oper) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean createServiceProfile(ServiceProfile serviceProfile) {
        return false;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
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
    public List<String> getAuthRejectionReasons(Subscription subscription) {
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
