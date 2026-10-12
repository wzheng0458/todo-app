<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const showHeader = computed(() => auth.isLoggedIn && route.name === 'todos')

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header v-if="showHeader" class="topbar">
    <div class="topbar-inner">
      <div class="brand">✔ TaskFlow</div>
      <div class="user">
        <span class="user-name">{{ auth.username }}</span>
        <button class="btn btn-ghost" @click="logout">Log out</button>
      </div>
    </div>
  </header>
  <main class="page">
    <router-view />
  </main>
</template>
