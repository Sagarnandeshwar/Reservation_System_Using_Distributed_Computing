:: Adapted from run_client.sh
:: @author Ye Tong Zhou
:: Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]
@echo off

:: java -Djava.security.policy=java.policy -cp ..\Server\RMIInterface.jar:. Client.RMIClient %1 %2
java -cp "..\Server\RMIInterface.jar;." Client.TCPClient %1 %2
