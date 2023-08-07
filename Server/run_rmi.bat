:: Adapted from run_rmi.sh
:: @author Ye Tong Zhou
@echo off

:: Start in background, dump output
start /B rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 7050 > NUL 2>&1

