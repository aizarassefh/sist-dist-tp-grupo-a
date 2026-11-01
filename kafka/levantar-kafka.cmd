@echo off
rem Levanta Kafka 3.7.0 en modo KRaft (un nodo, sin Zookeeper ni Docker) en localhost:9092.
rem
rem Requisito: descomprimir kafka_2.13-3.7.0.tgz (https://archive.apache.org/dist/kafka/3.7.0/)
rem dentro de esta carpeta, de modo que quede kafka\kafka_2.13-3.7.0\. Solo hace falta Java.
rem
rem Se llama a Java directo: los .bat de bin\windows arman un classpath que supera el
rem largo maximo de linea de cmd ("La linea escrita es demasiado larga").
rem enabledelayedexpansion: dentro del if, %CLUSTER_ID% se expandiria vacio.
setlocal enabledelayedexpansion
rem server.properties usa log.dirs=datos, relativo a esta carpeta.
cd /d "%~dp0"
set KAFKA=%~dp0kafka_2.13-3.7.0
set CP=%KAFKA%\libs\*
set CONFIG=%~dp0server.properties
if not exist "%KAFKA%\libs" (
  echo No encuentro %KAFKA%. Descomprimi kafka_2.13-3.7.0.tgz en esta carpeta.
  exit /b 1
)
if not exist "%~dp0datos\meta.properties" (
  for /f %%i in ('java -cp "%CP%" kafka.tools.StorageTool random-uuid') do set CLUSTER_ID=%%i
  java -cp "%CP%" -Dlog4j.configuration=file:%KAFKA%\config\tools-log4j.properties kafka.tools.StorageTool format -t !CLUSTER_ID! -c "%CONFIG%"
)
java -Xmx512m -cp "%CP%" -Dlog4j.configuration=file:%KAFKA%\config\log4j.properties -Dkafka.logs.dir=%~dp0logs kafka.Kafka "%CONFIG%"
