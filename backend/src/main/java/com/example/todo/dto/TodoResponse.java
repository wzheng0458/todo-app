package com.example.todo.dto;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Priority priority,
        LocalDate dueDate,
        String attachmentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TodoResponse from(Todo t) {
        return new TodoResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.isCompleted(),
                t.getPriority(), t.getDueDate(), t.getAttachmentName(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
