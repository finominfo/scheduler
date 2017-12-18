package hu.finominfo.sheduler;

import hu.finominfo.sheduler.people.People;
import hu.finominfo.sheduler.people.Person;

import java.io.IOException;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        People people = new People();
        System.out.println(people.getPeople().entrySet().stream().findAny());
    }
}
