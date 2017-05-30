/* derbysyncclient
 * @autor dmk.dp.ua 2014-03-14
 */
package derbysyncclient;

/** ClientInstanceInfo
 * Информация об экзепляре клиента и сервисе с которым он работает.
 * @author dmk.dp.ua 2014-03-14
 * v.1.0 - первая версия с отправкой данных на сервер.
 * v.1.1 - версия с разделением отправки-приема и обработки.
 * v.1.2 - версия с отправкой запросов на данные с сервера.
 * v.1.3 - оптимизирован процесс обмена данными (сначала отправка запросов
 *       на данные с сервера потом отправка запросов с данными клиента)
 * v.1.4 - Сделана корректная обработка пустых сообщений с сервера.
 * v.1.5 - Удаление переданных на сервер данных.
 * v.1.5.1 - передача во вложении всех колонок данных кроме колонок с названием ATTRIBUTES
 * v.1.6 - в пакете с данными клиента для принятия сервером теперь передается и дата создания записи и ключи таблицы клиента.
 *          версия работает с сервером синхронизации 1.6.
 */
public class ClientInstanceInfo {
    
    public static final String SERVICE_NAME = "MSSQLSyncService";
    public static final String SERVICE_ID = "mssqlsyncservice";
    public static final String SERVICE_VERSION = "1.6";
    
    public static final String CLIENT_NAME = "DerbySyncClient";
    public static final String CLIENT_ID = "derbysyncclient";
    public static final String CLIENT_VERSION = "1.6";
}
