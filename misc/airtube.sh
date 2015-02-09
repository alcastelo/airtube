#!/bin/sh

[ -e misc/logging.properties ] || cp misc/logging.properties.default misc/logging.properties

java -Djava.util.logging.config.file=misc/logging.properties -jar package/airtube-1.0/airtube-java.jar $@ 2>&1
