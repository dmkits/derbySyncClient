/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-14
 */
package derbysyncclient;

/** ClientInstanceInfo
 * Информация об экзепляре клиента и сервисе с которым он работает.
 * @author dmk.dp.ua 2014-03-14
 * v1.0 - первая версия с отправкой данных на сервер.
 * v1.1 - версия с разделением отправки-приема и обработки.
 * v1.2 - версия с отправкой запросов на данные с сервера.
 * v1.3 - оптимизирован процесс обмена данными (сначала отправка запросов 
 *       на данные с сервера потом отправка запросов с данными клиента)
 * v1.4 - Сделана корректная обработка пустых сообщений с сервера.
 * v1.5 - Удаление переданных на сервер данных.
 */
public class ClientInstanceInfo {
    
    public static final String SERVICE_NAME = "MSSQLSyncService";
    public static final String SERVICE_ID = "mssqlsyncservice";
    public static final String SERVICE_VERSION = "1.5";
    
    public static final String CLIENT_NAME = "DerbySyncClient";
    public static final String CLIENT_ID = "derbysyncclient";
    public static final String CLIENT_VERSION = "1.5";
}
