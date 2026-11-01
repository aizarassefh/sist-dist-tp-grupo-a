@echo off
rem Publica en eventos.novedades un mensaje que ningun consumidor puede procesar
rem (le falta idMensaje), para demostrar el tratamiento de errores: el consumidor
rem lo rechaza sin reintentar, va a eventos.novedades.DLT y aparece en la pantalla
rem de Mensajeria como fallido.
setlocal
set KAFKA=%~dp0kafka_2.13-3.7.0
set CP=%KAFKA%\libs\*
set LOG=-Dlog4j.configuration=file:%KAFKA%\config\tools-log4j.properties
echo {"versionEsquema":1,"tipo":"ALTA","idEvento":999,"tituloEvento":"Mensaje de prueba sin idMensaje"} | java -cp "%CP%" %LOG% kafka.tools.ConsoleProducer --bootstrap-server localhost:9092 --topic eventos.novedades
echo Mensaje invalido publicado en eventos.novedades.
