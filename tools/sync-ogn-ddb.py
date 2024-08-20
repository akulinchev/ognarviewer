#!/usr/bin/python3

import os

from urllib.request import urlopen


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


with urlopen("https://ddb.glidernet.org/download/") as res:
    with open(os.path.join(BASE_DIR, "app", "src", "main", "registrations", "ogn.csv"), "w") as f:
        f.write("#{}\n".format(res.headers.get("Date")))
        f.write(res.read().decode("utf-8"))
