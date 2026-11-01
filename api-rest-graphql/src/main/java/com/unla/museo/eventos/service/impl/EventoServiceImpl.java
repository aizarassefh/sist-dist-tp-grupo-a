package com.unla.museo.eventos.service.impl;

import com.unla.museo.comun.errores.ConflictoException;
import com.unla.museo.comun.errores.RecursoInexistenteException;
import com.unla.museo.comun.errores.SolicitudInvalidaException;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.entity.util.ConteoPorEvento;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.eventos.service.EventoService;
import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.eventos.dto.EventoDTO;
import com.unla.museo.eventos.dto.EventoRequest;
import com.unla.museo.eventos.dto.FilaReporteEventoDTO;
import com.unla.museo.eventos.dto.PaginaEventosDTO;
import com.unla.museo.eventos.dto.PersonaDTO;
import com.unla.museo.seguridad.service.UsuarioActualService;
import com.unla.museo.novedades.NovedadPublisher;
import com.unla.museo.novedades.NovedadTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class EventoServiceImpl implements EventoService {

    private static final int TAMANIO_PAGINA_DEFECTO = 10;
    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioActualService usuarioActualService;
    private final Clock clock;
    private final NovedadPublisher novedadPublisher;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             InscripcionRepository inscripcionRepository,
                             UsuarioActualService usuarioActualService,
                             Clock clock,
                             NovedadPublisher novedadPublisher) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioActualService = usuarioActualService;
        this.clock = clock;
        this.novedadPublisher = novedadPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaEventosDTO listar(LocalDate desde, LocalDate hasta, TipoEvento tipo, Long curadorId,
                                   EstadoEvento estado, Integer pagina, Integer tamanio, Authentication authentication) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        int paginaEfectiva = pagina != null ? pagina : 0;
        int tamanioEfectivo = tamanio != null ? tamanio : TAMANIO_PAGINA_DEFECTO;
        if (paginaEfectiva < 0) {
            throw new SolicitudInvalidaException("La página no puede ser negativa");
        }
        if (tamanioEfectivo < 1 || tamanioEfectivo > TAMANIO_PAGINA_MAXIMO) {
            throw new SolicitudInvalidaException("El tamaño de página debe estar entre 1 y " + TAMANIO_PAGINA_MAXIMO);
        }
        EstadoEvento estadoEfectivo = estado != null ? estado : EstadoEvento.TODOS;
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaExclusivo = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        LocalDateTime ahora = LocalDateTime.now(clock);

        boolean filtrarPasados = estadoEfectivo == EstadoEvento.PASADOS;
        boolean filtrarFuturos = estadoEfectivo == EstadoEvento.FUTUROS;
        Pageable pageable = PageRequest.of(paginaEfectiva, tamanioEfectivo,
                Sort.by(Sort.Order.asc("fechaHora"), Sort.Order.asc("id")));
        Page<EventoEntity> paginaDeEventos = eventoRepository.buscar(
                desdeDateTime, hastaExclusivo, tipo, curadorId, filtrarPasados, filtrarFuturos, ahora, pageable);

        List<EventoEntity> eventoEntities = paginaDeEventos.getContent();
        List<Long> ids = eventoEntities.stream().map(EventoEntity::getId).toList();
        Map<Long, Long> conteos = contarInscriptosPorEvento(ids);
        Set<Long> inscriptoDelUsuario = idsInscriptoDelUsuarioActual(ids, authentication);

        List<EventoDTO> items = eventoEntities.stream()
                .map(evento -> new EventoDTO(
                        evento.getId(), evento.getTitulo(), evento.getDescripcion(), evento.getTipo(),
                        evento.getFechaHora(), evento.getDuracionMinutos(), evento.getCupoMaximo(),
                        PersonaDTO.desde(evento.getCurador()),
                        conteos.getOrDefault(evento.getId(), 0L),
                        inscriptoDelUsuario.contains(evento.getId()),
                        null))
                .toList();

        return new PaginaEventosDTO(items, paginaEfectiva, tamanioEfectivo, paginaDeEventos.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilaReporteEventoDTO> obtenerFilasParaReporte(LocalDate desde, LocalDate hasta, TipoEvento tipo,
                                                              EstadoEvento estado) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        EstadoEvento estadoEfectivo = estado != null ? estado : EstadoEvento.TODOS;
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaExclusivo = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean filtrarPasados = estadoEfectivo == EstadoEvento.PASADOS;
        boolean filtrarFuturos = estadoEfectivo == EstadoEvento.FUTUROS;

        return eventoRepository.buscarParaReporte(desdeDateTime, hastaExclusivo, tipo, filtrarPasados, filtrarFuturos, ahora)
                .stream()
                .map(fila -> new FilaReporteEventoDTO(
                        fila.getId(), fila.getTitulo(), fila.getTipo(), fila.getFechaHora(),
                        new PersonaDTO(fila.getCuradorId(), fila.getCuradorNombre() + " " + fila.getCuradorApellido()),
                        fila.getCupoMaximo(), fila.getCantidadInscriptos()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDTO obtenerDetalle(Long id, Authentication authentication) {
        EventoEntity eventoEntity = eventoRepository.buscarPorIdConCurador(id)
                .orElseThrow(() -> new RecursoInexistenteException("El evento no existe"));
        return mapearConDetalle(eventoEntity, authentication);
    }

    @Override
    public EventoDTO crear(EventoRequest request) {
        UsuarioEntity curador = usuarioActualService.obtenerCuradorValido(request.getCuradorId());
        EventoEntity eventoEntity = new EventoEntity();
        aplicar(eventoEntity, request, curador);
        EventoEntity guardado = eventoRepository.save(eventoEntity);
        publicarNovedad(guardado, NovedadTipo.ALTA, null, resumirEstado(guardado));
        return new EventoDTO(guardado.getId(), guardado.getTitulo(), guardado.getDescripcion(), guardado.getTipo(),
                guardado.getFechaHora(), guardado.getDuracionMinutos(), guardado.getCupoMaximo(),
                PersonaDTO.desde(curador), 0L, false, List.of());
    }

    @Override
    public EventoDTO editar(Long id, EventoRequest request) {
        EventoEntity eventoEntity = eventoRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoInexistenteException("El evento no existe"));
        UsuarioEntity curador = usuarioActualService.obtenerCuradorValido(request.getCuradorId());
        long inscriptos = inscripcionRepository.countByEventoId(id);
        if (request.getCupoMaximo() < inscriptos) {
            throw new ConflictoException("El cupo no puede ser menor a la cantidad de inscriptos actuales");
        }
        String estadoAnterior = resumirEstado(eventoEntity);
        aplicar(eventoEntity, request, curador);
        EventoEntity guardado = eventoRepository.save(eventoEntity);
        publicarNovedad(guardado, NovedadTipo.MODIFICACION, estadoAnterior, resumirEstado(guardado));
        List<PersonaDTO> inscriptosDTO = inscripcionRepository.findByEventoIdOrderByFechaInscripcionAsc(id).stream()
                .map(inscripcion -> PersonaDTO.desde(inscripcion.getUsuario()))
                .toList();
        return new EventoDTO(guardado.getId(), guardado.getTitulo(), guardado.getDescripcion(), guardado.getTipo(),
                guardado.getFechaHora(), guardado.getDuracionMinutos(), guardado.getCupoMaximo(),
                PersonaDTO.desde(curador), inscriptos, false, inscriptosDTO);
    }

    @Override
    public void borrar(Long id) {
        EventoEntity eventoEntity = eventoRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoInexistenteException("El evento no existe"));
        publicarNovedad(eventoEntity, NovedadTipo.CANCELACION, resumirEstado(eventoEntity), null);
        eventoRepository.delete(eventoEntity);
    }

    @Override
    public void inscribirse(Long eventoId, Authentication authentication) {
        // Primera operación: leer con bloqueo pesimista antes de cualquier validación.
        EventoEntity eventoEntity = eventoRepository.buscarPorIdConBloqueo(eventoId)
                .orElseThrow(() -> new RecursoInexistenteException("El evento no existe"));
        UsuarioEntity usuario = usuarioActualService.obtenerPorEmail(authentication.getName());
        LocalDateTime ahora = LocalDateTime.now(clock);

        if (!eventoEntity.getFechaHora().isAfter(ahora)) {
            throw new ConflictoException("El evento ya comenzó");
        }
        if (inscripcionRepository.existsByEventoIdAndUsuarioId(eventoId, usuario.getId())) {
            throw new ConflictoException("Ya está inscripto en este evento");
        }
        long inscriptos = inscripcionRepository.countByEventoId(eventoId);
        if (inscriptos >= eventoEntity.getCupoMaximo()) {
            throw new ConflictoException("El evento no tiene cupo disponible");
        }

        InscripcionEntity inscripcionEntity = new InscripcionEntity();
        inscripcionEntity.setEvento(eventoEntity);
        inscripcionEntity.setUsuario(usuario);
        inscripcionEntity.setFechaInscripcion(ahora);
        inscripcionRepository.save(inscripcionEntity);
    }

    @Override
    public void desinscribirse(Long eventoId, Authentication authentication) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new RecursoInexistenteException("El evento no existe");
        }
        UsuarioEntity usuario = usuarioActualService.obtenerPorEmail(authentication.getName());
        inscripcionRepository.deleteByEventoIdAndUsuarioId(eventoId, usuario.getId());
    }

    private void aplicar(EventoEntity eventoEntity, EventoRequest request, UsuarioEntity curador) {
        eventoEntity.setTitulo(request.getTitulo());
        eventoEntity.setDescripcion(request.getDescripcion());
        eventoEntity.setTipo(request.getTipo());
        eventoEntity.setFechaHora(request.getFechaHora());
        eventoEntity.setDuracionMinutos(request.getDuracionMinutos());
        eventoEntity.setCupoMaximo(request.getCupoMaximo());
        eventoEntity.setCurador(curador);
    }

    private EventoDTO mapearConDetalle(EventoEntity eventoEntity, Authentication authentication) {
        UsuarioEntity usuarioActual = usuarioActualService.obtenerPorEmail(authentication.getName());
        boolean esGestor = usuarioActualService.esGestor(authentication);
        long cantidadInscriptos = inscripcionRepository.countByEventoId(eventoEntity.getId());
        boolean inscripto = inscripcionRepository.existsByEventoIdAndUsuarioId(eventoEntity.getId(), usuarioActual.getId());

        List<PersonaDTO> inscriptos = null;
        if (esGestor) {
            inscriptos = inscripcionRepository.findByEventoIdOrderByFechaInscripcionAsc(eventoEntity.getId()).stream()
                    .map(inscripcion -> PersonaDTO.desde(inscripcion.getUsuario()))
                    .toList();
        }

        return new EventoDTO(eventoEntity.getId(), eventoEntity.getTitulo(), eventoEntity.getDescripcion(), eventoEntity.getTipo(),
                eventoEntity.getFechaHora(), eventoEntity.getDuracionMinutos(), eventoEntity.getCupoMaximo(),
                PersonaDTO.desde(eventoEntity.getCurador()), cantidadInscriptos, inscripto, inscriptos);
    }

    private Map<Long, Long> contarInscriptosPorEvento(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> conteos = new HashMap<>();
        for (ConteoPorEvento conteo : inscripcionRepository.contarPorEventos(ids)) {
            conteos.put(conteo.getEventoId(), conteo.getCantidad());
        }
        return conteos;
    }

    private Set<Long> idsInscriptoDelUsuarioActual(List<Long> ids, Authentication authentication) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        UsuarioEntity usuarioActual = usuarioActualService.obtenerPorEmail(authentication.getName());
        return new HashSet<>(inscripcionRepository.buscarEventoIdsInscriptoDeUsuario(ids, usuarioActual.getId()));
    }

    @Override
    @Transactional
    public EventoDTO modificarCupo(Long id, Integer nuevoCupo) {
        EventoEntity eventoEntity = eventoRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoInexistenteException("El evento no existe"));

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!eventoEntity.getFechaHora().isAfter(ahora)) {
            throw new ConflictoException("No se puede modificar el cupo de un evento que ya inició");
        }

        long cantidadInscriptos = inscripcionRepository.countByEventoId(id);
        if (nuevoCupo < cantidadInscriptos) {
            throw new ConflictoException(
                "El nuevo cupo (" + nuevoCupo + ") no puede ser menor a los inscriptos actuales (" + cantidadInscriptos + ")"
            );
        }

        String estadoAnterior = resumirEstado(eventoEntity);
        eventoEntity.setCupoMaximo(nuevoCupo);
        EventoEntity guardado = eventoRepository.save(eventoEntity);
        publicarNovedad(guardado, NovedadTipo.MODIFICACION, estadoAnterior, resumirEstado(guardado));

        List<PersonaDTO> inscriptosDTO = inscripcionRepository.findByEventoIdOrderByFechaInscripcionAsc(id).stream()
                .map(inscripcion -> PersonaDTO.desde(inscripcion.getUsuario()))
                .toList();

        return new EventoDTO(guardado.getId(), guardado.getTitulo(), guardado.getDescripcion(), guardado.getTipo(),
                guardado.getFechaHora(), guardado.getDuracionMinutos(), guardado.getCupoMaximo(),
                PersonaDTO.desde(guardado.getCurador()), cantidadInscriptos, false, inscriptosDTO);
    }

    private String resumirEstado(EventoEntity evento) {
        return "Título: %s | Tipo: %s | Fecha: %s | Duración: %d min | Cupo: %d".formatted(
                evento.getTitulo(),
                evento.getTipo(),
                evento.getFechaHora(),
                evento.getDuracionMinutos(),
                evento.getCupoMaximo());
    }

    private void publicarNovedad(EventoEntity evento, NovedadTipo tipo, String datosAnteriores, String datosPosteriores) {
        com.unla.museo.novedades.NovedadEvento novedad = new com.unla.museo.novedades.NovedadEvento(
                java.util.UUID.randomUUID(), tipo, evento.getId(), evento.getTitulo(),
                evento.getFechaHora(), java.time.Instant.now(clock), responsableActual(evento),
                datosAnteriores, datosPosteriores);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    novedadPublisher.publicar(novedad);
                }
            });
        } else {
            novedadPublisher.publicar(novedad);
        }
    }

    private Long responsableActual(EventoEntity evento) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            try {
                return usuarioActualService.obtenerPorEmail(authentication.getName()).getId();
            } catch (RuntimeException ignored) {
                // El evento sigue siendo válido; se usa el curador como fallback.
            }
        }
        return evento.getCurador().getId();
    }
}
