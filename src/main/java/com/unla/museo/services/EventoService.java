package com.unla.museo.services;

import com.unla.museo.entities.Evento;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public interface EventoService {
    List<Evento> listarTodos();
    Evento crear(Evento evento);
    byte[] exportarEventosAExcel() throws IOException ;

}