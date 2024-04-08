#!/usr/bin/env bash

cat $CONTRACT_FILE | sed 's/openapi: \"3.1.0\"/openapi: \"3.0.3\"/g'
