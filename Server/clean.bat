:: Adapted from Makefile
:: @author Ye Tong Zhou
@echo off

:: Deletes all server files
del /f Server\Interface\*.class Server\Common\*.class Server\RMI\*.class Server\TCP\*.class
del /f RMIInterface.jar
del /f java.policy
