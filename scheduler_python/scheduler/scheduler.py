# scheduler_python/scheduler/scheduler.py

import random
import sys
from collections import defaultdict
import datetime
from ..util.hungarian_holidays import HungarianHolidays
from ..people.type import Type
from ..util.key_value_store import KeyValueStore

class Scheduler:
    """
    Python replica of the Java Scheduler class.
    The logic is intentionally mirrored from Scheduler.java.
    """

    def __init__(self, people_map, local_date):
        """
        :param people_map: Dict[str, Person]
        :param local_date: A datetime.date or something with day=1. 
                           We'll do scheduling for that month.
        """
        self.people = people_map  # name -> Person
        # just like Java: random seeded with System.currentTimeMillis
        self.random_gen = random.Random()
        self.random_gen.seed(int(datetime.datetime.now().timestamp()*1000))

        self.fo_names = {}        # day -> string (who is FO)
        self.scheduled = {}       # day -> set of strings (2 names or less)
        self.hated = {}           # day -> set of strings
        self.num_of_days = self.days_in_month(local_date.year, local_date.month)
        self.local_date = datetime.date(local_date.year, local_date.month, 1)

        # fill scheduled/hated with empty sets from i in [-8..(num_of_days+10]]
        for i in range(-8, self.num_of_days+11):
            self.scheduled[i] = set()
            self.hated[i] = set()

        # figure out all Hungarian holidays in that month
        self.holidays = [d.day for d in HungarianHolidays.get_holidays_for_month(self.local_date.year, self.local_date.month)]
        
        # now we figure out which day-of-week each day is
        self.mondays = []
        self.tuesdays = []
        self.wednesdays = []
        self.thursdays = []
        self.fridays = []
        self.saturdays = []
        self.sundays = []

        self.count_days()

        # replicate logic
        self.set_hated()
        self.set_wanted()
        self.set_desired_number_of_fridays_and_weekends_and_holidays()
        self.set_weekends_and_holidays()
        self.set_weekdays()
        self.balance_ims()

        self.kv_store = KeyValueStore()

    def get_key_value_store(self):
        return self.kv_store

    def get_local_date(self):
        return self.local_date

    def get_num_of_days(self):
        return self.num_of_days

    def get_holidays(self):
        return self.holidays

    def get_saturdays(self):
        return self.saturdays

    def get_sundays(self):
        return self.sundays

    def days_in_month(self, year, month):
        # same as date.lengthOfMonth
        import calendar
        return calendar.monthrange(year, month)[1]

    def count_days(self):
        """
        Java logic uses 'while (result.getMonthValue() == localDate.getMonthValue()) { ... }'
        We'll do a for day in 1..num_of_days
        Then check dayOfWeek
        """
        for day in range(1, self.num_of_days+1):
            d = datetime.date(self.local_date.year, self.local_date.month, day)
            wd = d.weekday()  # Monday=0, Sunday=6 in Python
            if d.year == 2024 and d.month == 8 and day == 3:
                # Special logic from Java? 
                # If (year=2024 && month=8 && day=3) -> mondays
                self.mondays.append(day)
            elif d.year == 2024 and d.month == 8 and day == 19:
                # saturdays
                self.saturdays.append(day)
            else:
                if wd == 0:
                    self.mondays.append(day)
                elif wd == 1:
                    self.tuesdays.append(day)
                elif wd == 2:
                    self.wednesdays.append(day)
                elif wd == 3:
                    self.thursdays.append(day)
                elif wd == 4:
                    self.fridays.append(day)
                elif wd == 5:
                    self.saturdays.append(day)
                elif wd == 6:
                    self.sundays.append(day)

    def set_hated(self):
        """
        Mirroring the Java's setHated method.
        1) For each person, if they have hatesMonday, we add monday to that person's hatedDays, etc.
        2) Then we fill self.hated[day] with that person's name
        3) If a day is hated by people.size - 2, then the 2 left are scheduled automatically.
        """
        for name, person in self.people.items():
            if person.is_hates_mondays():
                for d in self.mondays:
                    person.hated_days.append(d)
            if person.is_hates_tuesdays():
                for d in self.tuesdays:
                    person.hated_days.append(d)
            if person.is_wanted_tuesdays():
                # This logic from Java: if person.isWantedTuesdays() -> add tuesdays to person.wantedDays
                for d in self.tuesdays:
                    person.wanted_days.add(d)
            if person.is_hates_wednesdays():
                for d in self.wednesdays:
                    person.hated_days.append(d)
            if person.is_hates_thursdays():
                for d in self.thursdays:
                    person.hated_days.append(d)
            if person.is_hates_fridays():
                for d in self.fridays:
                    person.hated_days.append(d)
            if person.is_hates_weekends():
                for d in self.saturdays:
                    person.hated_days.append(d)
                for d in self.sundays:
                    person.hated_days.append(d)
            if person.is_hates_weekdays():
                for dd in [*self.mondays, *self.tuesdays, *self.wednesdays, *self.thursdays, *self.fridays]:
                    person.hated_days.append(dd)

        # Now fill self.hated
        n_people = len(self.people)
        for name, person in self.people.items():
            for hated_day in person.hated_days:
                self.hated[hated_day].add(name)

        # if a day is hated by n_people - 2 => only 2 left possible => schedule them
        for day in range(-8, self.num_of_days+11):
            # if that day is in range(1..num_of_days) let's check
            if 1 <= day <= self.num_of_days:
                set_hated = self.hated[day]
                if len(set_hated) > n_people - 2:
                    raise RuntimeError(f"Too many people hate day {day}: {set_hated}")
                if len(set_hated) == n_people - 2:
                    possible_names = set(self.people.keys())
                    possible_names = possible_names - set_hated
                    self.scheduled[day] = set(self.scheduled[day]) | possible_names
                    # if that yields 2 names, pick FO
                    if len(self.scheduled[day]) == 2:
                        self.fo_names[day] = self.select_fo(self.scheduled[day], day)

    def set_wanted(self):
        """
        Mirroring the Java's setWanted method.
        If multiple want the same day, we add them. If more than 2 => exception.
        If exactly 2 and both want FO or both want BO => exception.
        Then pick FO from them.
        """
        for name, person in self.people.items():
            for wday in person.get_wanted_days():
                st = self.scheduled[wday]
                st.add(name)
                if len(st) > 2:
                    raise RuntimeError(f"More than two people want day {wday}: {st}")
                if len(st) == 2:
                    # check if both are FO or both are BO
                    # gather their day type
                    names_list = list(st)
                    t0 = self.people[names_list[0]].get_type(wday)
                    t1 = self.people[names_list[1]].get_type(wday)
                    if t0 == Type.BO and t1 == Type.BO:
                        raise RuntimeError(f"Two BO want the same day {wday}: {st}")
                    if t0 == Type.FO and t1 == Type.FO:
                        raise RuntimeError(f"Two FO want the same day {wday}: {st}")
                    self.fo_names[wday] = self.select_fo(st, wday)

    def set_desired_number_of_fridays_and_weekends_and_holidays(self):
        """
        Mirroring setDesiredNumberOfFridaysAndWeekendsAndHolidays in Java.
        For each person, schedule them on X holidays if they want that many, 
        X fridays, saturdays, sundays, etc.
        """
        for name, p in self.people.items():
            # p.get_num_of_wanted_holidays
            hol_count = p.get_num_of_wanted_holidays()
            if hol_count > 0:
                self.schedule_desired_days(p, hol_count, self.holidays, "holiday")
            fr_count = p.get_num_of_wanted_fridays()
            if fr_count > 0:
                self.schedule_desired_days(p, fr_count, self.fridays, "friday")
            sa_count = p.get_num_of_wanted_saturdays()
            if sa_count > 0:
                self.schedule_desired_days(p, sa_count, self.saturdays, "saturday")
            su_count = p.get_num_of_wanted_sundays()
            if su_count > 0:
                self.schedule_desired_days(p, su_count, self.sundays, "sunday")

    def schedule_desired_days(self, person, count, day_list, day_name):
        """
        Java logic: we see if they're already scheduled on some of those days => decrement the desired count
        Then we pick from the leftover days if there's space ( <2 people ), pick random day for them
        """
        # Already scheduled days in day_list:
        for d in day_list:
            if person.get_name() in self.scheduled[d]:
                count -= 1
        # possibleDays
        possible_days1 = []
        for d in day_list:
            if person.get_name() not in self.scheduled[d]:
                possible_days1.append(d)
        # we only want those which have < 2 people scheduled
        possible_days = [d for d in possible_days1 if len(self.scheduled[d]) < 2]

        if count > 0:
            if len(possible_days) < count:
                raise RuntimeError(f"Not enough days for {person.get_name()} to work on {day_name}s.")
            while count > 0:
                chosen_day = self.random_gen.choice(possible_days)
                if person.get_name() not in self.scheduled[chosen_day]:
                    self.scheduled[chosen_day].add(person.get_name())
                    # pick FO if none chosen yet
                    if (chosen_day not in self.fo_names) or (not self.fo_names[chosen_day]):
                        # pick random from the scheduled set
                        arr = list(self.scheduled[chosen_day])
                        chosen_fo = self.random_gen.choice(arr)
                        self.fo_names[chosen_day] = chosen_fo
                    count -= 1

    def set_weekends_and_holidays(self):
        """
        For saturdays, sundays, holidays => do the logic from setWeekendsAndHolidays in Java.
        We fill up with people who do not hate these days, ensuring coverage.
        """
        # We'll handle saturdays, sundays, holidays in that order or collectively:
        sets_of_days = [self.saturdays, self.sundays, self.holidays]
        for days in sets_of_days:
            ent = self.get_the_most_hated_and_not_scheduled(days)
            while ent[1] is not None:
                day = ent[0]
                # get weekend ordered possibilities
                ordered_persons = self.get_weekend_ordered_possibilities(day)
                # if no one scheduled yet => add the first
                if len(self.scheduled[day]) == 0:
                    self.scheduled[day].add(ordered_persons[0])
                # find second:
                first_person = list(self.scheduled[day])[0]
                self.find_first_good_for(first_person, ordered_persons, day)
                if len(self.scheduled[day]) == 2:
                    self.fo_names[day] = self.select_fo(self.scheduled[day], day)
                ent = self.get_the_most_hated_and_not_scheduled(days)

    def get_weekend_ordered_possibilities(self, saturday_number):
        """
        Java calls getWeekendPossibilities, then orders them by how many times they've been scheduled (plus adjacency).
        We'll replicate that logic. 
        """
        # We'll find all who do not hate that day nor the next day if it is Sunday, etc.
        persons = self.get_weekend_possibilities(saturday_number)
        # we build scheduleNumbers
        schedule_numbers = {}
        for name in persons:
            schedule_numbers[name] = 0

        # adjacency penalty, etc.
        # The Java code does:
        # +14 for each time scheduled in general
        # +4 if scheduled day-2 or day+3
        # +2 if scheduled day-3 or day+4
        # +1000 if scheduled day-7 or day+7
        # +1000 if scheduled day-6 or day+8
        # That might be repeated? We'll replicate literally.

        for d, name_set in self.scheduled.items():
            for n in name_set:
                if n in persons:
                    schedule_numbers[n] += 14
                    # day difference checks
                    if (d == saturday_number - 2) or (d == saturday_number + 3):
                        schedule_numbers[n] += 4
                    if (d == saturday_number - 3) or (d == saturday_number + 4):
                        schedule_numbers[n] += 2
                    if (d == saturday_number - 7) or (d == saturday_number + 7):
                        schedule_numbers[n] += 1000
                    if (d == saturday_number - 6) or (d == saturday_number + 8):
                        schedule_numbers[n] += 1000
        # now order ascending
        return self.order_schedule_numbers(schedule_numbers)

    def get_weekend_possibilities(self, saturday_number):
        """
        The Java code calls getWeekendPossibilities. 
        We combine saturday_number and saturday_number+1 for Sunday.
        We also block scheduling for day-1 and day+2 (?), etc.
        Actually in Java code:
           possible set = everyone
           remove hated[saturday_number], remove hated[sun], remove scheduled[friday], remove scheduled[monday]
        """
        sunday_number = saturday_number + 1
        friday_number = saturday_number - 1
        monday_number = saturday_number + 2
        # The Java code does: removeAll(hated[sat]); removeAll(hated[sun]); removeAll(sched[fri]); removeAll(sched[mon])
        possibilities = set(self.people.keys())
        if saturday_number in self.hated:
            possibilities = possibilities - self.hated[saturday_number]
        if sunday_number in self.hated:
            possibilities = possibilities - self.hated[sunday_number]
        if friday_number in self.scheduled:
            possibilities = possibilities - self.scheduled[friday_number]
        if monday_number in self.scheduled:
            possibilities = possibilities - self.scheduled[monday_number]
        return list(possibilities)

    def set_weekdays(self):
        """
        The Java code calls getTheMostHatedAndNotScheduledDay repeatedly
        Then picks an order of persons with getTheFewestScheduledPerson. 
        Then schedules up to 2.
        """
        day_persons = self.get_the_most_hated_and_not_scheduled_day()
        while day_persons[1] is not None:
            day = day_persons[0]
            ordered_persons = self.get_the_fewest_scheduled_person(day_persons)
            if len(self.scheduled[day]) == 0:
                self.scheduled[day].add(ordered_persons[0])
            first_person = list(self.scheduled[day])[0]
            self.find_first_good_for(first_person, ordered_persons, day)
            if len(self.scheduled[day]) == 2:
                self.fo_names[day] = self.select_fo(self.scheduled[day], day)
            day_persons = self.get_the_most_hated_and_not_scheduled_day()

    def get_the_most_hated_and_not_scheduled_day(self):
        """
        Java code returns a Map.Entry<Integer, Set<String>> with 'position' and the possible set of people
        (i.e. not in hated, not in scheduled, etc.). 
        We pick day with the largest number of total hated.
        Then result2 = allPeople - thatSet. 
        Then if scheduled[day].size()<2 => we do it. 
        If we can't find any day, we return (someDay, None).
        """
        most_hated = -1
        position = -1
        final_result2 = None
        for i in range(1, self.num_of_days+1):
            if len(self.scheduled[i]) < 2:
                # dayHated = union of hated[i], scheduled[i], scheduled[i-1], scheduled[i+1]
                day_hated = set()
                if i in self.hated:
                    day_hated = day_hated.union(self.hated[i])
                if (i in self.scheduled):
                    day_hated = day_hated.union(self.scheduled[i])
                if ((i-1) in self.scheduled):
                    day_hated = day_hated.union(self.scheduled[i-1])
                if ((i+1) in self.scheduled):
                    day_hated = day_hated.union(self.scheduled[i+1])
                if len(day_hated) > most_hated:
                    most_hated = len(day_hated)
                    position = i
                    result = day_hated
                    # result2 = all ppl - result
                    result2 = set(self.people.keys()) - result
                    final_result2 = result2
        return (position, final_result2)

    def get_the_most_hated_and_not_scheduled(self, days):
        """
        For weekends/holidays. The Java code is similar to the above, 
        but uses day+2 in the union instead of day+1. 
        """
        most_hated = -1
        position = -1
        final_result2 = None
        for i in range(1, self.num_of_days+1):
            if i in days and len(self.scheduled[i]) < 2:
                day_hated = set()
                if i in self.hated:
                    day_hated = day_hated.union(self.hated[i])
                if i in self.scheduled:
                    day_hated = day_hated.union(self.scheduled[i])
                if (i-1) in self.scheduled:
                    day_hated = day_hated.union(self.scheduled[i-1])
                if (i+2) in self.scheduled:
                    day_hated = day_hated.union(self.scheduled[i+2])
                if len(day_hated) > most_hated:
                    most_hated = len(day_hated)
                    position = i
                    result = day_hated
                    result2 = set(self.people.keys()) - result
                    final_result2 = result2
        return (position, final_result2)

    def get_the_fewest_scheduled_person(self, day_persons):
        """
        Returns a sorted list of names based on schedule weighting. 
        In Java, we do the adjacency logic, then subtract manualDayDifference *7, etc.
        """
        day = day_persons[0]
        persons = day_persons[1]
        schedule_numbers = {}
        for n in persons:
            schedule_numbers[n] = 0
        # adjacency logic: +7 for each time scheduled, +2 if day +/-2, +1 if day +/-3
        for d, pset in self.scheduled.items():
            for n in pset:
                if n in persons:
                    schedule_numbers[n] += 7
                    if d == (day-2) or d == (day+2):
                        schedule_numbers[n] += 2
                    if d == (day-3) or d == (day+3):
                        schedule_numbers[n] += 1

        # also subtract manualDayDifference * 7
        for n in schedule_numbers.keys():
            schedule_numbers[n] -= (self.people[n].get_manual_day_difference() * 7)

        return self.order_schedule_numbers(schedule_numbers)

    def order_schedule_numbers(self, schedule_numbers):
        """
        Sort ascending by the integer value. Return list of names from min to max.
        """
        # We can do sorted by value
        return sorted(schedule_numbers.keys(), key=lambda k: schedule_numbers[k])

    def find_first_good_for(self, person_name, ordered_persons, day):
        """
        The Java code:
         - We find the first in ordered_persons that is not the same as 'person'
           and that has a 'goodWith()' match for day type combination.
        If none found => error.
        """
        tperson = self.people[person_name]
        t1 = tperson.get_type(day)
        for n in ordered_persons:
            if n != person_name:
                t2 = self.people[n].get_type(day)
                if t2.good_with(t1):
                    # schedule them
                    self.scheduled[day].add(n)
                    return
        # if we never found anything
        if len(ordered_persons)==1 and len(self.scheduled[day])==1:
            # Java says: "I found only one person... for day"
            msg = f"I found only one person ({list(ordered_persons)}) for day {day}"
            raise RuntimeError(msg)
        else:
            msg = f"No second person found for day {day} among {ordered_persons}"
            raise RuntimeError(msg)

    def select_fo(self, names_set, day):
        """
        Replicates selectFo from Java. We must pick which of the two will be FO (IMS1).
        - If one is no-FO, the other is automatically FO
        - If one has day=FO, the other has day=BO, we pick the FO
        - If both want FO => error
        - If both want BO => error
        - else pick the smaller IMS1Value from getIMS1Value
        """
        names_list = list(names_set)
        if len(names_list)!=2:
            if len(names_list)==1:
                # weird but let's just pick that one as FO
                return names_list[0]
            raise RuntimeError(f"select_fo called but we have not exactly 2 names: {names_set}")

        name1 = names_list[0]
        name2 = names_list[1]
        p1 = self.people[name1]
        p2 = self.people[name2]

        # check if any is nofo
        if p1.is_nofo() and (not p2.is_nofo()):
            return name2
        if (not p1.is_nofo()) and p2.is_nofo():
            return name1
        if p1.is_nofo() and p2.is_nofo():
            raise RuntimeError(f"Both no-fo: {name1}, {name2} for day {day}")

        # check if day type is FO or BO
        t1 = p1.get_type(day)
        t2 = p2.get_type(day)
        if t1==Type.BO and t2==Type.FO:
            return name2
        if t1==Type.FO and t2==Type.BO:
            return name1
        if t1==Type.FO and t2==Type.FO:
            raise RuntimeError(f"Both want FO for day {day}: {names_set}")
        if t1==Type.BO and t2==Type.BO:
            raise RuntimeError(f"Both want BO for day {day}: {names_set}")

        # else pick who has smaller IMS1Value
        v1 = self.get_ims1_value(name1)
        v2 = self.get_ims1_value(name2)
        if v1 < v2:
            return name1
        elif v2 < v1:
            return name2
        else:
            # tie => pick random
            return self.random_gen.choice([name1, name2])

    def get_ims1_value(self, name):
        """
        Java logic:
          countAll = how many days total the person has in self.scheduled
          countFo  = how many days the person is FO in self.fo_names
          value = countFo*2 - countAll
        """
        count_all = 0
        for sset in self.scheduled.values():
            if name in sset:
                count_all += 1
        count_fo = 0
        for fo in self.fo_names.values():
            if fo == name:
                count_fo += 1
        value = count_fo*2 - count_all
        return value

    def balance_ims(self):
        """
        The Java code tries to repeatedly reduce the difference in IMS1 coverage among FO people.
        """
        fo_people = [p for p in self.people.values() if not p.is_nofo()]
        for i in range(10):
            # find maxIMS1
            max_val = 0
            max_p = None
            for p in fo_people:
                val = self.get_ims1_value(p.get_name())
                if val > max_val:
                    max_val = val
                    max_p = p
            if max_val > 1:
                # gather all scheduled days for which this max_p is in scheduled and is FO
                # the Java code actually does: 
                #   scheduled days which contain p, and foNames[day] = p
                # Then for each day, if that day type != FO for that person, we see if we can swap
                # It's a bit complicated in Java, let's replicate carefully:
                relevant_days = []
                for day, day_set in self.scheduled.items():
                    if max_p.get_name() in day_set:
                        if self.fo_names.get(day, None) == max_p.get_name():
                            relevant_days.append(day)
                min_val_local = max_val
                minIMS1 = None
                position_for_swap = None
                for day in relevant_days:
                    # if person type != FO => check if there's someone in that day who might become FO
                    if max_p.get_type(day) != Type.FO:
                        # find the other name in that day
                        day_list = list(self.scheduled[day])
                        if len(day_list)==2:
                            if day_list[0] == max_p.get_name():
                                other_name = day_list[1]
                            else:
                                other_name = day_list[0]
                            other_person = self.people[other_name]
                            if (other_person.get_type(day) != Type.BO):
                                # potential to swap
                                v = self.get_ims1_value(other_name)
                                if v < min_val_local:
                                    min_val_local = v
                                    minIMS1 = other_name
                                    position_for_swap = day
                if (max_val - min_val_local) > 1 and minIMS1 is not None and position_for_swap is not None:
                    self.fo_names[position_for_swap] = minIMS1

    def get_scheduled(self):
        return self.scheduled

    def get_fo_names(self):
        return self.fo_names
