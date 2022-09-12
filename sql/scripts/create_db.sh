#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
psql -p $PGPORT $DB_NAME < $DIR/../src/create_tables.sql
psql -p $PGPORT $DB_NAME < $DIR/../src/create_index.sql
psql -p $PGPORT $DB_NAME < $DIR/../src/load_data.sql
psql -p $PGPORT $DB_NAME < $DIR/../src/create_trigger.sql