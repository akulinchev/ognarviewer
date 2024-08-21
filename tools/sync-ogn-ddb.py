#!/usr/bin/python3

import os

from urllib.request import urlopen

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

with urlopen("https://ddb.glidernet.org/download/") as resp:
    with open(os.path.join(BASE_DIR, "app", "src", "main", "registrations", "ogn.csv"), "w") as f:
        f.write("#{}\n".format(resp.headers.get("Date")))
        f.write("#Exported from ddb.glidernet.org\n")
        f.write("#License: ODC-BY (https://opendatacommons.org/licenses/by/summary/)\n")
        f.write("\n".join(resp.read().decode("utf-8").splitlines()))
