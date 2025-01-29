# scheduler_python/people/people.py

import sys
import os
import re
import codecs
from ..people.person import Person
from ..people.type import Type
from ..common.globals import Globals
from ..util.key_value_store import KeyValueStore

class People:
    """
    Python version of the Java People class.
    It reads lines from config.csv, interprets each line (with same logic).
    """
    def __init__(self):
        self.people = {}
        self.hated = {}
        for i in range(32):
            self.hated[i] = set()
        g = Globals()
        config_file = g.get_config_file()

        # In Java, it reads the entire file into 'content' and splits lines.
        # We'll do similarly in Python:
        with codecs.open(config_file, "r", encoding="utf-8") as f:
            lines = f.read().splitlines()

        # We get all distinct names from KeyValueStore in Java as well
        # because People tries to see if the name is recognized from DB or "newperson".
        kv = KeyValueStore()
        known_names = kv.get_names()
        kv.close()
        keywords = [
            "nofo","hend","hweek","hmon","htue","hwen","hthu","hfri","wtue"
        ]

        new_person_must_be_tagged = False
        new_person_obj = None

        # The Java code logic is: for each line, split on spaces, parse them.
        for line in lines:
            line = line.strip()
            if not line:
                continue
            expressions = line.split()
            person = None
            # We replicate the logic from People.java
            for expr in expressions:
                expr = expr.strip()
                if not expr:
                    continue

                # If person is still None, we treat expr as the name (or 'newperson'):
                if person is None:
                    if new_person_obj is not None:
                        # Means we got a newPerson line
                        if expr.lower() == "newperson":
                            # finalize new_person
                            person = new_person_obj
                            self.people[person.get_name()] = person
                            new_person_obj = None
                        else:
                            raise RuntimeError("New person declared but 'newperson' keyword not found.")
                    else:
                        # Check if name is known or brand new:
                        replaced = expr.replace("_", " ")
                        if replaced in known_names:
                            # Just create it as a known Person
                            person = Person(replaced)
                            self.people[replaced] = person
                        else:
                            # The Java code logic: if not found among known names, 
                            # it tries to see if the next token is "newperson"? 
                            # We replicate that approach: let's store it in new_person_obj
                            new_person_obj = Person(replaced)
                else:
                    # if expr is in the keywords:
                    if expr.lower() in keywords:
                        if expr.lower() == "nofo":
                            for d in person.types.keys():
                                person.set_type(d, Type.BO)
                            person.set_nofo(True)
                        elif expr.lower() == "hend":
                            person.set_hates_weekends(True)
                        elif expr.lower() == "hweek":
                            person.set_hates_weekdays(True)
                        elif expr.lower() == "hmon":
                            person.set_hates_mondays(True)
                        elif expr.lower() == "htue":
                            person.set_hates_tuesdays(True)
                        elif expr.lower() == "hwen":
                            person.set_hates_wednesdays(True)
                        elif expr.lower() == "hthu":
                            person.set_hates_thursdays(True)
                        elif expr.lower() == "hfri":
                            person.set_hates_fridays(True)
                        elif expr.lower() == "wtue":
                            person.set_wanted_tuesdays(True)
                    else:
                        # not a keyword, parse further
                        c = expr[0]  # first char
                        remaining = expr[1:]
                        # parse days
                        days = []
                        if "-" in remaining:
                            splitted = remaining.split("-")
                            start = int(splitted[0])
                            end = int(splitted[1])
                            for dd in range(start, end+1):
                                days.append(dd)
                        else:
                            days.append(int(remaining))

                        if c == 'u':
                            # set numOfWantedHolidays
                            person.set_num_of_wanted_holidays(days[0])
                        elif c == 'p':
                            # set numOfWantedFridays
                            person.set_num_of_wanted_fridays(days[0])
                        elif c == 's':
                            # set numOfWantedSaturdays
                            person.set_num_of_wanted_saturdays(days[0])
                        elif c == 'v':
                            # set numOfWantedSundays
                            person.set_num_of_wanted_sundays(days[0])
                        elif c == 'w':
                            # wanted days
                            for d in days:
                                person.get_wanted_days().add(d)
                                if d in person.get_hated_days():
                                    raise RuntimeError(f"{person.get_name()} wants and hates day {d} at once")
                        elif c == 'h':
                            # hated days
                            for d in days:
                                person.get_hated_days().append(d)
                                if d in person.get_wanted_days():
                                    raise RuntimeError(f"{person.get_name()} wants and hates day {d} at once")
                        elif c == 'f':
                            # set day type FO
                            for d in days:
                                person.set_type(d, Type.FO)
                        elif c == 'b':
                            # set day type BO
                            for d in days:
                                person.set_type(d, Type.BO)
                        elif c == '+':
                            # manualDayDifference
                            person.set_manual_day_difference(days[0])
                        elif c == '-':
                            # negative manualDayDifference
                            person.set_manual_day_difference(-days[0])
                        else:
                            pass
                            # The Java code doesn't handle other letters differently, so we ignore.

        # If at the end we still have new_person_obj not None, then user forgot "newperson" keyword
        # But let's just ignore or raise. The original Java logic would raise.
        if new_person_obj is not None:
            raise RuntimeError("Found a name that is not in DB but did not get 'newperson' tag in line, can't finalize person")

    def get_people(self):
        return self.people
