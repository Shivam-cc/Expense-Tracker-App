import { createContext, useContext, useState, useCallback } from 'react'
import { authApi } from '../services/api'
import { preparePassword } from '../utils/crypto'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user')
    return saved ? JSON.parse(saved) : null
  })

  const login = useCallback(async (email, password) => {
    const normalizedEmail = email.trim().toLowerCase()
    const encrypted = await preparePassword(password)
    const res = await authApi.login({ email: normalizedEmail, password: encrypted })
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify({ name: res.name, email: res.email }))
    setToken(res.token)
    setUser({ name: res.name, email: res.email })
  }, [])

  const register = useCallback(async (name, email, password, otp) => {
    const normalizedEmail = email.trim().toLowerCase()
    const encrypted = await preparePassword(password)
    const res = await authApi.register({ name: name.trim(), email: normalizedEmail, password: encrypted, otp })
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify({ name: res.name, email: res.email }))
    setToken(res.token)
    setUser({ name: res.name, email: res.email })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
