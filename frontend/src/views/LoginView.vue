<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { errMsg } from '../api/http'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    router.push({ name: 'todos' })
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <form class="card auth-card" @submit.prevent="submit">
      <div class="brand brand-lg">✔ TaskFlow</div>
      <h1>Welcome back</h1>
      <p class="muted">Log in to manage your tasks.</p>

      <p v-if="error" class="alert alert-error">{{ error }}</p>

      <label class="field">
        <span>Username</span>
        <input v-model="form.username" autocomplete="username" required autofocus />
      </label>
      <label class="field">
        <span>Password</span>
        <input v-model="form.password" type="password" autocomplete="current-password" required />
      </label>

      <button class="btn btn-primary btn-block" :disabled="loading">
        {{ loading ? 'Logging in…' : 'Log in' }}
      </button>
      <p class="muted center">
        No account? <router-link to="/register">Create one</router-link>
      </p>
    </form>
  </div>
</template>
