package hu.finominfo.scheduler;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.scheduler.Scheduler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Main {


    public static void main(String[] args) throws IOException {
        LocalDate localDate = LocalDateTime.now().toLocalDate().plusMonths(1);
        People people = new People();
        Scheduler scheduler = new Scheduler(people.getPeople(), localDate);
        System.out.println(scheduler.getScheduled());
    }
}
