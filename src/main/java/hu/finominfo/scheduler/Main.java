package hu.finominfo.scheduler;

import hu.finominfo.scheduler.common.Globals;
import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Type;
import hu.finominfo.scheduler.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {


    public static void main(String[] args) throws IOException {
        MainTask mainTask = new MainTask(args);
        mainTask.make();
    }


}
