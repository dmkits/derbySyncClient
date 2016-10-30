/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-14
 */
package derbysyncclient;

/** Реализация интерфейса удаленного вызова при проверке наличия уже запущенного экземпляра приложения.
 * @author dmk.dp.ua 2014-04-14
 */
public class AppMessageImpl implements AppMessage {
    
    /* Сообщение от запущенного экземпляра - имя приложения.
     */
    @Override
    public String getMessage() {
        return ClientInstanceInfo.CLIENT_NAME;
    }
    
}
