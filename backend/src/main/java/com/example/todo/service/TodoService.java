package com.example.todo.service;

import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.model.User;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "dueDate", "title", "priority", "completed");

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    public TodoService(TodoRepository todoRepository, UserRepository userRepository, FileStorageService storage) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> search(String username, String q, Boolean completed, Priority priority,
                                             String sortBy, String direction, int page, int size) {
        User user = getUser(username);

        Specification<Todo> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), user.getId());
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("title")), like),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like)));
        }
        if (completed != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("completed"), completed));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }

        String sortField = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Secondary sort keeps pagination stable
        Sort sort = Sort.by(dir, sortField).and(Sort.by(Sort.Direction.DESC, "id"));

        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<TodoResponse> result = todoRepository
                .findAll(spec, PageRequest.of(Math.max(page, 0), safeSize, sort))
                .map(TodoResponse::from);
        log.debug("Search by {} q='{}' completed={} priority={} -> {} results",
                username, q, completed, priority, result.getTotalElements());
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public TodoResponse get(String username, Long id) {
        return TodoResponse.from(findOwned(username, id));
    }

    @Transactional
    public TodoResponse create(String username, TodoRequest req) {
        Todo todo = new Todo();
        todo.setUser(getUser(username));
        apply(todo, req);
        Todo saved = todoRepository.save(todo);
        log.info("User {} created todo {}", username, saved.getId());
        return TodoResponse.from(saved);
    }

    @Transactional
    public TodoResponse update(String username, Long id, TodoRequest req) {
        Todo todo = findOwned(username, id);
        apply(todo, req);
        log.info("User {} updated todo {}", username, id);
        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse toggle(String username, Long id) {
        Todo todo = findOwned(username, id);
        todo.setCompleted(!todo.isCompleted());
        log.info("User {} set todo {} completed={}", username, id, todo.isCompleted());
        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(String username, Long id) {
        Todo todo = findOwned(username, id);
        storage.delete(todo.getAttachmentStoredName());
        todoRepository.delete(todo);
        log.info("User {} deleted todo {}", username, id);
    }

    // ---------- Attachments ----------

    @Transactional
    public TodoResponse attach(String username, Long id, MultipartFile file) {
        Todo todo = findOwned(username, id);
        String stored = storage.store(file);
        storage.delete(todo.getAttachmentStoredName()); // replace any previous file
        todo.setAttachmentStoredName(stored);
        todo.setAttachmentName(file.getOriginalFilename());
        log.info("User {} attached '{}' to todo {}", username, file.getOriginalFilename(), id);
        return TodoResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public Resource loadAttachment(String username, Long id) {
        Todo todo = findOwned(username, id);
        if (todo.getAttachmentStoredName() == null) {
            throw new ResourceNotFoundException("This todo has no attachment");
        }
        return storage.load(todo.getAttachmentStoredName());
    }

    @Transactional(readOnly = true)
    public String attachmentName(String username, Long id) {
        return findOwned(username, id).getAttachmentName();
    }

    @Transactional
    public TodoResponse removeAttachment(String username, Long id) {
        Todo todo = findOwned(username, id);
        storage.delete(todo.getAttachmentStoredName());
        todo.setAttachmentStoredName(null);
        todo.setAttachmentName(null);
        log.info("User {} removed attachment from todo {}", username, id);
        return TodoResponse.from(todo);
    }

    // ---------- helpers ----------

    private void apply(Todo todo, TodoRequest req) {
        todo.setTitle(req.title().trim());
        todo.setDescription(req.description());
        todo.setDueDate(req.dueDate());
        todo.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        if (req.completed() != null) {
            todo.setCompleted(req.completed());
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Looks up a todo that belongs to the user; other users' todos look like 404. */
    private Todo findOwned(String username, Long id) {
        User user = getUser(username);
        return todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo " + id + " not found"));
    }
}
