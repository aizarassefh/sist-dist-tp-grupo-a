package com.unla.museo.reportes.util;

import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.dto.FilaReporteEventoDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Arma el .xlsx del reporte de asistencia a partir de las mismas filas que
 * usa el reporte GraphQL (EventoService.obtenerFilasParaReporte), así los dos
 * nunca dan números distintos.
 */
@Service
public class ExportadorExcelReporte {

    private static final String[] ENCABEZADOS = {"Fecha", "Título", "Curador", "Inscriptos", "Cupo Máximo", "% Ocupación"};

    public byte[] exportar(List<FilaReporteEventoDTO> filas, TipoEvento tipoFiltro) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            CellStyle estiloEncabezado = crearEstiloEncabezado(libro);
            CellStyle estiloFecha = crearEstiloFecha(libro);
            CellStyle estiloPorcentaje = crearEstiloPorcentaje(libro);

            List<TipoEvento> tipos = tipoFiltro != null ? List.of(tipoFiltro) : List.of(TipoEvento.values());
            for (TipoEvento tipo : tipos) {
                Sheet hoja = libro.createSheet(nombreHoja(tipo));
                escribirEncabezado(hoja, estiloEncabezado);
                List<FilaReporteEventoDTO> filasDelTipo = filas.stream().filter(f -> f.tipo() == tipo).toList();
                escribirFilas(hoja, filasDelTipo, estiloFecha, estiloPorcentaje);
                for (int columna = 0; columna < ENCABEZADOS.length; columna++) {
                    hoja.autoSizeColumn(columna);
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            // ByteArrayOutputStream/XSSFWorkbook en memoria no fallan por I/O real;
            // si pasa igual, no hay forma de recuperarse acá.
            throw new UncheckedIOException(ex);
        }
    }

    private String nombreHoja(TipoEvento tipo) {
        return switch (tipo) {
            case VISITA_GUIADA -> "VISITAS GUIADAS";
            case TALLER -> "TALLERES";
            case CHARLA -> "CHARLAS";
        };
    }

    private void escribirEncabezado(Sheet hoja, CellStyle estilo) {
        Row fila = hoja.createRow(0);
        for (int columna = 0; columna < ENCABEZADOS.length; columna++) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(ENCABEZADOS[columna]);
            celda.setCellStyle(estilo);
        }
    }

    private void escribirFilas(Sheet hoja, List<FilaReporteEventoDTO> filas, CellStyle estiloFecha,
                                CellStyle estiloPorcentaje) {
        int numeroDeFila = 1;
        for (FilaReporteEventoDTO fila : filas) {
            Row filaExcel = hoja.createRow(numeroDeFila++);

            Cell fecha = filaExcel.createCell(0);
            fecha.setCellValue(fila.fechaHora());
            fecha.setCellStyle(estiloFecha);

            filaExcel.createCell(1).setCellValue(fila.titulo());
            filaExcel.createCell(2).setCellValue(fila.curador().nombre());
            filaExcel.createCell(3).setCellValue(fila.cantidadInscriptos());
            filaExcel.createCell(4).setCellValue(fila.cupoMaximo());

            Cell ocupacion = filaExcel.createCell(5);
            ocupacion.setCellValue((double) fila.cantidadInscriptos() / fila.cupoMaximo());
            ocupacion.setCellStyle(estiloPorcentaje);
        }
    }

    private CellStyle crearEstiloEncabezado(XSSFWorkbook libro) {
        Font negrita = libro.createFont();
        negrita.setBold(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(negrita);
        return estilo;
    }

    private CellStyle crearEstiloFecha(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return estilo;
    }

    private CellStyle crearEstiloPorcentaje(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("0.0%"));
        return estilo;
    }
}
