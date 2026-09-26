
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import { ApolloClient, InMemoryCache } from '@apollo/client'
import { ApolloProvider } from '@apollo/client/react'
import { HttpLink } from '@apollo/client/link/http'
import { SetContextLink } from '@apollo/client/link/context'
import { ErrorLink } from '@apollo/client/link/error'
import { ServerError } from '@apollo/client/errors'

import './index.css'
import App from './App.jsx'
import { API_URL, avisarSesionVencida, obtenerToken } from './api/cliente'

const httpLink = new HttpLink({
  uri: `${API_URL}/graphql`,
})

const authLink = new SetContextLink((prevContext) => {
  const token = obtenerToken()

  return {
    headers: {
      ...prevContext.headers,
      authorization: token ? `Bearer ${token}` : '',
    },
  }
})

// Spring Security corta con 401 antes de llegar al resolver cuando el token
// venció: sin esto la pantalla quedaría mostrando el error en lugar de
// volver al login.
const sesionVencidaLink = new ErrorLink(({ error }) => {
  if (ServerError.is(error) && error.statusCode === 401) {
    avisarSesionVencida()
  }
})

const client = new ApolloClient({
  link: sesionVencidaLink.concat(authLink).concat(httpLink),
  cache: new InMemoryCache(),
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <ApolloProvider client={client}>
        <App />
      </ApolloProvider>
    </BrowserRouter>
  </StrictMode>,
)
