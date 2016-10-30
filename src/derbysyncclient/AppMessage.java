/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-14
 */
package derbysyncclient;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Интерфейс для удаленного вызова при проверке наличия уже запущенного экзепляра приложения.
 * @author dmk.dp.ua 2014-04-14
 */
public interface AppMessage extends Remote {
    
    /* Сообщение от запущенного экземпляра
     */
    public String getMessage() throws RemoteException;
    
}
