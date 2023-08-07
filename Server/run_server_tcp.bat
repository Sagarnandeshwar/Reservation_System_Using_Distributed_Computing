:: Adapted from run_server.sh
:: @author Ye Tong Zhou
:: Usage: ./run_server.bat [<rmi_name>]
@echo off

:: call run_rmi.bat > NUL 2>&1 REM extra dumping just in case

java Server.TCP.TCPResourceManager %1

