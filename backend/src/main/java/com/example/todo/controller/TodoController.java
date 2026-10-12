package com.example.todo.controller;

import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.model.Priority;
import com.example.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * GET /api/todos?q=milk&completed=false&priority=HIGH&sortBy=dueDate&direction=asc&page=0&size=10
     */
    @GetMapping
    public PageResponse<TodoResponse> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return todoService.search(user.getUsername(), q, completed, priority, sortBy, direction, page, size);
    }

    @GetMapping("/{id}")
    public TodoResponse get(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return todoService.get(user.getUsername(), id);
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@AuthenticationPrincipal UserDetails user,
                                               @Valid @RequestBody TodoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.create(user.getUsername(), req));
    }

    @PutMapping("/{id}")
    public TodoResponse update(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                               @Valid @RequestBody TodoRequest req) {
        return todoService.update(user.getUsername(), id, req);
    }

    @PatchMapping("/{id}/toggle")
    public TodoResponse toggle(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return todoService.toggle(user.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        todoService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ---------- File upload ----------

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TodoResponse upload(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                               @RequestParam("file") MultipartFile file) {
        return todoService.attach(user.getUsername(), id, file);
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        Resource resource = todoService.loadAttachment(user.getUsername(), id);
        String name = todoService.attachmentName(user.getUsername(), id);
        MediaType type = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @DeleteMapping("/{id}/attachment")
    public TodoResponse removeAttachment(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return todoService.removeAttachment(user.getUsername(), id);
    }
}
