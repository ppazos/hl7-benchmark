@echo off
java -cp hl7-benchmark.jar;%GROOVY_HOME%\embeddable\groovy-all-2.4.3.jar;lib\commons-cli-1.2.jar;lib\commons-collections-3.2.1.jar;lib\commons-logging-1.2.jar;lib\http-builder-0.5.2.jar;lib\httpclient-4.4.1.jar;lib\httpcore-4.4.1.jar;lib\json-lib-2.3-jdk15.jar;lib\xml-resolver-1.2.jar com.cabolabs.hl7benchmark.Main %1 %2 %3 %4 %5 %6 %7 %8 %9
