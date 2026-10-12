<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { errMsg } from '../api/http'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ username: '', email: '', password: '', confirm: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (form.password !== form.confirm) {
    error.value = 'Passwords do not match'
    return
  }
  loading.value = true
  try {
    await auth.register(form.username.trim(), form.email.trim(), form.password)
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
      <h1>Create account</h1>
      <p class="muted">It only takes a few seconds.</p>

      <p v-if="error" class="alert alert-error">{{ error }}</p>

      <label class="field">
        <span>Username</span>
        <input v-model="form.username" autocomplete="username" minlength="3" maxlength="50" required autofocus />
      </label>
      <label class="field">
        <span>Email</span>
        <input v-model="form.email" type="email" autocomplete="email" required />
      </label>
      <label class="field">
        <span>Password</span>
        <input v-model="form.password" type="password" autocomplete="new-password" minlength="6" required />
      </label>
      <label class="field">
        <span>Confirm password</span>
        <input v-model="form.confirm" type="password" autocomplete="new-password" required />
      </label>

      <button class="btn btn-primary btn-block" :disabled="loading">
        {{ loading ? 'Creating…' : 'Sign up' }}
      </button>
      <p class="muted center">
        Already registered? <router-link to="/login">Log in</router-link>
      </p>
    </form>
  </div>
</template>
