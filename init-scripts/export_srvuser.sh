#!/usr/bin/env bash

DIR=/var/run/secrets/nais.io/service_user/

echo "Attempting to export serviceuser from $DIR if it exists"

if test -d $DIR;
then
    for FILE in `ls $DIR`
    do
       UPPERCASE=$(echo "$FILE" | tr '[:lower:]' '[:upper:]')
       echo "- exporting $UPPERCASE"
       export $UPPERCASE=`cat $DIR/$FILE`
    done
fi