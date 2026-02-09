package com.hackathon.app.service;

import com.hackathon.app.dto.CreateTaskRequest;
import com.hackathon.app.dto.TaskResponse;
import com.hackathon.app.dto.UpdateTaskRequest;
import com.hackathon.app.entity.Task;
import com.hackathon.app.entity.User;
import com.hackathon.app.exception.ResourceNotFoundException;
import com.hackathon.app.repository.TaskRepository;
import com.hackathon.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User user = getCurrentUser();

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus("PENDING");
        task.setUser(user);

        task = taskRepository.save(task);
        return new TaskResponse(task);
    }

    public List<TaskResponse> getAllTasks() {
        User user = getCurrentUser();
        return taskRepository.findByUserId(user.getId()).stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }

        return new TaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        task = taskRepository.save(task);
        return new TaskResponse(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        User user = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }

        taskRepository.delete(task);
    }
}
