import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Catalogo from '../pages/Catalogo'
import DetalleObra from '../pages/DetalleObra'
import Eventos from '../pages/Eventos'
import DetalleEvento from '../pages/DetalleEvento'
import EditarEvento from '../pages/EditarEvento'
import FiltrosFavoritos from '../pages/FiltrosFavoritos'
import ReporteAsistencia from '../components/ReporteAsistencia'
import HistorialAuditoria from '../pages/HistorialAuditoria'
import Recorridos from '../pages/Recorridos'
import Comparar from '../pages/Comparar'
import Recomendaciones from '../pages/Recomendaciones'
import Explorar from '../pages/Explorar'
import ExplorarMovimiento from '../pages/ExplorarMovimiento'
import Home from '../components/Home'
import Login from '../components/Login'
import Registro from '../components/Registro'
import { ROLES } from './roles'
import Navegacion from './Navegacion'
import RequiereRol from './RequiereRol'

function RutasPublicas({ onLogin }) {
  return (
    <main className="app-acceso">
      <Routes>
        <Route path="/registro" element={<Registro onLogin={onLogin} />} />
        <Route path="*" element={<Login onLogin={onLogin} />} />
      </Routes>
    </main>
  )
}

function RutasPrivadas({ user, onLogout }) {
  return (
    <main className="app">
      <Navegacion user={user} onLogout={onLogout} />

      <Routes>
        <Route path="/" element={<Home usuario={user} />} />
        <Route path="/coleccion" element={<Catalogo />} />
        <Route path="/obra/:id" element={<DetalleObra usuario={user} />} />
        <Route path="/eventos" element={<Eventos usuario={user} />} />
        <Route
          path="/eventos/nuevo"
          element={
            <RequiereRol usuario={user} roles={[ROLES.CURADOR, ROLES.ADMINISTRADOR]}>
              <EditarEvento />
            </RequiereRol>
          }
        />
        <Route
          path="/eventos/:id/editar"
          element={
            <RequiereRol usuario={user} roles={[ROLES.CURADOR, ROLES.ADMINISTRADOR]}>
              <EditarEvento />
            </RequiereRol>
          }
        />
        <Route path="/eventos/:id" element={<DetalleEvento usuario={user} />} />
        <Route path="/favoritos" element={<FiltrosFavoritos />} />
        <Route
          path="/recorridos"
          element={
            <RequiereRol usuario={user} roles={[ROLES.VISITANTE]}>
              <Recorridos />
            </RequiereRol>
          }
        />
        <Route path="/comparar" element={<Comparar />} />
        <Route
          path="/recomendaciones"
          element={
            <RequiereRol usuario={user} roles={[ROLES.VISITANTE]}>
              <Recomendaciones />
            </RequiereRol>
          }
        />
        <Route path="/explorar" element={<Explorar />} />
        <Route path="/explorar/:movimiento" element={<ExplorarMovimiento />} />
        <Route
          path="/reporte"
          element={
            <RequiereRol usuario={user} roles={[ROLES.CURADOR, ROLES.ADMINISTRADOR]}>
              <ReporteAsistencia />
            </RequiereRol>
          }
        />
        <Route
          path="/historial"
          element={
            <RequiereRol usuario={user} roles={[ROLES.CURADOR, ROLES.ADMINISTRADOR]}>
              <HistorialAuditoria />
            </RequiereRol>
          }
        />
      </Routes>
    </main>
  )
}

function AppRouter({ token, user, onLogin, onLogout }) {
  return (
    <BrowserRouter>
      {!token ? (
        <RutasPublicas onLogin={onLogin} />
      ) : !user ? (
        <main className="app">
          <p>Cargando usuario...</p>
        </main>
      ) : (
        <RutasPrivadas user={user} onLogout={onLogout} />
      )}
    </BrowserRouter>
  )
}

export default AppRouter
