package hu.finominfo.sheduler.scheduler;

import hu.finominfo.sheduler.people.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kks on 2017.12.18..
 */
public class Scheduler {
    private final Map<String, Person> people;
    private final Map<Integer, List<String>> scheduled = new HashMap<>();

    public Scheduler(Map<String, Person> people) {
        this.people = people;
        setWanted();
    }

    private void setWanted() {
        people.entrySet().stream().forEach(entry -> entry.getValue().getWantedDays().forEach(wantedDay -> {
            List<String> list = scheduled.get(wantedDay);
            if (list == null) {
                list = new ArrayList<String>();
                scheduled.put(wantedDay, list);
            }
            list.add(entry.getKey());
        }));
    }

    public Map<Integer, List<String>> getScheduled() {
        return scheduled;
    }
}
