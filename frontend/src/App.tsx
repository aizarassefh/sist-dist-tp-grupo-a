import { useApolloClient } from '@apollo/client/react'
import { useEffect, useState } from 'react'
import { borrarToken, EVENTO_SESION_VENCIDA, obtenerToken, pedir } from './api/cliente'
import AppRouter from './router/AppRouter'

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

  return (
    <AppRouter
      token={token}
      user={user}
      onLogin={handleLogin}
      onLogout={handleLogout}
    />
  )
}

export default App
