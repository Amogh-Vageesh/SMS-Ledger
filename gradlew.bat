@echo off  
set DIRNAME=%%~dp0  
set APP_HOME=%%DIRNAME%%  
if defined JAVA_HOME (set JAVA_EXE=%%JAVA_HOME%%\bin\java.exe) else (set JAVA_EXE=java.exe)  
set CLASSPATH=%%APP_HOME%%\gradle\wrapper\gradle-wrapper.jar  
"%%JAVA_EXE%%" %%JAVA_OPTS%% %%GRADLE_OPTS%% -classpath "%%CLASSPATH%%" org.gradle.wrapper.GradleWrapperMain %%* 
