#!/usr/bin/python3

import os
import subprocess

from email.utils import parsedate_to_datetime
from urllib.request import urlopen

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_FILE = os.path.join(BASE_DIR, "app", "src", "main", "registrations", "ogn.csv")

with urlopen("https://ddb.glidernet.org/download/") as resp:
    with open(CSV_FILE, "w") as f:
        http_date = resp.headers.get("Date")
        f.write("#{}\n".format(http_date))
        f.write("#Exported from ddb.glidernet.org\n")
        f.write("#License: ODC-BY (https://opendatacommons.org/licenses/by/summary/)\n")
        f.write("\n".join(resp.read().decode("utf-8").splitlines()))

subprocess.run(["git", "checkout", "-b", parsedate_to_datetime(http_date).strftime("ddb-sync-%Y%m%d-%H%M%S")], cwd=BASE_DIR)
subprocess.run(["git", "add", CSV_FILE], cwd=BASE_DIR)
subprocess.run(["git", "commit", "-m", "Sync OGN DDB to " + http_date], cwd=BASE_DIR)
