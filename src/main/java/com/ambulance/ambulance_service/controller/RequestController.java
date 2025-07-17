package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.RequestDTO;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {
    
    @Autowired
    private RequestService requestService;
    
    @PostMapping
    public ResponseEntity<Request> createRequest(@Valid @RequestBody RequestDTO requestDTO) {
        Request request = new Request();
        request.setUserName(requestDTO.getUserName());
        request.setUserContact(requestDTO.getUserContact());
        request.setLocation(requestDTO.getLocation());
        
        Request savedRequest = requestService.createRequest(request);
        return new ResponseEntity<>(savedRequest, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<?> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        return ResponseEntity.ok(requestService.getPendingRequests());
    }
}