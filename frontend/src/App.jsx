import { useApolloClient } from '@apollo/client/react'
import {
  Link,
  Route,
  Routes,
  useNavigate
} from 'react-router-dom'
import { useEffect, useState } from 'react'
import Catalogo from './pages/Catalogo'
import DetalleObra from './pages/DetalleObra'
import ReporteAsistencia from './components/ReporteAsistencia'
import Home from './components/Home'
import Login from './components/Login'
import Registro from './components/Registro'
import RequiereRol from './components/RequiereRol'
import { borrarToken, EVENTO_SESION_VENCIDA, obtenerToken, pedir } from './api/cliente'
import { ROLES, esGestor } from './roles'

function Navegacion({ user, onLogout }) {
  return (
    <nav className="navegacion">
      <Link
        to="/"
        className="navegacion-logo"
      >
        Museo Virtual
      </Link>

      <div className="navegacion-links">
        <Link
          to="/"
          className="navegacion-link"
        >
          Inicio
        </Link>

        <Link
          to="/coleccion"
          className="navegacion-link"
        >
          Colección
        </Link>

        {esGestor(user) && (
          <Link
            to="/reporte"
            className="navegacion-link"
          >
            Reporte
          </Link>
        )}

        <span className="usuario-info">
          {user?.firstName} {user?.lastName}
          {' · '}
          {user?.role}
        </span>

        <button
          type="button"
          onClick={onLogout}
          className="navegacion-link"
        >
          Cerrar sesión
        </button>
      </div>
    </nav>
  )
}

function Inicio() {
  const navigate = useNavigate()

  return (
    <Home
      onExplorar={() => navigate('/coleccion')}
    />
  )
}

function Reporte() {
  return <ReporteAsistencia />
}

function App() {
  const client = useApolloClient()

  const [token, setToken] = useState(obtenerToken)
  const [user, setUser] = useState(null)

  const handleLogin = (accessToken) => {
    setToken(accessToken)
  }

  const handleLogout = () => {
    borrarToken()
    setToken(null)
    setUser(null)
    // Sin esto, al entrar con otro usuario Apollo seguiría mostrando
    // resultados cacheados de la sesión anterior.
    client.clearStore()
  }

  useEffect(() => {
    function alVencerSesion() {
      setToken(null)
      setUser(null)
      client.clearStore()
    }

    window.addEventListener(EVENTO_SESION_VENCIDA, alVencerSesion)
    return () => window.removeEventListener(EVENTO_SESION_VENCIDA, alVencerSesion)
  }, [client])

  useEffect(() => {
    if (!token) {
      return
    }

    let cancelado = false

    pedir('/api/auth/me')
      .then((userData) => {
        if (!cancelado) {
          setUser(userData)
        }
      })
      .catch((error) => {
        console.error(error)
        if (!cancelado) {
          borrarToken()
          setToken(null)
          setUser(null)
        }
      })

    return () => {
      cancelado = true
    }
  }, [token])

  if (!token) {
    return (
      <main className="app">
        <Routes>
          <Route
            path="/registro"
            element={<Registro onLogin={handleLogin} />}
          />

          <Route
            path="*"
            element={<Login onLogin={handleLogin} />}
          />
        </Routes>
      </main>
    )
  }

  if (!user) {
    return (
      <main className="app">
        <p>Cargando usuario...</p>
      </main>
    )
  }

  return (
    <main className="app">
      <Navegacion
  user={user}
  onLogout={handleLogout}/>

      <Routes>
        <Route
          path="/"
          element={<Inicio />}
        />

        <Route
          path="/coleccion"
          element={<Catalogo />}
        />

        <Route
          path="/obra/:id"
          element={<DetalleObra />}
        />

        <Route
          path="/reporte"
          element={
            <RequiereRol
              usuario={user}
              roles={[ROLES.CURADOR, ROLES.ADMINISTRADOR]}
            >
              <Reporte />
            </RequiereRol>
          }
        />
      </Routes>
    </main>
  )
}

export default App