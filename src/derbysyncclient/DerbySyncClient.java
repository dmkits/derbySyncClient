/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-14
 */
package derbysyncclient;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static derbysyncclient.SyncDB.*;

/** DerbySyncClient
 * Клиент Web-сервиса синхронизации данных с базой MSSQL.
 * Web-сервис построен на базе спецификаций JAXM и SAAJ.
 * Клиент отправляет сервису SOAP-сообщения-запросы и принимает от сервиса SOAP-сообщения-ответы.
 * @author dmk.dp.ua 2014-03-14
 */
public class DerbySyncClient {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.DerbySyncClient");

    private static String HOST_NAME;
    private static Registry m_registry = null;
    private static AppMessage m_message = null;
    
    private static ClientLocalConfig cvoClientLocalConfig = null;
    
    private static String cvsDBUrl = null;
    private static String cvsDBUser = null;
    private static String cvsDBPassword = null;
    private static Session cvoDBSession = null;

    private static String cvsServiceURL = null;
    private static HashMap<String,String> cvsPOSClientInformation = null;

    private static int Msg_Package_Count =0; // количество отправок сообщений за 1 сеанс связи
    private static int Msg_Fail_Count =0; // кол-во попыток отправки пакетов и обработки ответов в случае неудачи
    private static int Term_Storage_Day =0; // срок хранения данных свыше которого данные удаляются

    private DerbySyncClient() {
    }
            
    /** Запуск приложения.
     * @param args the command line arguments
     * Запуск приложения.
     * Вывод стартовых и завершающих логов. 
     * Проверка регистрации приложения. Регистрация запускаемого приложения и удаление регистрации по завершении работы. */
    public static void main(final String[] args) {
        logger.log(Level.INFO, "---------------------------------------------------------------");
        logger.log(Level.INFO, "STARTING DerbySyncClient..................................................................................................");
        if (isAppRegister()) {// проверка на наличие уже запущенного экземпляра приложения
            logger.log(Level.SEVERE, "FAILED launch a second instance of " + ClientInstanceInfo.CLIENT_NAME + "!");
            return;
        }
        instanceRegistering();// регистрация запущенного приложения
        try {
            if (init(args)) return;//if did update
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FAILED init " + ClientInstanceInfo.CLIENT_NAME + "! Reason:"+e.getLocalizedMessage());
            instanceUnregistering();
            return;
        }
        try {
            run(args);// формирование, отправка сообщений и прием и обработка ответов.
        } catch (Exception e) {
            logger.log(Level.SEVERE, "----------!!!!!!!!!!DerbySyncClient aborted!!!!!!!!!!----------");
            logger.log(Level.SEVERE, "CAUSE: {0}", e.getMessage());
        }
        instanceUnregistering();// удаление регистрации приложения
        logger.log(Level.INFO, "FINISHED DerbySyncClient...................................................................................................");
    }
    
    private static boolean init(String[] args) throws Exception {
        HOST_NAME = java.net.InetAddress.getLocalHost().getHostName();
        // чтение локальных параметров из файла настроек .properties
        loadLocalConfig();
        //----------регистрация драйвера базы данных, из которой извлекаются данные для отправки----------
        SyncDB.regDBDriver(cvoClientLocalConfig.getProperty("db.driverlib"), cvoClientLocalConfig.getProperty("db.driver"));
        //----------подключение к базе данных, из которой извлекаются данные для отправки----------
        cvoDBSession= SyncDB.connectToDB(cvsDBUrl, cvsDBUser,cvsDBPassword);
        cvsPOSClientInformation= SyncDB.getPOSClientInformation(cvoDBSession);
        if (args.length==0) return false;
        String sArg1= args[0];
        //---создание объектов синхронизации в базе клиента и выход если указан аргумент -CREATE_DB_SYNC_OBJECTS---
        if (sArg1.equalsIgnoreCase("-CREATE_DB_SYNC_OBJECTS")) {
            SyncDB.createSyncObjects(cvoDBSession);
            return true;
        }
        //---обновление бд и выход если указан аргумент -UPDATE_DB_1_5---
        if (sArg1.equalsIgnoreCase("-UPDATE_DB_1_5")) {
            SyncDB.updateDB_1_5(cvoDBSession);
            return true;
        }
        //---обновление синх. триггеров для бд в.2.33 и выход если указан аргумент -UPDATE_DB_2_33_TO_APP_1_5_1---
        if (sArg1.equalsIgnoreCase("-UPDATE_DB_2_33_TO_APP_1_5_1")) {
            SyncDB.updateDB_2_33_TO_APP_1_5_1(cvoDBSession);
            return true;
        }
        //---удаление примененных данных синхронизации и выход если указан аргумент -DEL_APPLIED_DATA---
        if (sArg1.equalsIgnoreCase("-DEL_APPLIED_DATA")) {
            SyncDB.delApplSyncData(cvoDBSession, Term_Storage_Day);
            return true;
        }
        return false;
    }

    /** Формирование, отправка запросов к службе синхронизации на сервере, получение и обработка ответов.
     * @param args the command line arguments
     * Из файла локальных настроек config.properties читаются локальные параметры приложения,
     *   производится соединение с БД derby.
     * Сначала на сервер отправляются запросы на входящие данные синхронизации.
     * Полученные данные сохраняются в таблице входящих данных синхронизации и применяются
     *   (выполняются запросы на вставку/обновление данных в соответствующих таблицах данных).
     * Далее извлекаются данные для сохранения на сервере,
     *   если данные для сохранения есть, отправляется запрос с исходящими данными синхронизации для сохранения сервере,
     *   если данных на сохранение нет, извлекаются данные на применение и отправляется запрос на применение данных на сервере,
     *   затем снова извлекаются данные на сохранение и т.д.
     * При формировании запроса на сохранение извлекаются данные из таблицы исходящих данных синхронизации,
     *   а также извлекаются сами данные синхронизации из соответствующих таблиц с данными.
     * По результату сохранения и применения данных на сервере сервер отправляет ответ с результатами сохранения/применения
     *   исходящих данных на сервере, на основании которых обновляется статус, сообщения и даты обновления и применения
     *   данных в таблице исходящих данных синхронизации.
     * Количество отправок сообщений за один сеанс работы приложения определяется локальным параметром MsgPackageCount. */
    private static void run(String[] args) throws Exception {
        RequestToSyncService requestToSyncService=
                new RequestToSyncService(cvsServiceURL, cvsPOSClientInformation.get(POS_ClientSyncName), HOST_NAME);
        // ---поочередная отправка сообщений на входящие данные синхронизации с сервера, сохранение и применение данных с сервера,
        // отправка ответов с результатами сохранения и применения входящих данных---
        //в количестве, установленном параметром "MsgPackageCount"
        int iErrCount = 0; //счетчик неудачных отправок
        for (int i=1;i<= Msg_Package_Count;i++) {
            logger.log(Level.INFO, "REQUEST TO GET SYNC INC DATA...........................................................................................");
            try {
                if (!requestToSyncService.getSyncIncData()) { //запрос на данные с сервера
                    logger.log(Level.INFO, "No sync inc data from server!");
                    break; //если на сервере нет данных для клиента
                }
                iErrCount=0; //если обработка ответа закончилась удачно- сбос счетчика неудачных попыток
            } catch (Exception e) {
                logger.log(Level.SEVERE, "FAILED get sync inc data from server! Reason: {0}", e.getMessage());
                iErrCount++; //счетчик неудачных попыток обработки ответа с сервера
            }
            if (iErrCount>=Msg_Fail_Count) break; //если количество неудачных попыток обработки ответа с сервера >= Msg_Fail_Count обработка прерывается
        }
        // ----------поочередная отправка сообщений с данными клиента для сохранения на сервере и прием ответов по результату сохранения данных на сервере----------
        //в количестве, установленном параметром "MsgPackageCount"
        iErrCount = 0; //счетчик неудачных отправок
        for (int i=1;i<= Msg_Package_Count;i++) {
            logger.log(Level.INFO, "REQUEST TO STORE SYNC OUT DATA.........................................................................................");
            try {
                HashMap<String,String> outputSyncData= null;
                if ((outputSyncData=SyncDB.getSyncDataOutItem(cvoDBSession))==null) break; //если нет данных для отправки на сервер
                HashMap<String,String> outputSyncDataValues=
                        SyncDB.getOutSyncDataValues(cvoDBSession, outputSyncData);
                HashMap<String,Object> serverOutSyncDataStoreResult=
                        requestToSyncService.storeSyncOutData(outputSyncData, outputSyncDataValues);
                if (SyncDB.updSyncDataOutStateStoreOnServer(cvoDBSession, outputSyncData.get("ID"), serverOutSyncDataStoreResult)) {
                    iErrCount = 0; //если отправка запроса и обработка ответа закончились удачно- сбос счетчика неудачных попыток
                } else {
                    iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "FAILED store sync out data to server! Reason: {0}", e.getLocalizedMessage());
                iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
            }
            if (iErrCount>=Msg_Fail_Count) break; //если количество неудачных попыток отправки запроса и обработки ответа с сервера >= Msg_Fail_Count обработка прерывается
        }
        // ----------поочередная отправка сообщений с данными клиента для применения на сервере и прием ответов по результату применения данных на сервере----------
        //в количестве, установленном параметром "MsgPackageCount"
        iErrCount = 0; //счетчик неудачных отправок
        for (int i=1;i<= Msg_Package_Count;i++) {
            logger.log(Level.INFO, "REQUEST TO APPLY SYNC OUT DATA.....................................................................................");
            try {
                HashMap<String,String> notAppliedOutputSyncData= null;
                if ((notAppliedOutputSyncData=SyncDB.getNotAppliedOutputSyncData(cvoDBSession))==null) break;//если нет данных для отправки на сервер
                HashMap<String,Object> serverOutSyncDataApplyResult=
                        requestToSyncService.applySyncOutData(notAppliedOutputSyncData);
                if (SyncDB.updOutDataStateApplyOnServer(cvoDBSession, notAppliedOutputSyncData.get("ID"), serverOutSyncDataApplyResult)) {
                    iErrCount = 0; //если отправка запроса и обработка ответа закончились удачно- сбос счетчика неудачных попыток
                } else {
                    iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "FAILED apply sync out data on server! Reason: {0}", e.getLocalizedMessage());
                iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
            }
            if (iErrCount>=Msg_Fail_Count) break; //если количество неудачных попыток отправки запроса и обработки ответа с сервера >= Msg_Fail_Count обработка прерывается
        }
        requestToSyncService.close();
    }
    
    /* Проверка на наличие уже запущенного экземпляра приложения. */
    private static boolean isAppRegister() {
        try {
            Registry registry = LocateRegistry.getRegistry();           
            AppMessage m_appstub = (AppMessage) registry.lookup("AppMessage");        
            if (ClientInstanceInfo.CLIENT_NAME.equalsIgnoreCase(m_appstub.getMessage())) {
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "FAILED to verify registration another instatnce.");
        }
        return false;
    }
    /* Регистрация экземпляра приложения. */
    private static void instanceRegistering() {
        try {
            m_registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            m_message = new AppMessageImpl();
            AppMessage m_stub = (AppMessage) UnicastRemoteObject.exportObject(m_message, 0);
            m_registry.bind("AppMessage", m_stub); 
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FAILED to registry instatnce.",e);
        }
    }
    /* Удаление объектов регистрации приложения из реестра. */
    private static void instanceUnregistering() {
        try {
            m_registry.unbind("AppMessage");
            UnicastRemoteObject.unexportObject(m_message, true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FAILED to unregister application",e);
        }
    }
    /* Чтение локальных настроек из файла .properties локальных настроек приложения. */
    private static void loadLocalConfig() throws Exception {
        cvoClientLocalConfig = new ClientLocalConfig();
        boolean loadDefaultValues= false;
        try {//чтение настроек из файла
            cvoClientLocalConfig.load();
        } catch (Exception e){
            cvoClientLocalConfig.loadDefault();
            loadDefaultValues= true;
        }
        try {
            cvsDBUrl = cvoClientLocalConfig.getProperty("db.URL");
            cvsDBUser = cvoClientLocalConfig.getProperty("db.user");
            cvsDBPassword = cvoClientLocalConfig.getProperty("db.password");
            cvsServiceURL=cvoClientLocalConfig.getProperty("SyncService.URL");
            try{
                Msg_Package_Count =Integer.valueOf(cvoClientLocalConfig.getProperty("MsgPackageCount"));
            } catch (Exception e){
                Msg_Package_Count= 100;
            }
            try{
                Msg_Fail_Count=Integer.valueOf(cvoClientLocalConfig.getProperty("MsgFailCount"));
            } catch (Exception e){
                Msg_Fail_Count= 10;
            }
            try{
                Term_Storage_Day =Integer.valueOf(cvoClientLocalConfig.getProperty("TermStorageDay"));
            } catch (Exception e){
                Term_Storage_Day = 100;
            }
        } catch (Exception e){
            logger.log(Level.INFO, "FAILED to set local config parameters!");
        }
        if(!loadDefaultValues)
            logger.log(Level.INFO, "Loaded local parameters:\n db.URL={0} db.user={1} db.password={2} SyncService.URL={3} MsgPackageCount={4} TermStorageDay={5}",
                    new Object[]{cvsDBUrl, cvsDBUser,cvsDBPassword, cvsServiceURL, Msg_Package_Count, Term_Storage_Day});
        else logger.log(Level.INFO, "Setted default local parameters:\n db.URL={0} db.user={1} db.password={2} SyncService.URL={3} MsgPackageCount={4} TermStorageDay={5}",
                new Object[]{cvsDBUrl, cvsDBUser, cvsDBPassword, cvsServiceURL, Msg_Package_Count, Term_Storage_Day});
        if(loadDefaultValues){
            try {
                cvoClientLocalConfig.save();// запись локальных настроек в файл .properties
            } catch (Exception e){
                logger.log(Level.INFO, "FAILED to save local parameters!",e);
            }
        }
    }
}
