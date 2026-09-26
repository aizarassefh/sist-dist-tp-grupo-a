import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { guardarToken, mensajeDeError, pedir } from '../api/cliente'
import PantallaAcceso from './PantallaAcceso'

// Misma regla que valida el backend (ver validación de registro), repetida acá
// para que el usuario vea el motivo antes de mandar el formulario.
const REGEX_PASSWORD = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,}$/

function Registro({ onLogin }) {
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [email, setEmail] = useState('')
  const [telefono, setTelefono] = useState('')
  const [password, setPassword] = useState('')
  const [repetirPassword, setRepetirPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()

    setError('')

    if (password.length < 8 || password.length > 100 || !REGEX_PASSWORD.test(password)) {
      setError('La contraseña no cumple con el formato requerido.')
      return
    }

    if (password !== repetirPassword) {
      setError('Las contraseñas no coinciden.')
      return
    }

    setLoading(true)

    try {
      await pedir('/api/auth/register', {
        metodo: 'POST',
        cuerpo: {
          email,
          firstName: nombre,
          lastName: apellido,
          phoneNumber: telefono || null,
          password,
        },
      })

      // El registro no deja al usuario autenticado, así que se hace login
      // con las mismas credenciales para no pedírselas de nuevo.
      const data = await pedir('/api/auth/login', {
        metodo: 'POST',
        cuerpo: { email, password },
      })

      guardarToken(data.accessToken)

      onLogin(data.accessToken)
      // /registro no existe para un usuario logueado: sin esto quedaría una
      // página vacía debajo de la navegación.
      navigate('/')
    } catch (error) {
      setError(
        mensajeDeError(error, {
          409: 'Ese email ya está registrado.',
          400: 'Revisá los datos: alguno no es válido.',
        })
      )
    } finally {
      setLoading(false)
    }
  }

  return (
  <PantallaAcceso>
    <form onSubmit={handleSubmit}>

      <h2>Creá tu cuenta</h2>

      <div>
        <label>Nombre</label>
        <input
          type="text"
          value={nombre}
          onChange={(event) => setNombre(event.target.value)}
          required
        />
      </div>

      <div>
        <label>Apellido</label>
        <input
          type="text"
          value={apellido}
          onChange={(event) => setApellido(event.target.value)}
          required
        />
      </div>

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
        <label>Teléfono (opcional)</label>
        <input
          type="tel"
          value={telefono}
          onChange={(event) => setTelefono(event.target.value)}
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
        <p className="login-ayuda">
          Entre 8 y 100 caracteres, con al menos una mayúscula, una minúscula,
          un número y un carácter especial: @ $ ! % * ? & . # _ -
        </p>
      </div>

      <div>
        <label>Repetir contraseña</label>
        <input
          type="password"
          value={repetirPassword}
          onChange={(event) => setRepetirPassword(event.target.value)}
          required
        />
      </div>

      {error && (
        <p className="login-error">
          {error}
        </p>
      )}

      <button type="submit" disabled={loading}>
        {loading ? 'Creando cuenta...' : 'Crear cuenta'}
      </button>

      <p className="login-enlace">
        <Link to="/">¿Ya tenés cuenta? Iniciá sesión</Link>
      </p>

    </form>
  </PantallaAcceso>
)
}

export default Registro
