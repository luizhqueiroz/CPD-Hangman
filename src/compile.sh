#!/bin/bash

# Clear the console
clear

# Remove existing class files
rm -f *.class

# Compile Java files with classpath
javac -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" *.java

#java -Djavax.net.ssl.keyStore=serverkeystore.jks     -Djavax.net.ssl.keyStorePassword=password     -Djavax.net.ssl.trustStore=clienttruststore.jks     -Djavax.net.ssl.trustStorePassword=password  -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" Server 1234 0 database.json

#java -Djavax.net.ssl.keyStore=serverkeystore.jks     -Djavax.net.ssl.keyStorePassword=password     -Djavax.net.ssl.trustStore=clienttruststore.jks     -Djavax.net.ssl.trustStorePassword=password  -cp ".:./lib/gson-2.10.1.jar:./lib/jbcrypt-0.4.jar" Client 1234 0