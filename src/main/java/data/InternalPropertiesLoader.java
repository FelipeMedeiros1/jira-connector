package data;

import exceptions.AutomationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class InternalPropertiesLoader {

    static final Logger logger = LogManager.getLogger(InternalPropertiesLoader.class);

    private PropertiesLoader projectProperties;
    private Properties frameworkProperties;
    private String fileName;

    public InternalPropertiesLoader(String fileName){
        this.fileName = fileName;
        if(fileName.startsWith("/") || fileName.startsWith("\\"))
            fileName = fileName.substring(1, fileName.length());
        File projectPropertiesFile = new File(System.getProperty("user.dir") + "/src/test/resources/configuration.properties");
        if(projectPropertiesFile.exists()){
            projectProperties = new PropertiesLoader("configuration.properties");
        }

        frameworkProperties = new Properties();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = loader.getResourceAsStream(fileName);
            frameworkProperties.load(inputStream);
        }catch (Exception e){
            throw new AutomationException("Falha ao carregar a properties '%s' do framework - [%s]", fileName, e.getMessage());
        }
    }

    public String getValue(String key){
        if(projectProperties != null){
            if(projectProperties.getValue(key) != null){
                return projectProperties.getValue(key);
            }
        }

        try {
            return frameworkProperties.getProperty(key);
        } catch(Exception e ) {
            throw new AutomationException("A chave '%s' não foi localizada no arquivo properties '%s' do framework", key, fileName);
        }
    }

	public void setValue(String key, String value) {
		projectProperties.setValue(key, value);
		
	}
}
