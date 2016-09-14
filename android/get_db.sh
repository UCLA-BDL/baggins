#!/bin/sh

RUN_AS=edu.ucla.cs.baggins
DB_NAME=daycare_db
DEST_DIR=/sdcard/baggins

adb shell "run-as $RUN_AS cp /data/data/edu.ucla.cs.baggins/databases/$DB_NAME $DEST_DIR"

adb pull $DEST_DIR/$DB_NAME .
sqliteman $DB_NAME
