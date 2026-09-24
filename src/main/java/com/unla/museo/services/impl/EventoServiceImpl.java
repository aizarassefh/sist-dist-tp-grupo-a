package com.unla.museo.services.impl;

import com.unla.museo.entities.Evento;
import com.unla.museo.repositories.EventoRepository;
import com.unla.museo.services.EventoService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventoServiceImpl implements EventoService {
    @Autowired
    private EventoRepository eventoRepository;

    public List<Evento> listarTodos() {
        return eventoRepository.findAll();
    }

    public Evento crear(Evento evento) {
        return eventoRepository.save(evento);
    }

    public byte[] exportarEventosAExcel() throws IOException {
        List<Evento> eventos = eventoRepository.findAll();
        
        // Agrupar eventos por su tipo (ej. "Visitas Guiadas", "Talleres")
        Map<String, List<Evento>> eventosAgrupados = eventos.stream()
            .collect(Collectors.groupingBy(Evento::getTipoEvento));

        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Iterar sobre los grupos y crear una hoja por cada tipo de evento
            for (Map.Entry<String, List<Evento>> entry : eventosAgrupados.entrySet()) {
                String tipo = entry.getKey();
                List<Evento> eventosDeTipo = entry.getValue();
                
                Sheet sheet = workbook.createSheet(tipo);
                
                // Cabeceras
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Título");
                headerRow.createCell(1).setCellValue("Fecha");
                headerRow.createCell(2).setCellValue("Inscriptos");
                headerRow.createCell(3).setCellValue("Cupo Máximo");

                // Datos
                int rowIdx = 1;
                for (Evento evt : eventosDeTipo) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(evt.getTitulo());
                    if (evt.getFechaHora() != null) {
                        row.createCell(1).setCellValue(evt.getFechaHora().toString());
                    }
                    row.createCell(2).setCellValue(evt.getInscriptos() != null ? evt.getInscriptos() : 0);
                    row.createCell(3).setCellValue(evt.getCupoMaximo() != null ? evt.getCupoMaximo() : 0);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}