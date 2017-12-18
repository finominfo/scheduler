package hu.finominfo.sheduler.common;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Globals {
    private static Globals ourInstance = new Globals();

    public static Globals getInstance() {
        return ourInstance;
    }

    private Globals() {
    }

    private final String configFile = "config.csv";
    private final String resultFile = "result.csv";

    public String getConfigFile() {
        return configFile;
    }

    public String getResultFile() {
        return resultFile;
    }
}
