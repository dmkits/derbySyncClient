/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-25
 */
package derbysyncclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/** Session
 * Соединение с базой данных.
 * @author dmk.dp.ua 2014-03-24
 */
public class Session  {
    
    private String cvsURL;
    private String cvsUserName;
    private String cvsUserPassword;
    
    private Connection cvoCon;
    private boolean cvbInTransaction;

   
    /** Creates a new instance of Session */
    public Session() {
        
        cvsURL = "";
        cvsUserName = "";
        cvsUserPassword = "";

        cvoCon = null;
        cvbInTransaction = false;
    }
    
    public void setConnectParams(String surl, String suser, String spassword) {
        cvsURL = surl;
        cvsUserName = suser;
        cvsUserPassword = spassword;
    }
    
    public void connect() throws SQLException {
        
        // rollback and close all
        close();
        // new connection
        cvoCon = (cvsUserName == null && cvsUserPassword == null)
                ? DriverManager.getConnection(cvsURL)
                : DriverManager.getConnection(cvsURL, cvsUserName, cvsUserPassword);         
        cvoCon.setAutoCommit(true);
        cvbInTransaction = false;
    }     

    public void close() {
        
        if (cvoCon != null) {
            try {
                if (cvbInTransaction) {
                    cvbInTransaction = false; 
                    cvoCon.rollback();
                    cvoCon.setAutoCommit(true);  
                }            
                cvoCon.close();
            } catch (SQLException e) {
                // 
            } finally {
                cvoCon = null;
            }
        }
    }
    
    public Connection getConnection() throws SQLException {
        
        if (!cvbInTransaction) {
            ensureConnection();
        }
        return cvoCon;
    }
    
    public void begin() throws SQLException {
        
        if (cvbInTransaction) {
            throw new SQLException("Already in transaction");
        } else {
            ensureConnection();
            cvoCon.setAutoCommit(false);
            cvbInTransaction = true;
        }
    }
    public void commit() throws SQLException {
        if (cvbInTransaction) {
            cvbInTransaction = false; // 
            cvoCon.commit();
            cvoCon.setAutoCommit(true);          
        } else {
            throw new SQLException("Transaction not started");
        }
    }
    public void rollback() throws SQLException {
        if (cvbInTransaction) {
            cvbInTransaction = false; // 
            cvoCon.rollback();
            cvoCon.setAutoCommit(true);            
        } else {
            throw new SQLException("Transaction not started");
        }
    }
    public boolean isTransaction() {
        return cvbInTransaction;
    }
    
    private void ensureConnection() throws SQLException {
        // только при isTransaction == false
        boolean bclosed;
        try {
            bclosed = cvoCon == null || cvoCon.isClosed();
        } catch (SQLException e) {
            bclosed = true;
        }
        // reconnect if closed
        if (bclosed) {
            connect();
        }
    }  

    public String getURL() throws SQLException {
        return getConnection().getMetaData().getURL();
    }

}
