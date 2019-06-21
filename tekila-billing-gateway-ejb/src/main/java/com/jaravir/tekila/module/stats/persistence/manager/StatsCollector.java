package com.jaravir.tekila.module.stats.persistence.manager;

import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sajabrayilov on 7/8/2015.
 */

@DeclareRoles({"system"})
@RunAs("system")
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class StatsCollector {
    @EJB
    private OnlineStatsPersistenceFacade onlineFacade;
    @EJB private ServiceProviderPersistenceFacade serviceProviderFacade;
    @EJB private OfflineStatsPersistenceFacade offlineFacade;

//    @Resource(name = "jdbc/Azertelekom.Radius")
    @Resource(name = "jdbc/Azertelekom.Radius.Stats")
    private DataSource azertelekomRadiusDataSource;

    @Resource(name = "jdbc/Citynet.Radius")
    private DataSource citynetRadiusDataSource;

    @Resource(name = "jdbc/Uninet.Radius")
    private DataSource uninetRadiusDataSource;

    @Resource(name = "jdbc/Dataplus.Radius")
    private DataSource dataPlusRadiusDataSource;

//    private final static Logger log = Logger.getLogger(StatsCollector.class);
    private final static Logger log = LoggerFactory.getLogger(StatsCollector.class);
    private final static long OFFLINE_STATS_LIMIT = 50000;

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/15")
    public void collect() {
        log.debug("Starting collecting online stats");

        log.debug("Starting collecting Azertelekom online stats");
        List<StatsRecord> azertelekomStatsList = collectAzertelekomStatsFromRadius();

        log.debug("Starting collecting Citynet online stats");
        List<StatsRecord> citynetStatsList = collectCitynetStatsFromRadius();

        log.info("Persiststing stats....");
        try {
            persist(azertelekomStatsList, citynetStatsList);
        }
        catch (Exception ex) {
            log.error("Cannot collect transaction", ex);
        }finally {
           citynetStatsList.clear();
        }
        log.debug("Finished collecting online stats");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private List<StatsRecord> collectAzertelekomStatsFromRadius() {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        StatsRecord stats = null;

        int res = -1;

        List<StatsRecord> statsList = new ArrayList<>();
        try {
            radiusConnection = azertelekomRadiusDataSource.getConnection();
            String expirationDateAsString = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            st = radiusConnection.prepareStatement("select ac.AccountID, a.username, a.acctinputoctets, a.acctoutputoctets, a.acctstarttime, a.nasipaddress, a.framedipaddress, a.callingstationid\n" +
                            " from radacct a\n" +
                            " join Accounts ac on a.username = ac.UserName");

            rs = st.executeQuery();

            while (rs.next()) {
                stats = new StatsRecord();
                SimpleDateFormat frm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                stats.setAccountID(String.valueOf(rs.getLong(1)));
                stats.setUser(rs.getString(2));
                double upload = rs.getLong(4) / 1024d / 1024d;
                double download = rs.getLong(3) / 1024d / 1024d;
                stats.setDown(String.format("%.3f", upload));
                stats.setUp(String.format("%.3f", download));
                stats.setNasIpAddress(rs.getString(6));
                stats.setFramedAddress(rs.getString(7));
                stats.setCallingStationID(rs.getString(8));

                try {
                    String startTime = rs.getString(5);
                   // log.debug(String.format("username=%s, startTime=%s", stats.getUser(), startTime));
                    if (startTime != null)
                        stats.setStartDate(frm.parse(startTime));
                }
                catch (ParseException ex) {
                    log.error("Cannot parse startdate" , ex);
                }

                statsList.add(stats);
                //log.debug("Stats username: " + stats.getUser());
            }
        }
        catch (SQLException ex) {
            log.error("Cannot collect azertelekom online stats", ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        }
        finally {
            try {
                if (st != null)
                    st.close();
                if (rs != null)
                    rs.close();
                if (radiusConnection != null)
                    radiusConnection.close();
            }
            catch (SQLException ex) {
                log.error("Cannot cleanup azertelekom radius connection resources", ex);
            }
        }
        log.info("Azertelekom stats record size:" + statsList.size());
        return statsList;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private List<StatsRecord> collectCitynetStatsFromRadius() {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        List<StatsRecord> statsRecordList = new ArrayList<>();
        try {
            radiusConnection = citynetRadiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("select AC.ACC_ID,RA.USERNAME,AC.STATUS, RA.ACCTINPUTOCTETS,RA.ACCTOUTPUTOCTETS,RA.ACCTSTARTTIME,RA.NASIPADDRESS,RA.FRAMEDIPADDRESS,RA.CALLINGSTATIONID,RA.SERVICE,RA.RADACCTID " +
                    "from ncn_accounts AC join ncn_radacct RA on AC.USERNAME = SUBSTR(RA.USERNAME,21)");
            rs = st.executeQuery();

            while (rs.next()) {
                StatsRecord statsRecord = new StatsRecord();
                statsRecord.setAccountID(rs.getString(1));
                statsRecord.setUser(rs.getString(2));

                double download = 1.0 * rs.getLong(4) / 1024d / 1024d;
                statsRecord.setDown(String.format("%.3f", download));

                double upload = 1.0 * rs.getLong(5) / 1024d / 1024d;
                statsRecord.setUp(String.format("%.3f", upload));

                statsRecord.setStartDate(rs.getDate(6));
                statsRecord.setNasIpAddress(rs.getString(7));
                statsRecord.setFramedAddress(rs.getString(8));
                statsRecord.setCallingStationID(rs.getString(9));
                statsRecordList.add(statsRecord);
            }
        } catch (SQLException e) {
            log.error("Cannot collect citynet online stats", e);
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
            } catch (SQLException e) {
                log.error("Cannot cleanup citynet radius connection resources", e);
            }
        }
        log.info("Citynet online stats records size: " + statsRecordList.size());
        return statsRecordList;
    }

    private void persist (List<StatsRecord> azertelekomStatsList, List<StatsRecord> citynetStatsList) throws Exception {
        int count = onlineFacade.clear();
        log.debug(String.format("%d entries deleted", count));

        long id = 1;
        ServiceProvider provider = serviceProviderFacade.find(454100L);
        log.info("Service provider: " + provider);
        for (StatsRecord statsRecord : azertelekomStatsList) {
            onlineFacade.create(statsRecord, provider, id);
            id++;
        }

        provider = serviceProviderFacade.find(454105L);
        log.info("Service provider: " + provider);
        for (StatsRecord statsRecord : citynetStatsList) {
            onlineFacade.create(statsRecord, provider, id);
            id++;
        }
    }

    @RolesAllowed("system")
//    @Schedule(hour = "*")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "20")
    public void collectOfflineStats() {
        log.debug("Starting collecting ofline stats");
        long start = System.currentTimeMillis();
        //long lastId = offlineFacade.getLastId();
        long lastAzertelekomRadacctId = offlineFacade.getLastIdByProvider(Providers.AZERTELECOM.getId());
        log.debug(String.format("offlineStats: Last Azertelekom Radacct id: %d", lastAzertelekomRadacctId));
        List<StatsRecord> azertelekomStatsList = collectOfflineStatsFromRadius(lastAzertelekomRadacctId);

        Date lastCitynetDate = offlineFacade.getLastDateByProvider(Providers.CITYNET.getId());
        log.debug(String.format("offlineStats: Last Citynet Session End Date: %s", lastCitynetDate.toString()));
        List<StatsRecord> citynetStatsList = collectOfflineStatsFromCitynetRadius(lastCitynetDate);

        Date lastUninetDate = offlineFacade.getLastDateByProvider(Providers.UNINET.getId());
        log.debug(String.format("offlineStats: Last Uninet Session End Date: %s", lastUninetDate.toString()));
        List<StatsRecord> uninetStatsList = collectOfflineStatsFromUninetRadius(lastUninetDate);


        Date lastDataPlusDate = offlineFacade.getLastDateByProvider(Providers.DATAPLUS.getId());
        log.debug(String.format("offlineStats: Last dataPlus Session End Date: %s", lastDataPlusDate.toString()));
        List<StatsRecord> dataPlusStatsList = collectOfflineStatsFromDataPlusRadius(lastDataPlusDate);


        log.info("Persiststing offline stats....");
        try {
            persistOfflineStats(azertelekomStatsList, citynetStatsList, uninetStatsList, dataPlusStatsList);
        }
        catch (Exception ex) {
            log.error("offlineStats: Cannot collect transaction", ex);
        }finally {
            azertelekomStatsList.clear();
            citynetStatsList.clear();
            uninetStatsList.clear();
            dataPlusStatsList.clear();

        }
        log.info("collectOfflineStats  elapsed time : {}",(System.currentTimeMillis()-start)/1000);
        log.debug("Finished collecting offline stats");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private List<StatsRecord> collectOfflineStatsFromRadius(long lastId) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        StatsRecord stats = null;

        int res = -1;

        List<StatsRecord> statsList = new ArrayList<>();
        String startTime = null, stopTime = null;
        try {
            radiusConnection = azertelekomRadiusDataSource.getConnection();
            String expirationDateAsString = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            //st = radiusConnection.prepareCall("{CALL AccountProcess (5, 'Ethernet0/0/10:test-fbe9-b9f0', 21, '2014-12-31 00:00:00', 0, 1, 'Admin', @Result);}");
            st = radiusConnection.prepareStatement("select a.radacctid, ac.AccountID, a.username, a.acctinputoctets, " +
                    "                    a.acctoutputoctets, a.acctstarttime, a.acctstoptime, a.nasipaddress, a.framedipaddress,\n" +
                    "                    a.callingstationid, a.acctterminatecause, a.acctsessiontime, a.acctsessionid  " +
                    "from Accounts ac " +
                    "join (select * from radacct0 a  where a.radacctid > ? limit ?) " +
                    "a on a.username = ac.UserName " +
                    "order by a.radacctid");
            st.setLong(1, lastId);
            st.setLong(2, OFFLINE_STATS_LIMIT);
            st.setQueryTimeout(20);
            rs = st.executeQuery();

            while (rs.next()) {
                stats = new StatsRecord();
                SimpleDateFormat frm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                stats.setId(rs.getLong(1));
                stats.setUser(rs.getString(3));
                double upload = rs.getLong(5) / 1024d / 1024d;
                double download = rs.getLong(4) / 1024d / 1024d;
                stats.setDown(String.format("%.3f", upload));
                stats.setUp(String.format("%.3f", download));
                stats.setNasIpAddress(rs.getString(8));
                stats.setFramedAddress(rs.getString(9));
                stats.setCallingStationID(rs.getString(10));
                stats.setAccountID(String.valueOf(rs.getLong(2)));
                stats.setTerminationCause(rs.getString(11));
                stats.setSessionDuration(String.valueOf(rs.getLong(12)));
                stats.setAccountSessionID(rs.getString(13));

                try {
                    startTime = rs.getString(6);
                    // log.debug(String.format("username=%s, startTime=%s", stats.getUser(), startTime));
                    if (startTime != null)
                        stats.setStartDate(frm.parse(startTime));

                    stopTime = rs.getString(7);
                    // log.debug(String.format("username=%s, startTime=%s", stats.getUser(), startTime));
                    if (stopTime != null)
                        stats.setStopDate(frm.parse(stopTime));
                }
                catch (ParseException ex) {
                    log.error("Azertelekom offlineStats: Cannot parse startdate" , ex);
                }

                statsList.add(stats);
                //log.debug("Stats username: " + stats.getUser());
            }
        }
        catch (SQLException ex) {
            log.error("Cannot collect offline stats for Azertelekom", ex);
            //throw new ProvisioningException("Error openning service on radius" , ex.getCause());
        }
        finally {
            try {
                if (st != null)
                    st.close();
                if (rs != null)
                    rs.close();
                if (radiusConnection != null)
                    radiusConnection.close();
            }
            catch (SQLException ex) {
                log.error("offlineStats: Cannot close resource for Azertelekom", ex);
            }
        }
        log.info("offlineStats: Azertelekom stats record size:" + statsList.size());
        return statsList;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<StatsRecord> collectOfflineStatsFromCitynetRadius(Date lastDate) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<StatsRecord> statsRecordList = new ArrayList<>();
        try {
            radiusConnection = citynetRadiusDataSource.getConnection();
            st = radiusConnection.prepareStatement("select RA.RADACCTID, RA.USERNAME, RA.ACCTINPUTOCTETS,RA.ACCTOUTPUTOCTETS," +
                    "RA.NASIPADDRESS, RA.FRAMEDIPADDRESS, RA.CALLINGSTATIONID, NA.ACC_ID, RA.ACCTTERMINATECAUSE," +
                    "RA.ACCTSESSIONTIME, RA.ACCTSESSIONID, RA.ACCTSTARTTIME, RA.ACCTSTOPTIME " +
                    "from ncn_radacct_history RA left join ncn_accounts NA on " +
                    "NA.USERNAME = SUBSTR(RA.USERNAME, 21) where RA.ACCTSTOPTIME > ?");
            java.sql.Timestamp endDate = new java.sql.Timestamp(lastDate.getTime());
            log.info("end date tmstmp = " + endDate.toString());
            st.setTimestamp(1, endDate);
            rs = st.executeQuery();
            while (rs.next()) {
                StatsRecord record = new StatsRecord();
                record.setId(rs.getLong(1));
                record.setUser(rs.getString(2));

                double download = 1.0 * rs.getLong(3) / 1024d / 1024d;
                record.setDown(String.format("%.3f", download));

                double upload = 1.0 * rs.getLong(4) / 1024d / 1024d;
                record.setUp(String.format("%.3f", upload));

                record.setNasIpAddress(rs.getString(5));
                record.setFramedAddress(rs.getString(6));
                record.setCallingStationID(rs.getString(7));
                record.setAccountID(rs.getString(8));
                record.setTerminationCause(rs.getString(9));
                record.setSessionDuration(String.valueOf(rs.getLong(10)));
                record.setAccountSessionID(rs.getString(11));
                record.setStartDate(rs.getDate(12));
                record.setStopDate(rs.getDate(13));
                statsRecordList.add(record);
                record=null;
            }
        } catch (Exception e) {
            log.error("Could not fetch Citynet Offline Stats", e);
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
            } catch (SQLException e) {
                log.error("Could not cleanup sql resources during fetching of Citynet offline stats", e);
            }
        }
        log.info("offlineStats: Citynet stats record size:" + statsRecordList.size());
        return statsRecordList;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<StatsRecord> collectOfflineStatsFromUninetRadius(Date lastDate) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<StatsRecord> statsRecordList = new ArrayList<>();
        try {
            radiusConnection = uninetRadiusDataSource.getConnection();
            st = radiusConnection.prepareStatement("select id, username, input_octets, output_octets," +
                    "nas, ip, dslam, session_time, start_time, stop_time " +
                    "from sessions_history where stop_time > ?");

            java.sql.Timestamp endDate = new java.sql.Timestamp(lastDate.getTime());
            log.info("end date tmstmp = " + endDate.toString());
            st.setTimestamp(1, endDate);
            rs = st.executeQuery();
            while (rs.next()) {
                StatsRecord record = new StatsRecord();
                record.setId(rs.getLong(1));
                record.setUser(rs.getString(2));
                record.setAccountID(rs.getString(2));

                double download = 1.0 * rs.getLong(3) / 1024d / 1024d;
                record.setDown(String.format("%.3f", download));

                double upload = 1.0 * rs.getLong(4) / 1024d / 1024d;
                record.setUp(String.format("%.3f", upload));

                record.setNasIpAddress(rs.getString(5));
                record.setFramedAddress(rs.getString(6));

                String dslamIpAddress = rs.getString(7);
                if (dslamIpAddress.contains("//") && dslamIpAddress.contains("atm")) {
                    dslamIpAddress = dslamIpAddress.substring(dslamIpAddress.indexOf("//") + "//".length(), dslamIpAddress.indexOf("atm") - 1);
                }
                record.setDslamAddress(dslamIpAddress);

                record.setSessionDuration(String.valueOf(rs.getLong(8)));
                record.setStartDate(rs.getDate(9));
                record.setStopDate(rs.getDate(10));

                statsRecordList.add(record);
            }
        } catch (Exception e) {
            log.error("Could not fetch Uninet Offline Stats", e);
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
            } catch (SQLException e) {
                log.error("Could not cleanup sql resources during fetching of Uninet offline stats", e);
            }
        }
        log.info("offlineStats: Uninet stats record size:" + statsRecordList.size());
        return statsRecordList;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    List<StatsRecord> collectOfflineStatsFromDataPlusRadius(Date lastDate) {
        Connection radiusConnection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<StatsRecord> statsRecordList = new ArrayList<>();
        try {
            radiusConnection = dataPlusRadiusDataSource.getConnection();

            st = radiusConnection.prepareStatement("select id, username, int_octets, out_octets," +
                    "nas, ip, dslam, session_time, start_time, stop_time " +
                    "from sessions_history where stop_time > ?");

            java.sql.Timestamp endDate = new java.sql.Timestamp(lastDate.getTime());
            log.info("end date tmstmp = " + endDate.toString());
            st.setTimestamp(1, endDate);
            rs = st.executeQuery();
            log.debug("size rs "+rs.getFetchSize());
            while (rs.next()) {
                StatsRecord record = new StatsRecord();
                record.setId(rs.getLong(1));
                record.setUser(rs.getString(2));
                record.setAccountID(rs.getString(2));

                double download = 1.0 * rs.getLong(3) / 1024d / 1024d;
                record.setDown(String.format("%.3f", download));

                double upload = 1.0 * rs.getLong(4) / 1024d / 1024d;
                record.setUp(String.format("%.3f", upload));

                record.setNasIpAddress(rs.getString(5));
                record.setFramedAddress(rs.getString(6));

                String dslamIpAddress = rs.getString(7);
                if (dslamIpAddress.contains("//") && dslamIpAddress.contains("atm")) {
                    dslamIpAddress = dslamIpAddress.substring(dslamIpAddress.indexOf("//") + "//".length(), dslamIpAddress.indexOf("atm") - 1);
                }
                record.setDslamAddress(dslamIpAddress);

                record.setSessionDuration(String.valueOf(rs.getLong(8)));
                record.setStartDate(rs.getDate(9));
                record.setStopDate(rs.getDate(10));

                statsRecordList.add(record);
            }
        } catch (Exception e) {
            log.error("Could not fetch DataPlus Offline Stats", e);
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
            } catch (SQLException e) {
                log.error("Could not cleanup sql resources during fetching of DataPlus offline stats", e);
            }
        }
        log.info("offlineStats: dataplus stats record size:" + statsRecordList.size());
        return statsRecordList;
    }



    private void persistOfflineStats (
            List<StatsRecord> azertelekomStatsList,
            List<StatsRecord> citynetStatsList,
            List<StatsRecord> uninetStatsList,
            List<StatsRecord> dataPlusStatsList) throws Exception {
        ServiceProvider azertelekomProvider = serviceProviderFacade.find(Providers.AZERTELECOM.getId());
        log.info("Offline stats: persisting records for service provider: " + azertelekomProvider);
        for (StatsRecord statsRecord : azertelekomStatsList) {
            offlineFacade.create(statsRecord, azertelekomProvider);
        }

        ServiceProvider citynetProvider = serviceProviderFacade.find(Providers.CITYNET.getId());
        log.info("Offline stats: persisting records for service provider: " + citynetProvider);
        for (StatsRecord statsRecord : citynetStatsList) {
            offlineFacade.create(statsRecord, citynetProvider);
        }

        ServiceProvider uninetProvider = serviceProviderFacade.find(Providers.UNINET.getId());
        log.info("Offline stats: persisting records for service provider: " + uninetProvider);
        for (StatsRecord statsRecord : uninetStatsList) {
            offlineFacade.create(statsRecord, uninetProvider);
        }

        ServiceProvider dataPlusProvider = serviceProviderFacade.find(Providers.DATAPLUS.getId());
        log.info("Offline stats: persisting records for service provider: " + dataPlusProvider);
        for (StatsRecord statsRecord : dataPlusStatsList) {
            offlineFacade.create(statsRecord, dataPlusProvider);
        }



    }
}
