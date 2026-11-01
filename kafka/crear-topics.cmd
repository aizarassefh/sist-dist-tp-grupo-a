@echo off
rem Crea los topics de la Entrega 3 con la cantidad de particiones documentada.
rem Si no se crean a mano, el backend los crea solos al publicar, pero con el
rem valor por defecto de particiones del broker.
setlocal
set KAFKA=%~dp0kafka_2.13-3.7.0
set CP=%KAFKA%\libs\*
set LOG=-Dlog4j.configuration=file:%KAFKA%\config\tools-log4j.properties
java -cp "%CP%" %LOG% org.apache.kafka.tools.TopicCommand --bootstrap-server localhost:9092 --create --if-not-exists --topic eventos.novedades --partitions 3 --replication-factor 1
java -cp "%CP%" %LOG% org.apache.kafka.tools.TopicCommand --bootstrap-server localhost:9092 --create --if-not-exists --topic eventos.novedades.DLT --partitions 1 --replication-factor 1
java -cp "%CP%" %LOG% org.apache.kafka.tools.TopicCommand --bootstrap-server localhost:9092 --describe --topic eventos.novedades
