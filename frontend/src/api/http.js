import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000
})

// Attach the JWT to every request
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Expired / invalid token -> back to login
http.interceptors.response.use(
  (res) => res,
  (error) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      if (location.pathname !== '/login') location.href = '/login'
    }
    return Promise.reject(error)
  }
)

/** Turns an axios error into a readable message. */
export function errMsg(e) {
  const d = e.response?.data
  if (d?.errors) return Object.values(d.errors).join('. ')
  return d?.message || (e.code === 'ERR_NETWORK' ? 'Cannot reach the server' : e.message) || 'Something went wrong'
}

export default http
