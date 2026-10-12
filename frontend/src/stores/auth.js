import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || ''
  }),
  getters: {
    isLoggedIn: (s) => !!s.token
  },
  actions: {
    _save(data) {
      this.token = data.token
      this.username = data.username
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
    },
    async login(username, password) {
      const { data } = await http.post('/auth/login', { username, password })
      this._save(data)
    },
    async register(username, email, password) {
      const { data } = await http.post('/auth/register', { username, email, password })
      this._save(data)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
    }
  }
})
