/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-14
 */
package derbysyncclient;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.*;

/** DerbySyncClient
 * Клиент Web-сервиса синхронизации данных с базой MSSQL.
 * Web-сервис построен на базе спецификаций JAXM и SAAJ.
 * Клиент отправляет сервису SOAP-сообщения-запросы и принимает от сервиса SOAP-сообщения-ответы.
 * @author dmk.dp.ua 2014-03-14
 */
public class DerbySyncClient {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.DerbySyncClient");
    
    private static Registry m_registry = null;
    private static AppMessage m_message = null;
    
    private static ClientLocalConfig cvoClientLocalConfig = null;
    
    private static String cvsDBUrl = null;
    private static String cvsDBUser = null;
    private static String cvsDBPassword = null;
    private static Session cvoDBSession = null;

    private static String cvsServiceURL = null;
    private static URL cvoURLEndpoint = null;
    private static SOAPConnection cvoSOAPConnection = null;
    
    private static int cviCountID=0; // количество отправок сообщений за 1 сеанс связи
    private static int cviTermStorageDay=0; // срок хранения данных свыше которого данные удаляются

    private DerbySyncClient() {
    }
            
    /** Запуск приложения.
     * @param args the command line arguments
     * Запуск приложения.
     * Вывод стартовых и завершающих логов. 
     * Проверка регистрации приложения. Регистрация запускаемого приложения и удаление регистрации по завершении работы. */
    public static void main(final String[] args) {
        logger.log(Level.INFO, "");
        logger.log(Level.INFO, "");
        logger.log(Level.INFO, "------------------------------Starting DerbySyncClient------------------------------");

        // проверка на наличие уже запущенного экземпляра приложения
        if (isAppRegister()) {
            logger.log(Level.WARNING, "FAILED launch a second instance of " + ClientInstanceInfo.CLIENT_NAME + "!");
            return;
        }
        // регистрация запущенного приложения
        instanceRegistering();

        try {
            if (init(args)) return;//if did update
        } catch (Exception e) {
            logger.log(Level.WARNING, "FAILED init " + ClientInstanceInfo.CLIENT_NAME + "! Reason:"+e.getLocalizedMessage());
            return;
        }

        // формирование, отправка сообщений и прием и обработка ответов.
        try {
            run(args);
        } catch (Exception e) {
            logger.log(Level.WARNING, "----------!!!!!!!!!!DerbySyncClient aborted!!!!!!!!!!----------");
            logger.log(Level.WARNING, "CAUSE: {0}", e.getMessage());
        }

        // удаление регистрации приложения
        instanceUnregistering();
        logger.log(Level.INFO, "------------------------------DerbySyncClient finished------------------------------");
    }
    
    private static boolean init(String[] args) throws Exception {
        // чтение локальных параметров из файла настроек .properties
        loadLocalConfig();
        //----------регистрация драйвера базы данных, из которой извлекаются данные для отправки----------
        regDBDriver();
        //----------подключение к базе данных, из которой извлекаются данные для отправки----------
        connectToDB();

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
            SyncDB.delApplSyncData(cvoDBSession, cviTermStorageDay);
            return true;
        }
        return false;
    }

    /** Формирование и отправка сообщений-запросов, прием и обработка сообщений-ответов.
     * @param args the command line arguments
     * Из файла локальных настроек .properties читаются локальные параметры приложения,
     * производится соединение с сервисом синхронизации на сервере,
     * производится соединение с БД derby.
     * и из таблицы с информацией о данных синхронизации на отправку читаются данные.
     * Сначала извлекаются данные для применения, если данные для применения есть формируется и отправляется запрос на применение, 
     * если данных на применение нет, извлекаются данные на прием и отправляется запрос на прием, 
     * затем снова извлекаются данные на применение и отправляется запрос на применение.
     * При формировании запроса на прием считанные данные записываются в сообщение-запрос, также в сообщение приложением записываются сами данные синхронизации,
     * сообщение-запрос отправляется сервису синхронизации на сервере и принимается сообщение-ответ,
     * по данным из сообщения-ответа обновляются поля в таблице с информацией о данных синхронизации на отправку. 
     * Количество отправок сообщений за один сеанс работы приложения определяется локальным параметром MsgPackageCount. */
    private static void run(String[] args) throws Exception {
        // ----------подготовка соединения с сервисом синхронизации на сервере----------
        prepSyncServiceConnnection();
        // ----------поочередная отправка сообщений с запросами на данные с сервера и прием и обработка ответов с данными с сервера----------
        //в количестве, установленном параметром "MsgPackageCount"
        int iErrCount = 0; //счетчик неудачных отправок
        for (int i=1;i<=cviCountID;i++) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "----------Prepearing and Sending request to get data from server and receiving and handling response----------");
            try {
                if (!getDataFromServer()) { //запрос на данные с сервера
                    break; //если на сервере нет данных для клиента
                }
                iErrCount=0; //если обработка ответа закончилась удачно- сбос счетчика неудачных попыток
            } catch (Exception e) {
                logger.log(Level.WARNING, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\b\n FAILED: {0}", e.getMessage());
                iErrCount++; //счетчик неудачных попыток обработки ответа с сервера
                if (iErrCount>=5) { break; } //если количество неудачных попыток обработки ответа с сервера >=5 обработка прерывается
            }
        }
        // ----------поочередная отправка сообщений с данными клиента для приема и применения на сервере и прием ответов по результату приема данных на сервере----------
        //в количестве, установленном параметром "MsgPackageCount"
        boolean bNoDataForSend = false, bNoDataForUpdate = false; //признаки отсутствия данных для отправки
        iErrCount = 0; //счетчик неудачных отправок
        for (int i=1;i<=cviCountID;i++) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "----------Prepearing and Sending request with client data to SyncService and receiving and handling response----------");
            try {
                if (sendClientData()) { //запрос на прием данных с клиента
                    //если есть данные для отправки запроса с данными клиента и произведены отправка запроса и получение ответа
                    bNoDataForSend=false;
                    iErrCount=0; //если отправка запроса и обработка ответа закончились удачно- сбос счетчика неудачных попыток
                } else { 
                    bNoDataForSend=true; //нет данных для отправки
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\b\n FAILED: {0}", e.getMessage());
                iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
                if (iErrCount>=5) { break; } //если количество неудачных попыток отправки запроса и обработки ответа с сервера >=5 обработка прерывается
                continue; 
            }
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, "----------Prepearing and Sending request to update status to SyncService and receiving and handling response----------");
            try {
                if (bNoDataForSend && bNoDataForUpdate) { break; } //выход из цикла если по результату попыток 2-ух отправок нет данных
                if (sendUpdRequest()) { //запрос на обновление статуса- применение данных клиента
                    //если есть данные для отправки запроса с данными клиента и произведены отправка запроса и получение ответа
                    bNoDataForUpdate=false;
                    iErrCount=0; //если отправка запроса и обработка ответа закончились удачно- сбос счетчика неудачных попыток
                } else { 
                    bNoDataForUpdate=true; //нет данных для отправки или статус примененных данных не положительный
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\b\n FAILED: {0}", e.getMessage());
                iErrCount++; //счетчик неудачных попыток отправки запроса и обработки ответа с сервера
                if (iErrCount>=5) { break; } //если количество неудачных попыток отправки запроса и обработки ответа с сервера >=5 обработка прерывается
                continue; 
            }
            if (bNoDataForSend && bNoDataForUpdate) { break; } //выход из цикла если по результату попыток 2-ух отправок нет данных
        }
        try { //закрытие SOAP-соединения
            cvoSOAPConnection.close();
        } catch (Exception e) {
        }
    }
    
    /* Формирование и отправка сообщения-запроса на прием данных синхронизации от клиента и получение и обработка ответа с сервера о приеме данных. 
       Если нет данных для отправки - результат false,
       если есть данные и отправка, получение ответа и обработка ответа прошли удачно-результат true.
       В случае ошибок генерируются исключения. */
    private static boolean sendClientData() throws Exception {
        SOAPMessage request;
        ClientSyncDataToSend oCSDTS = new ClientSyncDataToSend(cvoDBSession);
        try { //---создание сообщения-запроса---
            if (!oCSDTS.getSyncDataInf()) { return false; } //если в БД клиента в таблице синхронизации нет данных для отправки на прием на сервере
            request = oCSDTS.getRequest(); //получение сообщения-запроса
        } catch (Exception e) {
            throw new Exception("FAILED to get SOAP request message! "+e.getMessage());
        }
        SOAPMessage response;
        try { //---отправка сообщения-запроса и получение сообщения-ответа---
            logger.log(Level.INFO, "Sending request message and receiving response message.");
            response = cvoSOAPConnection.call(request, cvoURLEndpoint);
        } catch (Exception e) {
            throw new Exception("FAILED to send SOAP request message to sync service! "+e.getMessage());
        }
        try { //---обработка ответа---
            int iDataState;
            if ( (iDataState=oCSDTS.handlingResponse(response))<0)
                throw new Exception("FAILED to success handle SOAP response message! Reason: data state is "+Integer.toString(iDataState)+"!");
        } catch (Exception e) {
            throw new Exception("FAILED to handle SOAP response message! "+e.getMessage());
        }
        return true;
    }
    
    /* Формирование и отправка сообщения-запроса на применение данных синхронизации от клиента и получение и обработка ответа с сервера о применении данных. 
       Если нет данных для отправки - результат false,
       если есть данные и отправка, получение ответа и обработка ответа прошли удачно и статус применения данных >0 -результат true.
       В случае ошибок генерируются исключения. */
    private static boolean sendUpdRequest() throws Exception {
        SOAPMessage request;
        ClientSyncDataToUpdStatus CSDTUS2 = new ClientSyncDataToUpdStatus(cvoDBSession);
        try { //---создание сообщения-запроса---
            if (!CSDTUS2.getSyncDataInf()) { return false; } //если в БД клента в таблице синхронзации нет данных для отправки на применение на сервере
            request = CSDTUS2.getRequest(); //получение сообщения-запроса
        } catch (Exception e) {
            throw new Exception("FAILED to get SOAP request message! "+e.getMessage());
        }
        SOAPMessage response;
        try { //---отправка сообщения-запроса и получение сообщения-ответа---
            logger.log(Level.INFO, "Sending request message and receiving response message.");
            response = cvoSOAPConnection.call(request, cvoURLEndpoint);
        } catch (Exception e) {
            throw new Exception("FAILED to send SOAP request message to sync service! "+e.getMessage());
        }
        try { //---обработка ответа---
            int iDataAppliedState;
            if ( !((iDataAppliedState=CSDTUS2.handlingResponse(response))>0) )
                throw new Exception("FAILED to success handle SOAP response message! Reason: data state is "+Integer.toString(iDataAppliedState)+"!");
        } catch (Exception e) {
            throw new Exception("FAILED to handle SOAP response message! "+e.getMessage());
        }
        return true;
    }
    
    /* Формирование и отправка сообщения-запроса на данные с сервера и получение и обработка ответа с данными с сервера,
     * после чего формирование и отправка запроса с данными о результате приема-применения данных. */
    private static boolean getDataFromServer() throws Exception {
        SOAPMessage request;
        ClientSyncDataToGet CSDTG = new ClientSyncDataToGet(cvoDBSession);
        try { //---создание сообщения-запроса---
            request = CSDTG.getRequest();
        } catch (Exception e) {
            throw new Exception("FAILED to get SOAP request message! "+e.getMessage());
        }
        SOAPMessage response = null;
        try { //---отправка сообщения-запроса и получение сообщения-ответа---
            logger.log(Level.INFO, "Sending request message and receiving response message.");
            response = cvoSOAPConnection.call(request, cvoURLEndpoint);
        } catch (Exception e) {
            throw new Exception("FAILED to send SOAP request message to sync service! "+e.getMessage());
        }
        try { //---обработка ответа---
            if (!CSDTG.handlingResponse(response)) { //если пришедшее сообщение пустое без данных с сервера
                return false;
            }
        } catch (Exception e) {
            throw new Exception("FAILED to handle SOAP response message! "+e.getMessage());
        }
        try { //---создание сообщения-запроса с данными о результате получения и применения данных с сервера---
            request = CSDTG.getRequestWithResult();
        } catch (Exception e) {
            throw new Exception("FAILED to get SOAP request message! "+e.getMessage());
        }
        try { //---отправка сообщения-запроса и получение сообщения-ответа---
            logger.log(Level.INFO, "Sending request message and receiving response message.");
            response = cvoSOAPConnection.call(request, cvoURLEndpoint);
        } catch (Exception e) {
            throw new Exception("FAILED to send SOAP request message to sync service! "+e.getMessage());
        }
        return true;
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
            logger.log(Level.WARNING, "FAILED to registry instatnce.",e);
        }
    }
    
    /* Удаление объектов регистрации приложения из реестра. */
    private static void instanceUnregistering() {
        try {
            m_registry.unbind("AppMessage"); 
            UnicastRemoteObject.unexportObject(m_message, true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "FAILED to unregister application",e);
        }
    }
    
    /* Чтение локальных настроек из файла .properties локальных настроек приложения. */
    private static void loadLocalConfig() throws Exception {
        try {//чтение настроек из файла
            logger.log(Level.INFO, "Loading local parameters.");
            cvoClientLocalConfig = new ClientLocalConfig();
            cvoClientLocalConfig.load();
            cvsDBUrl = cvoClientLocalConfig.getProperty("db.URL");
            cvsDBUser = cvoClientLocalConfig.getProperty("db.user");
            cvsDBPassword = cvoClientLocalConfig.getProperty("db.password");
            cvsServiceURL=cvoClientLocalConfig.getProperty("SyncService.URL");
            cviCountID=Integer.valueOf(cvoClientLocalConfig.getProperty("MsgPackageCount"));
            cviTermStorageDay=Integer.valueOf(cvoClientLocalConfig.getProperty("TermStorageDay"));
            //logger.log(Level.INFO, "Loaded local parameters.");
            return;
        } catch (Exception e){
            logger.log(Level.INFO, "FAILED to load local config parameters!");
        }
        try {//установка значений настроек по умолчанию
            logger.log(Level.INFO, "Setting default local parameters.");
            cvoClientLocalConfig.loadDefault();
            cvsDBUrl = cvoClientLocalConfig.getProperty("db.URL");
            cvsDBUser = cvoClientLocalConfig.getProperty("db.user");
            cvsDBPassword = cvoClientLocalConfig.getProperty("db.password");
            cvsServiceURL=cvoClientLocalConfig.getProperty("SyncService.URL");
            cviCountID=Integer.valueOf(cvoClientLocalConfig.getProperty("MsgPackageCount"));
            cviTermStorageDay=Integer.valueOf(cvoClientLocalConfig.getProperty("TermStorageDay"));
        } catch (Exception e) {
            throw new Exception("FAILED to set default local config parameters! "+e.getMessage());
        }
        // запись локальных настроек в файл .properties
        try {
            cvoClientLocalConfig.save();
        } catch (Exception e){
            logger.log(Level.INFO, "FAILED to save local parameters!",e);
        }
    }
    
    /* Регистрация драйвера базы данных. */
    private static void regDBDriver() throws Exception {
        logger.log(Level.INFO, "Registering database driver.");
        try {
            ClassLoader cloader = new URLClassLoader(new URL[] {new File(cvoClientLocalConfig.getProperty("db.driverlib")).toURI().toURL()});
            DriverManager.registerDriver((Driver) Class.forName(cvoClientLocalConfig.getProperty("db.driver"), true, cloader).newInstance());
        } catch (Exception e) {
            throw new Exception("FAILED to register database driver! "+e.getMessage());
        }
    }
    
    /* Подключение к базе данных. */
    private static void connectToDB() throws Exception {
        logger.log(Level.INFO, "Connecting to database: \""+cvsDBUrl+"\"");
        try {
            cvoDBSession = new Session(); //
            cvoDBSession.setConnectParams(cvsDBUrl, cvsDBUser,cvsDBPassword);
            cvoDBSession.connect();
        } catch (Exception e) {
            throw new Exception("FAILED to connect to database: \""+cvsDBUrl+"\"! "+e.getMessage());
        }
    }
    
    /* Подготовка URl и SOAP-соединения с сервисом синхронизации на сервере. */
    private static void prepSyncServiceConnnection() throws Exception {
        SOAPConnectionFactory scf = null;
        try {
            logger.log(Level.INFO, "Prepearing URL to SyncService: "+cvsServiceURL+ClientInstanceInfo.SERVICE_NAME);
            cvoURLEndpoint = new URL(cvsServiceURL+ClientInstanceInfo.SERVICE_NAME); 
        } catch (Exception e) {
            throw new Exception("FAILED to prepare URL to SyncService \""+cvsServiceURL+ClientInstanceInfo.SERVICE_NAME+"\"! "+e.getMessage());
        }
        try {
            logger.log(Level.INFO, "Prepearing SOAP connection to SyncService.");
            scf = SOAPConnectionFactory.newInstance();
            cvoSOAPConnection = scf.createConnection();
        } catch (Exception e) {
            throw new Exception("FAILED to prepare SOAP connection to SyncService \""+cvsServiceURL+ClientInstanceInfo.SERVICE_NAME+"\"! "+e.getMessage());
        }
    }
}
