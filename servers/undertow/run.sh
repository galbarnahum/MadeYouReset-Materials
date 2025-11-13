#!/bin/bash
mvn clean package
mvn exec:java -Dexec.mainClass="com.example.HttpsServer"

