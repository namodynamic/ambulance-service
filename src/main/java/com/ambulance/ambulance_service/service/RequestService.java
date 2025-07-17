package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RequestService {
    
    @Autowired
    private RequestRepository requestRepository;
    
    public Request createRequest(Request request) {
        return requestRepository.save(request);
    }
    
    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByRequestTimeDesc();
    }
    
    public List<Request> getPendingRequests() {
        return requestRepository.findByStatus("PENDING");
    }
    
    public Request getRequestById(Long id) {
        return requestRepository.findById(id).orElse(null);
    }
    
    public Request updateRequest(Request request) {
        return requestRepository.save(request);
    }
}