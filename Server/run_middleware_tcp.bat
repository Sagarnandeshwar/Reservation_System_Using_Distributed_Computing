:: Adapted from run_middleware.sh
:: @author Ye Tong Zhou
@echo off

:: call run_rmi.bat > NUL 2>&1 REM extra dumping just in case

:: echo "Edit file run_middleware.bat to include instructions for launching the middleware"
:: echo '  %1 - hostname of Flights'
:: echo '  %2 - hostname of Cars'
:: echo '  %3 - hostname of Rooms'

java Server.TCP.TCPMiddleware %1 %2 %3

