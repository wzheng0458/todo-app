import http from './http'

export const listTodos = (params) => http.get('/todos', { params }).then((r) => r.data)
export const createTodo = (data) => http.post('/todos', data).then((r) => r.data)
export const updateTodo = (id, data) => http.put(`/todos/${id}`, data).then((r) => r.data)
export const toggleTodo = (id) => http.patch(`/todos/${id}/toggle`).then((r) => r.data)
export const deleteTodo = (id) => http.delete(`/todos/${id}`)

export const uploadAttachment = (id, file) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post(`/todos/${id}/attachment`, fd).then((r) => r.data)
}
export const removeAttachment = (id) => http.delete(`/todos/${id}/attachment`).then((r) => r.data)

/** The download endpoint needs the JWT, so fetch as a blob and trigger a save. */
export async function downloadAttachment(todo) {
  const res = await http.get(`/todos/${todo.id}/attachment`, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = todo.attachmentName || 'attachment'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
