import { useState } from 'react'
import { guardarToken, mensajeDeError, pedir } from '../api/cliente'

function Login({ onLogin }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()

    setError('')
    setLoading(true)

    try {
      const data = await pedir('/api/auth/login', {
        metodo: 'POST',
        cuerpo: { email, password },
      })

      guardarToken(data.accessToken)

      onLogin(data.accessToken)
    } catch (error) {
      setError(
        mensajeDeError(error, {
          401: 'Email o contraseña incorrectos',
        })
      )
    } finally {
      setLoading(false)
    }
  }

  return (
  <div className="login-container">
    <form onSubmit={handleSubmit}>

      <h2>Iniciar sesión</h2>

      <div>
        <label>Email</label>
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
      </div>

      <div>
        <label>Contraseña</label>
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
      </div>

      {error && (
        <p className="login-error">
          {error}
        </p>
      )}

      <button type="submit" disabled={loading}>
        {loading ? 'Ingresando...' : 'Iniciar sesión'}
      </button>

    </form>
  </div>
)
}

export default Login