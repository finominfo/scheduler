package hu.finominfo.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try {
            MainTask mainTask = new MainTask(args);
            mainTask.make();
        } catch (Exception e) {
            logger.error(e);
            String message = "";
            for(StackTraceElement stackTraceElement : e.getStackTrace()) {
                message = message + System.lineSeparator() + stackTraceElement.toString();
            }
            logger.error("Something weird happened. I will print the the complete stacktrace even if we have no exception just to help you find the cause" + message);
        }
    }


}
