package data;

import exceptions.AutomationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gerencia a leitura do arquivo properties
 *
 */

public class PropertiesLoader {
	
	static final Logger logger = LogManager.getLogger(PropertiesLoader.class);

    private Properties properties;
    private String fileName;

    public PropertiesLoader(String fileName){
    	this.fileName = fileName;
    	try {
    		FileInputStream inp = new FileInputStream(new File(System.getProperty("user.dir") + "/src/test/resources/" + fileName));
    		properties = new Properties();
            properties.load(inp);
		} catch (IOException e) {
			throw new AutomationException(e);
		}
    }
    
    public String getValue(String key) {
    	try {
            return properties.getProperty(key);
    	} catch(Exception e ) {
			throw new AutomationException("A chave '%s' não foi localizada no arquivo properties '%s'.", key, fileName);
		}
    }

	public Properties getProperties() {
		return properties;
	}

	public void setValue(String key, String value) {
		properties.setProperty(key, value);
		
	}
}
