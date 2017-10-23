/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-14
 */
package derbysyncclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** ClientLocalConfig
 * Локальные настройки клиента.
 * По умолчанию локальные настройки хранятся в папке приложения в файле config.properties.
 * Содержит методы чтения настроек из файла, записи настроек в файл,
 *  метод записи значений и получения значений настроек по ключу,
 *  метод, устанавливающий настройки по умолчанию, метод удаления файла настроек.
 * @author dmk
 * @version 1.0 (2014-03-14)
 */
public class ClientLocalConfig {

    private Properties m_propsconfig;
    private File configfile;
      
    public ClientLocalConfig() {
        init(getDefaultConfigFile());
    }

    public ClientLocalConfig(String sconfigfilename) {
        if (sconfigfilename.equals("")) {
            init(getDefaultConfigFile());
        } else {
            init(new File(sconfigfilename));
        }
    }
    
    public ClientLocalConfig(File configfile) {
        init(configfile);
    }
    
    private void init(File configfile) {
        this.configfile = configfile;
        m_propsconfig = new Properties();
    }
    
    private File getDefaultConfigFile() {
        String dirname = System.getProperty("dirname.path");
        dirname = dirname == null ? "./" : dirname;
        return new File(new File(dirname), "config.properties");
    }
    
    public File getConfigFile() {
        return configfile;
    }

    public String getProperty(String sKey) {
        return m_propsconfig.getProperty(sKey);
    }
    
    public void setProperty(String sKey, String sValue) {
        if (sValue == null) {
            m_propsconfig.remove(sKey);
        } else {
            m_propsconfig.setProperty(sKey, sValue);
        }
    }
    
    public void load() throws IOException {
        InputStream in = new FileInputStream(configfile);
        if (in != null) {
            m_propsconfig.load(in);
            in.close();
        }
    }
    
    public void loadDefault() {
        String dirname = System.getProperty("dirname.path");
        dirname = dirname == null ? "./" : dirname;
        //---------- data for connect to derby database ----------
        //m_propsconfig.setProperty("db.driverlib", new File(new File(dirname), "lib/derby.jar").getAbsolutePath());
        //m_propsconfig.setProperty("db.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        m_propsconfig.setProperty("db.driverlib", new File(new File(dirname), "lib/derbyclient.jar").getAbsolutePath());
        m_propsconfig.setProperty("db.driver", "org.apache.derby.jdbc.ClientDriver");
        m_propsconfig.setProperty("db.URL", "jdbc:derby://localhost:1527/<databasename>");
        m_propsconfig.setProperty("db.user", "APP");
        m_propsconfig.setProperty("db.password", "APP");
        //---------- data for connect to hsqldb database ----------
//        m_propsconfig.setProperty("db.driverlib", new File(new File(dirname), "lib/hsqldb.jar").getAbsolutePath());
//        m_propsconfig.setProperty("db.driver", "org.hsqldb.jdbcDriver");
//        m_propsconfig.setProperty("db.URL", "jdbc:hsqldb:file:" + new File(new File(System.getProperty("user.home")), AppLocalization.APP_ID + "-db").getAbsolutePath() + ";shutdown=true");
//        m_propsconfig.setProperty("db.user", "sa");
//        m_propsconfig.setProperty("db.password", "");
        //---------- data for connect to mysql database ----------
//        m_propsconfig.setProperty("db.driver", "com.mysql.jdbc.Driver");
//        m_propsconfig.setProperty("db.URL", "jdbc:mysql://localhost:3306/database");
//        m_propsconfig.setProperty("db.user", "user");         
//        m_propsconfig.setProperty("db.password", "password");
        //---------- data for connect to postgresql database ----------
//        m_propsconfig.setProperty("db.driver", "org.postgresql.Driver");
//        m_propsconfig.setProperty("db.URL", "jdbc:postgresql://localhost:5432/database");
//        m_propsconfig.setProperty("db.user", "user");         
//        m_propsconfig.setProperty("db.password", "password");        
        //---------- data for connect to SyncService ----------
        m_propsconfig.setProperty("SyncService.URL", "http://localhost:8080/"); //URL сервиса без имени службы
        m_propsconfig.setProperty("MsgPackageCount", "1000"); // максимальное кол-во пакетов, отправляемых за 1 раз
        /* срок хранения данных дней (свыше которого переданные и примененные данные удаляются вместе с журналом передачи */
        m_propsconfig.setProperty("TermStorageDay", "7"); 
}

    public void save() throws IOException {
        OutputStream out = new FileOutputStream(configfile);
        if (out != null) {
            m_propsconfig.store(out, ClientInstanceInfo.CLIENT_NAME + ". Configuration file.");
            out.close(); 
        }
    }

    public boolean delete() {
        loadDefault();
        return configfile.delete();
    }
}
