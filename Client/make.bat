:: Adapted from Makefile
:: @author Ye Tong Zhou
@echo off

:: all: java.policy compile-client

:: (java.policy) Creating policy file
echo Creating client java policy

:: Change DOS backslash path into URL forward slash
set curdir=%cd%
set curdir=%curdir:\=/%
echo grant codebase "file:/%curdir%/" { > java.policy
echo permission java.security.AllPermission; >> java.policy
echo }; >> java.policy

:: (../Server/RMIInterface.jar) Packaging RMIInterface jar (going in and out of folder)
cd ..\Server\
echo make.bat: changing directory to %cd%
echo Compiling RMI server interface
javac Server\Interface\IResourceManager.java
jar cvf RMIInterface.jar Server\Interface\IResourceManager.class
cd ..\Client\
echo make.bat: changing directory to %cd%

:: (compile-client)
javac -cp ../Server/RMIInterface.jar Client/*.java






