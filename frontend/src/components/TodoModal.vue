<script setup>
import { reactive, ref, computed } from 'vue'
import {
  createTodo, updateTodo, uploadAttachment, removeAttachment, downloadAttachment
} from '../api/todos'
import { errMsg } from '../api/http'

const props = defineProps({ todo: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.todo)
const form = reactive({
  title: props.todo?.title ?? '',
  description: props.todo?.description ?? '',
  priority: props.todo?.priority ?? 'MEDIUM',
  dueDate: props.todo?.dueDate ?? '',
  completed: props.todo?.completed ?? false
})
const file = ref(null)
const attachmentName = ref(props.todo?.attachmentName ?? null)
const error = ref('')
const saving = ref(false)

const MAX_MB = 5

function onFile(e) {
  const f = e.target.files[0]
  error.value = ''
  if (f && f.size > MAX_MB * 1024 * 1024) {
    error.value = `File is too large (max ${MAX_MB}MB)`
    e.target.value = ''
    file.value = null
    return
  }
  file.value = f || null
}

async function submit() {
  error.value = ''
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description?.trim() || null,
      priority: form.priority,
      dueDate: form.dueDate || null,
      completed: form.completed
    }
    let saved = isEdit.value ? await updateTodo(props.todo.id, payload) : await createTodo(payload)
    if (file.value) saved = await uploadAttachment(saved.id, file.value)
    emit('saved', isEdit.value ? 'Task updated' : 'Task created')
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    saving.value = false
  }
}

async function removeFile() {
  try {
    await removeAttachment(props.todo.id)
    attachmentName.value = null
  } catch (e) {
    error.value = errMsg(e)
  }
}

async function download() {
  try {
    await downloadAttachment({ id: props.todo.id, attachmentName: attachmentName.value })
  } catch (e) {
    error.value = errMsg(e)
  }
}
</script>

<template>
  <div class="overlay" @mousedown.self="emit('close')">
    <form class="modal card" @submit.prevent="submit">
      <div class="modal-head">
        <h2>{{ isEdit ? 'Edit task' : 'New task' }}</h2>
        <button type="button" class="icon-btn" aria-label="Close" @click="emit('close')">✕</button>
      </div>

      <p v-if="error" class="alert alert-error">{{ error }}</p>

      <label class="field">
        <span>Title *</span>
        <input v-model="form.title" maxlength="150" required autofocus />
      </label>

      <label class="field">
        <span>Description</span>
        <textarea v-model="form.description" rows="3" maxlength="1000"></textarea>
      </label>

      <div class="row-2">
        <label class="field">
          <span>Priority</span>
          <select v-model="form.priority">
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
        </label>
        <label class="field">
          <span>Due date</span>
          <input v-model="form.dueDate" type="date" />
        </label>
      </div>

      <label v-if="isEdit" class="check">
        <input v-model="form.completed" type="checkbox" /> Completed
      </label>

      <div class="field">
        <span>Attachment (max {{ MAX_MB }}MB)</span>
        <div v-if="attachmentName" class="attached">
          <span class="file-name">📎 {{ attachmentName }}</span>
          <button type="button" class="btn btn-ghost btn-sm" @click="download">Download</button>
          <button type="button" class="btn btn-ghost btn-sm danger" @click="removeFile">Remove</button>
        </div>
        <input type="file" accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.doc,.docx,.xls,.xlsx,.csv,.zip" @change="onFile" />
        <small v-if="attachmentName && file" class="muted">The new file will replace the current one.</small>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn btn-ghost" @click="emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="saving">{{ saving ? 'Saving…' : 'Save' }}</button>
      </div>
    </form>
  </div>
</template>
