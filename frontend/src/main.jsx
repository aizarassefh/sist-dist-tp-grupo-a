import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ApolloClient, InMemoryCache } from '@apollo/client'
import { ApolloProvider } from '@apollo/client/react'
import { HttpLink } from '@apollo/client/link/http'
import './index.css'
import App from './App.jsx'

const httpLink = new HttpLink({
  uri: 'http://localhost:8080/graphql',
})

const client = new ApolloClient({
  link: httpLink,
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