package hu.finominfo.scheduler;

import java.io.IOException;

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


    static {
        System.setProperty("log4j.xml", "resources/log4j.xml");
        System.setProperty("logFilename", "scheduler.log");
    }

    private static final Logger LOGGER = LogManager.getLogger(Main.class);


    public static void main(String[] args) {
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
