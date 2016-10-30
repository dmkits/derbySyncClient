/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** SyncDB
 * Операции в синхронизируемой базе данных.
 * Создание, обновление объектов синхронизации в базе данных клиента.
 * Создание доп. настроек в базе данных.
 * Скрипты создания читаются из ресурсов в пакете sqlscripts.
 * @author dmk.dp.ua 2014-04-15 */
public class SyncDB {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.SyncDB");
    
    /* Создание объектов синхронизации в базе данных. Создаются таблицы синхронизации и триггеры синхронизации. */
    public static void createSyncObjects(Session poDBSession) throws Exception {
        
        logger.log(Level.INFO, "----------Creating sync objects in database----------");
        int[] isRes = {0,0};
        String sQuery_upddb= TextFromResource.load("/sqlscripts/update_db_1_5.sql"); //чтение sql-скрипта из ресурса - обновление настроек бд
        execSript(poDBSession, sQuery_upddb, isRes); //выполнение скрипта
        String sQuery_cr= TextFromResource.load("/sqlscripts/synctables_cr.sql") ; //чтение sql-скрипта из ресурса - создание таблиц синхр.
        execSript(poDBSession, sQuery_cr, isRes); //выполнение скрипта
        String sQuery_cr2= TextFromResource.load("/sqlscripts/synctriggers_cr.sql"); //чтение sql-скрипта из ресурса - создание триггеров синхр.
        execSript(poDBSession, sQuery_cr2, isRes); //выполнение скрипта
        logger.log(Level.WARNING, "UPDATE RESULT: statements to execute {0}, executed {1}", new Object[]{isRes[0], isRes[1]});
        logger.log(Level.INFO, "----------Sync objects created in database.----------");
    }
    
    /* выполнение скрипта. в скрипте отдельные запросы разделены знаками ; 
     * возвращаемые значения: кол-во запросов в скрипте и кол-во удачно выполненных запросов */
    private static void execSript(Session poDBSession, String psScriptQuery, int[] pisRes) throws Exception {
        //разбор строки с sql-скриптом на отдельные запросы, разделенные ";" и поочередное выпонение полученных запросов
        StringTokenizer sST = new StringTokenizer(psScriptQuery,";");
        while (sST.hasMoreTokens()) {
            String sQuery= sST.nextToken();
            if (sQuery==null || sQuery.equals("") || sQuery.equals("\n")) { break; }
            pisRes[0]++;
            //подготовка обработчика запросов к БД и выполнение запроса-создания объектов синхронизации в базе данных
            try {
                Statement voSt=poDBSession.getConnection().createStatement();
                logger.log(Level.INFO, "Executing statement {0}: {1}", new Object[]{pisRes[0],sQuery});
                voSt.execute(sQuery);
                pisRes[1]++;
                voSt.close();
                logger.log(Level.INFO, "Statement {0} executed.", pisRes[0]);
            } catch (Exception e) {
                logger.log(Level.WARNING, "FAILED to execute database statement {0}! {1}", new Object[]{pisRes[0],e.getMessage()});
            }
        }
    }
    
    /* обновление настроек и объектов базы данных предыдущих версий до v.1.5. */
    public static void updateDB_1_5(Session poDBSession) throws Exception {
        
        logger.log(Level.INFO, "----------Updating database to DerbySyncClient v.1.5.----------");
        int[] isRes = {0,0};
        String sQuery_upddb= TextFromResource.load("/sqlscripts/update_db_1_5.sql"); //обновление настроек бд
        execSript(poDBSession, sQuery_upddb, isRes); //выполнение скрипта
        String sQuery_deltr= TextFromResource.load("/sqlscripts/synctriggers_dr.sql"); //удаление триггеров
        execSript(poDBSession, sQuery_deltr, isRes); //выполнение скрипта
        String sQuery_crtrg= TextFromResource.load("/sqlscripts/synctriggers_cr.sql"); //создание новых триггеров
        execSript(poDBSession, sQuery_crtrg, isRes); //выполнение скрипта
        logger.log(Level.WARNING, "UPDATE RESULT: statements to execute {0}, executed {1}", new Object[]{isRes[0], isRes[1]});
        logger.log(Level.INFO, "----------Database updated to DerbySyncClient v.1.5.----------");
    }
    
    /* удаление примененных данных из БД. Удаляются данные и записи журнала исходящих данных синхронизации. */
    public static void delApplSyncData(Session poDBSession, int piTermStorageDay) throws Exception {
        //чтение sql-запроса из ресурса
        logger.log(Level.INFO, "----------Deleting applied data----------");
        
        /* получение списка ID последних при закрытии чека операций отправленных и примененных записей синхронизации */
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdataout_sel_applied.sql") ;
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_sel);
        Calendar cgCalendar = new GregorianCalendar();//cur datetime
        cgCalendar.add(Calendar.DAY_OF_YEAR, -piTermStorageDay); //cur datetime - piTermStorageDay days
        oPSt.setDate(1, (new java.sql.Date(cgCalendar.getTime().getTime())) ); //cur datetime - piTermStorageDay days
        ResultSet oRS = null;
        try {
            oRS= oPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to get applied sync information data from database! "+e.getMessage());
        }
        while (true) { //цикл по ID
            int iID_End = 0;
            try {
                if (oRS.next()) {
                    iID_End = oRS.getInt("ID");
                    logger.log(Level.INFO, "Got ID group applied sync information from database. ID: {0}", iID_End);
                } else {
                    logger.log(Level.INFO, "No applied sync infomation for delete in database.");
                    break; //выход
                }
            } catch (Exception e) {
                throw new Exception("FAILED to get ID group applied sync information from database! "+e.getMessage());
            } 
            // удаление группы записей с iID_End
            delGroupAppData(poDBSession, iID_End);
        }
        oPSt.close();
    }
    
    /* удаление группы записей отправленных и примененных данных синхронизации начиная с последнего ID группы */
    private static void delGroupAppData(Session poDBSession, int piID_End) throws Exception {

        logger.log(Level.INFO, "------Deleting group applied data------");
        
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdataout_sel_deleting.sql") ;
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_sel);
        oPSt.setInt(1,piID_End); //End ID
        ResultSet oRS = null;
        try {
            oRS= oPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to get group sync information for delete from database! "+e.getMessage());
        }
        while (true) { //цикл по ID записей группы
            int iID = 0;
            String sTable, sKey1Field, sKey1Val, sKey2Field, sKey2Val;
            try {
                if (oRS.next()) {
                    iID = oRS.getInt("ID");
                    sTable = oRS.getString("TABLENAME");
                    sKey1Field = oRS.getString("TABLEKEY1IDNAME");
                    sKey1Val = oRS.getString("TABLEKEY1IDVAL");
                    sKey2Field = oRS.getString("TABLEKEY2IDNAME");
                    sKey2Val = oRS.getString("TABLEKEY2IDVAL");
                    logger.log(Level.INFO, "Got ID applied sync information for delete from database. ID: {0}", iID);
                } else {
                    break; //выход
                }
            } catch (Exception e) {
                throw new Exception("FAILED to get ID applied sync information for delete from database! "+e.getMessage());
            } 
            // удаление данных
            if (sTable.equals("APP.RECEIPTS")) { 
                delData(poDBSession, "APP.TAXLINES", "RECEIPT", sKey1Val, null, null); }
            delData(poDBSession, sTable, sKey1Field, sKey1Val, sKey2Field, sKey2Val);
            // удаление исходящих данных синхронизации с iID
            delSyncOutData(poDBSession, iID);
        }
        oPSt.close();
    }
    
    /* удаление данных из таблицы psTable, по ключу psKey1Field со значением psKey1Val и доп.клучу psKey2Field со значением psKey2Val */
    private static void delData(Session poDBSession, String psTable, 
            String psKey1Field, String psKey1Val, String psKey2Field, String psKey2Val) throws Exception {
        
        logger.log(Level.INFO, "---Deleting applied data ({0})---",psTable);
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_del= "delete from "+psTable+" where "+psKey1Field+"=?";
        if (psKey2Field!=null) { sQuery_del=sQuery_del+" and "+psKey2Field+"=?"; }
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_del);
        oPSt.setString(1,psKey1Val); 
        if (psKey2Field!=null) { oPSt.setString(2,psKey2Val); }
        //ResultSet oRS = null;
        try {
            oPSt.executeUpdate();
            oPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to delete data for from database! "+e.getMessage());
        }
    }

    /* удаление данных из таблицы исходящих данных синхронизации по ключу piID */
    private static void delSyncOutData(Session poDBSession, int piID) throws Exception {
        
        logger.log(Level.INFO, "---Deleting sync data (ID={0})---",piID);
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_del= "delete from APP.SYNCDATAOUT where ID=?";
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_del);
        oPSt.setInt(1,piID); 
        try {
            oPSt.executeUpdate();
            oPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to delete sync data for from database! "+e.getMessage());
        }
    }
}
