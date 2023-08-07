:: Adapted from run_client.sh
:: @author Ye Tong Zhou
:: Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]
@echo off

:: java -Djava.security.policy=java.policy -cp ..\Server\RMIInterface.jar:. Client.RMIClient %1 %2
java -Djava.security.policy=java.policy -cp "..\Server\RMIInterface.jar;." Client.RMIClient %1 %2

:: THIS TOOK ME 2 HOURS TO FIGURE OUT THE JAVA COMMAND ON WINDOWS SEPARATES CLASSPATH ARGS WITH SEMICOLONS WHEREAS UNIX
:: USES COLONS FPEHGFUIE  OHGUIGHG4WREUI ;OJGAW 4RJI GHEHUYIR GFVPEYURGVPY GFYHLI UKBN I AM GOING INSANE

