package com.unla.museo.controllers;

import com.unla.museo.constants.Roles;
import com.unla.museo.controllers.annotations.RequiresRoles;
import com.unla.museo.entities.Evento;
import com.unla.museo.services.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<Evento>> listar() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Evento> crear(@RequestBody Evento evento) {
        return new ResponseEntity<>(eventoService.crear(evento), HttpStatus.CREATED);
    }
    @RequiresRoles({Roles.ADMIN,Roles.CURADOR})
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarExcel() {
        try {
            byte[] file = eventoService.exportarEventosAExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_eventos.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}