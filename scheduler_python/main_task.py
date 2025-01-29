# scheduler_python/main_task.py

import sys
import os
import datetime
import math
import codecs
import random
from .people.people import People
from .scheduler.scheduler import Scheduler
from .util.excel_exporter import ExcelExporter
from .people.type import Type
from .common.globals import Globals
import calendar
import pathlib

class MainTask:
    """
    This is the Python counterpart to the Java MainTask class.
    """
    def __init__(self, args):
        self.args = args
        self.local_date = None

    def make(self):
        """
        The Java code:
         - parse date from args if exists (yymm format),
           else localDate = now + 1 month
         - read people
         - run scheduler
         - writeMonth()
         - excelExporter.writeMonthToExcel()
        """
        if self.args and len(self.args)==1:
            # parse something like "2308" => year=2023, month=08
            arg0 = self.args[0]
            yy = int(arg0[:2])
            mm = int(arg0[2:])
            year = 2000+yy
            dt = datetime.date(year, mm, 1)
            self.local_date = dt
        else:
            # default is next month
            now = datetime.date.today()
            year = now.year
            month = now.month
            # add 1 month
            # naive approach:
            new_month = month+1
            new_year = year
            if new_month>12:
                new_month = 1
                new_year +=1
            self.local_date = datetime.date(new_year, new_month, 1)

        # create People
        people = People()
        # create scheduler
        sched = Scheduler(people.get_people(), self.local_date)
        # writeMonth
        self.write_month(sched, people)
        # excel exporter
        exp = ExcelExporter(sched, people, self.local_date)
        exp.write_month_to_excel()

        # we skip the part about reading older months .csv to sum up,
        # but let's replicate some of the logic from Java if needed
        # or we do partial. We'll just skip the older months code, 
        # or replicate a small portion.

    def write_month(self, scheduler, people):
        """
        Mirroring the Java code that writes a .csv with w/f/b lines, etc.
        """
        to_txt_file = []
        # build lines about each day:
        for day in sorted(scheduler.get_scheduled().keys()):
            if day>=1 and day<=scheduler.get_num_of_days():
                st = scheduler.get_scheduled()[day]
                if len(st)==2:
                    arr = list(st)
                    # check which is FO 
                    p1type = people.get(arr[0]).get_type(day)
                    p2type = people.get(arr[1]).get_type(day)
                    if Type.is_first_fo(p1type, p2type):
                        line = f"{day} -> {arr[0]} - {arr[1]}"
                    else:
                        line = f"{day} -> {arr[1]} - {arr[0]}"
                    if day in scheduler.get_holidays():
                        line += " - Official Holiday"
                    to_txt_file.append(line)

        # increment scheduled counters
        for day, st in scheduler.get_scheduled().items():
            for nm in st:
                people.get(nm).increment_scheduled()
                people.get(nm).wanted_days.add(day)

        # now each person's total
        for name, pr in sorted(people.get_people().items()):
            to_txt_file.append(f"{name} - {pr.get_num_of_scheduled()}")

        # build a separate .csv content
        to_file = []
        for name, pr in sorted(people.get_people().items()):
            line = [name.replace(" ", "_")]
            # first we list "wX" for each day
            wdays = sorted(pr.get_wanted_days())
            for d in wdays:
                line.append(f"w{d}")
            # then for each day we show f or b
            # but only if the person is scheduled
            for d in wdays:
                # is FO or BO?
                if scheduler.get_fo_names().get(d,"") == name:
                    line.append(f"f{d}")
                else:
                    line.append(f"b{d}")
            to_file.append(" ".join(line))

        # write to "schedule-year-month.csv"
        csv_filename = f"schedule-{self.local_date.year}-{self.local_date.month}.csv"
        with codecs.open(csv_filename, "w", "utf-8") as f:
            f.write("\n".join(to_file))

        # the .txt file if you want:
        txt_filename = f"schedule-{self.local_date.year}-{self.local_date.month}.txt"
        # with open(txt_filename,"w",encoding="utf-8") as f:
        #     f.write("\n".join(to_txt_file))
