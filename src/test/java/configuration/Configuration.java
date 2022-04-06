package configuration;


import java.io.InputStream;
import java.util.Properties;


public class Configuration {
    private static Configuration instance;
    Properties configProps;
    static final String PROP_FILE_NAME = "config.properties";

    public static Configuration getInstance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    private static synchronized void createInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
    }

    private Configuration() {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(PROP_FILE_NAME)) {
            configProps = new Properties();
            configProps.load(is);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Properties read finished.");
        }
    }

    public String getStringValueOfProp(String propKey) {
        return  configProps.getProperty(propKey);
    }

}