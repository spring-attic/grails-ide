#!/bin/bash
mvn -Pe37\
 -Dp2.qualifier=BORK\
 -Dmaven.test.skip=true\
 clean\
 deploy\

