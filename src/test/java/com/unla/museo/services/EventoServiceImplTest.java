package com.unla.museo.services;

import com.unla.museo.entities.Evento;
import com.unla.museo.repositories.EventoRepository;
import com.unla.museo.services.impl.EventoServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {

    @Mock private EventoRepository eventoRepository;
    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl();
        ReflectionTestUtils.setField(service, "eventoRepository", eventoRepository);
    }

    @Test
    void listarTodosDelegatesToRepository() {
        List<Evento> events = List.of(event("Visitas"));
        when(eventoRepository.findAll()).thenReturn(events);

        assertSame(events, service.listarTodos());
        verify(eventoRepository).findAll();
    }

    @Test
    void crearDelegatesToRepository() {
        Evento event = event("Talleres");
        when(eventoRepository.save(event)).thenReturn(event);

        assertSame(event, service.crear(event));
        verify(eventoRepository).save(event);
    }

    @Test
    void exportGeneratesWorkbookGroupedByType() throws Exception {
        when(eventoRepository.findAll()).thenReturn(List.of(event("Visitas"), event("Talleres")));

        byte[] result = service.exportarEventosAExcel();

        assertTrue(result.length > 0);
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("Visitas"));
            assertNotNull(workbook.getSheet("Talleres"));
            assertEquals("Título", workbook.getSheet("Visitas").getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportWithNoEventsGeneratesEmptyWorkbook() throws Exception {
        when(eventoRepository.findAll()).thenReturn(List.of());

        byte[] result = service.exportarEventosAExcel();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertEquals(0, workbook.getNumberOfSheets());
        }
    }

    private Evento event(String type) {
        Evento event = new Evento();
        event.setTitulo("Evento de prueba");
        event.setTipoEvento(type);
        event.setInscriptos(5);
        event.setCupoMaximo(20);
        event.setFechaHora(LocalDateTime.of(2026, 1, 1, 10, 0));
        return event;
    }
}
