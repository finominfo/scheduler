# scheduler_python/people/person.py

from collections import defaultdict
from ..people.type import Type
import sys
import math
import threading
import random

class Person:
    """
    Python equivalent of the Java Person class.
    """
    def __init__(self, name):
        self.name = name
        self.hated_days = []
        self.wanted_days = set()
        # We'll store day -> Type
        self.types = {}
        # We'll store day from -5 to 35 inclusive (like Java).
        for i in range(-5, 36):
            self.types[i] = Type.FO_AND_BO
        self.num_of_scheduled = 0
        self.manual_day_difference = 0

        # The 4 new fields that we have getters/setters for in Java:
        self.num_of_wanted_holidays = 0
        self.num_of_wanted_fridays = 0
        self.num_of_wanted_saturdays = 0
        self.num_of_wanted_sundays = 0

        # boolean fields:
        self.nofo = False
        self.hates_weekends = False
        self.hates_weekdays = False
        self.hates_mondays = False
        self.hates_tuesdays = False
        self.wanted_tuesdays = False
        self.hates_wednesdays = False
        self.hates_thursdays = False
        self.hates_fridays = False

    def get_name(self):
        return self.name

    def get_type(self, day):
        return self.types[day]

    def set_type(self, day, t):
        self.types[day] = t

    # Mirror the Java isNofo, setNofo, etc. in Python property style:
    def is_nofo(self):
        return self.nofo

    def set_nofo(self, val):
        self.nofo = val

    def get_manual_day_difference(self):
        return self.manual_day_difference

    def set_manual_day_difference(self, val):
        self.manual_day_difference = val

    def increment_scheduled(self):
        self.num_of_scheduled += 1

    def get_num_of_scheduled(self):
        return self.num_of_scheduled

    # We keep the same approach for the rest:

    def is_hates_weekends(self):
        return self.hates_weekends

    def set_hates_weekends(self, b):
        self.hates_weekends = b

    def is_hates_weekdays(self):
        return self.hates_weekdays

    def set_hates_weekdays(self, b):
        self.hates_weekdays = b

    def is_hates_mondays(self):
        return self.hates_mondays

    def set_hates_mondays(self, b):
        self.hates_mondays = b

    def is_hates_tuesdays(self):
        return self.hates_tuesdays

    def set_hates_tuesdays(self, b):
        self.hates_tuesdays = b

    def is_wanted_tuesdays(self):
        return self.wanted_tuesdays

    def set_wanted_tuesdays(self, b):
        self.wanted_tuesdays = b

    def is_hates_wednesdays(self):
        return self.hates_wednesdays

    def set_hates_wednesdays(self, b):
        self.hates_wednesdays = b

    def is_hates_thursdays(self):
        return self.hates_thursdays

    def set_hates_thursdays(self, b):
        self.hates_thursdays = b

    def is_hates_fridays(self):
        return self.hates_fridays

    def set_hates_fridays(self, b):
        self.hates_fridays = b

    def get_hated_days(self):
        return self.hated_days

    def get_wanted_days(self):
        return self.wanted_days

    # The 4 numeric "desired" fields:
    def get_num_of_wanted_holidays(self):
        return self.num_of_wanted_holidays

    def set_num_of_wanted_holidays(self, x):
        self.num_of_wanted_holidays = x

    def get_num_of_wanted_fridays(self):
        return self.num_of_wanted_fridays

    def set_num_of_wanted_fridays(self, x):
        self.num_of_wanted_fridays = x

    def get_num_of_wanted_saturdays(self):
        return self.num_of_wanted_saturdays

    def set_num_of_wanted_saturdays(self, x):
        self.num_of_wanted_saturdays = x

    def get_num_of_wanted_sundays(self):
        return self.num_of_wanted_sundays

    def set_num_of_wanted_sundays(self, x):
        self.num_of_wanted_sundays = x
