/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/** TextFromResource
 * Чтение текста из файла ресурса приложения.
 * @author dmk.dp.ua 2014-04-15
 */
public class TextFromResource {

    private static final Logger logger = Logger.getLogger("derbysyncclient.TextFromResource");

    public TextFromResource() {
    }
    
    public static String load(String sResName) throws Exception {
        logger.log(Level.FINE, "Reading text data from resource \""+sResName+"\".");
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = DerbySyncClient.class.getResourceAsStream(sResName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "Cp1251"));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break; }
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw new Exception("FAILED to read resource \""+sResName+"\"! "+e.getMessage());
        }
    }
}
