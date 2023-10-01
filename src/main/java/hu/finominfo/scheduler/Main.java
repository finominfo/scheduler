package hu.finominfo.scheduler;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
        MainTask mainTask = new MainTask(args);
        mainTask.make();
    }


}
