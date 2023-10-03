package hu.finominfo.scheduler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import hu.finominfo.scheduler.scheduler.Scheduler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class Main {


    private static final Logger LOGGER = LogManager.getLogger(Main.class);


    public static void main(String[] args) throws Exception {
        //System.setProperty("log4j.configurationFile", String.valueOf(new File("resources", "log4j.xml").toURI()));
        System.setProperty("log4j.configurationFile", "log4j2.properties");
        System.setProperty("logFilename", "scheduler.log");
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
