package hu.finominfo.scheduler;

import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main {


    private static volatile Logger LOGGER;


    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("./log4j.properties");
        LOGGER = LogManager.getLogger(Main.class.getName());
        LOGGER.warn("Started...");
        try {
            MainTask mainTask = new MainTask(args);
            mainTask.make();
        } catch (Exception e) {
            String message = "";
            for(StackTraceElement stackTraceElement : e.getStackTrace()) {
                message = message + System.lineSeparator() + stackTraceElement.toString();
            }
            LOGGER.error("Something weird happened. I will print the the complete stacktrace even if we have no exception just to help you find the cause" + message);
        }
    }


}
