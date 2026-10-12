<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { listTodos, toggleTodo, deleteTodo, downloadAttachment } from '../api/todos'
import { errMsg } from '../api/http'
import TodoModal from '../components/TodoModal.vue'

const todos = ref([])
const pageInfo = reactive({ page: 0, totalPages: 0, totalElements: 0 })
const loading = ref(false)
const error = ref('')
const toast = ref('')

const filters = reactive({
  q: '',
  status: 'all',      // all | active | done
  priority: '',       // '' | LOW | MEDIUM | HIGH
  sortBy: 'createdAt',
  direction: 'desc',
  size: 8
})

const showModal = ref(false)
const editing = ref(null)

let toastTimer
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2500)
}

async function load(page = pageInfo.page) {
  loading.value = true
  error.value = ''
  try {
    const data = await listTodos({
      q: filters.q.trim() || undefined,
      completed: filters.status === 'all' ? undefined : filters.status === 'done',
      priority: filters.priority || undefined,
      sortBy: filters.sortBy,
      direction: filters.direction,
      page,
      size: filters.size
    })
    // If the last item of a page was deleted, step back one page
    if (data.content.length === 0 && data.page > 0 && data.totalPages > 0) {
      return load(data.totalPages - 1)
    }
    todos.value = data.content
    pageInfo.page = data.page
    pageInfo.totalPages = data.totalPages
    pageInfo.totalElements = data.totalElements
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
}

// Debounced search; instant reload for the other filters
let debounce
watch(() => filters.q, () => {
  clearTimeout(debounce)
  debounce = setTimeout(() => load(0), 300)
})
watch(() => [filters.status, filters.priority, filters.sortBy, filters.direction], () => load(0))

onMounted(() => load(0))
onBeforeUnmount(() => { clearTimeout(debounce); clearTimeout(toastTimer) })

function openNew() { editing.value = null; showModal.value = true }
function openEdit(t) { editing.value = t; showModal.value = true }

function onSaved(msg) {
  showModal.value = false
  showToast(msg)
  load()
}

async function onToggle(t) {
  try {
    await toggleTodo(t.id)
    load()
  } catch (e) { error.value = errMsg(e) }
}

async function onDelete(t) {
  if (!confirm(`Delete "${t.title}"?`)) return
  try {
    await deleteTodo(t.id)
    showToast('Task deleted')
    load()
  } catch (e) { error.value = errMsg(e) }
}

async function onDownload(t) {
  try { await downloadAttachment(t) } catch (e) { error.value = errMsg(e) }
}

function clearFilters() {
  filters.q = ''
  filters.status = 'all'
  filters.priority = ''
}

function isOverdue(t) {
  return !t.completed && t.dueDate && t.dueDate < new Date().toISOString().slice(0, 10)
}
function fmtDate(d) {
  return new Date(d + 'T00:00:00').toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}
const priorityLabel = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' }
</script>

<template>
  <section class="container">
    <div class="head-row">
      <div>
        <h1>My tasks</h1>
        <p class="muted">{{ pageInfo.totalElements }} {{ pageInfo.totalElements === 1 ? 'task' : 'tasks' }}</p>
      </div>
      <button class="btn btn-primary" @click="openNew">+ New task</button>
    </div>

    <div class="card toolbar">
      <input v-model="filters.q" class="search" type="search" placeholder="Search title or description…" />
      <select v-model="filters.status" aria-label="Status filter">
        <option value="all">All status</option>
        <option value="active">Active</option>
        <option value="done">Completed</option>
      </select>
      <select v-model="filters.priority" aria-label="Priority filter">
        <option value="">All priorities</option>
        <option value="HIGH">High</option>
        <option value="MEDIUM">Medium</option>
        <option value="LOW">Low</option>
      </select>
      <select v-model="filters.sortBy" aria-label="Sort by">
        <option value="createdAt">Sort: Created</option>
        <option value="dueDate">Sort: Due date</option>
        <option value="priority">Sort: Priority</option>
        <option value="title">Sort: Title</option>
        <option value="completed">Sort: Status</option>
      </select>
      <button class="btn btn-ghost" :title="filters.direction === 'asc' ? 'Ascending' : 'Descending'"
              @click="filters.direction = filters.direction === 'asc' ? 'desc' : 'asc'">
        {{ filters.direction === 'asc' ? '↑ Asc' : '↓ Desc' }}
      </button>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="loading && !todos.length" class="muted center pad">Loading…</div>

    <div v-else-if="!todos.length" class="card empty">
      <p>No tasks found.</p>
      <button v-if="filters.q || filters.status !== 'all' || filters.priority" class="btn btn-ghost" @click="clearFilters">
        Clear filters
      </button>
      <button v-else class="btn btn-primary" @click="openNew">Create your first task</button>
    </div>

    <ul v-else class="list" :class="{ dim: loading }">
      <li v-for="t in todos" :key="t.id" class="card item" :class="{ done: t.completed }">
        <input class="tick" type="checkbox" :checked="t.completed" :aria-label="`Mark ${t.title} done`" @change="onToggle(t)" />
        <div class="item-body">
          <div class="item-title">{{ t.title }}</div>
          <p v-if="t.description" class="item-desc">{{ t.description }}</p>
          <div class="meta">
            <span class="badge" :class="'p-' + t.priority.toLowerCase()">{{ priorityLabel[t.priority] }}</span>
            <span v-if="t.dueDate" class="chip" :class="{ overdue: isOverdue(t) }">
              📅 {{ fmtDate(t.dueDate) }}<template v-if="isOverdue(t)"> · overdue</template>
            </span>
            <button v-if="t.attachmentName" class="chip chip-link" @click="onDownload(t)">📎 {{ t.attachmentName }}</button>
          </div>
        </div>
        <div class="item-actions">
          <button class="btn btn-ghost btn-sm" @click="openEdit(t)">Edit</button>
          <button class="btn btn-ghost btn-sm danger" @click="onDelete(t)">Delete</button>
        </div>
      </li>
    </ul>

    <div v-if="pageInfo.totalPages > 1" class="pager">
      <button class="btn btn-ghost" :disabled="pageInfo.page === 0" @click="load(pageInfo.page - 1)">← Prev</button>
      <span class="muted">Page {{ pageInfo.page + 1 }} of {{ pageInfo.totalPages }}</span>
      <button class="btn btn-ghost" :disabled="pageInfo.page + 1 >= pageInfo.totalPages" @click="load(pageInfo.page + 1)">Next →</button>
    </div>

    <TodoModal v-if="showModal" :todo="editing" @close="showModal = false" @saved="onSaved" />
    <div v-if="toast" class="toast">{{ toast }}</div>
  </section>
</template>
