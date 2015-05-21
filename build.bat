del bin\*.* /Q
groovyc -cp .;lib\groovy-all-2.4.3.jar;lib\commons-cli-1.2.jar;lib\commons-collections-3.2.1.jar;lib\commons-logging-1.2.jar;lib\http-builder-0.5.2.jar;lib\httpclient-4.4.1.jar;lib\httpcore-4.4.1.jar;lib\json-lib-2.3-jdk15.jar;lib\xml-resolver-1.2.jar -d bin -j -Jsourcepath src src/com/cabolabs/hl7benchmark/Main.groovy
