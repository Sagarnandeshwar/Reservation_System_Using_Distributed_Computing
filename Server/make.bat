:: Adapted from Makefile
:: @author Ye Tong Zhou
@echo off

::Creating policy file
echo Creating server java policy

:: Change DOS backslash path into URL forward slash
set curdir=%cd%
set curdir=%curdir:\=/%
echo grant codebase "file:/%curdir%/" { > java.policy
echo permission java.security.AllPermission; >> java.policy
echo }; >> java.policy

:: Compile all classes
javac -target 11 -source 11 Server\RMI\*.java Server\Interface\IResourceManager.java Server\Common\*.java Server\TCP\*.java

