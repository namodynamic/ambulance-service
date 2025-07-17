package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class DispatchService {
    
    @Autowired
    private RequestService requestService;
    
    @Autowired
    private AmbulanceService ambulanceService;
    
    @Transactional
    public void dispatchAmbulance(Long requestId) {
        Request request = requestService.getRequestById(requestId);
        if (request == null) {
            throw new ServiceException("Request not found");
        }
        
        Ambulance availableAmbulance = ambulanceService.getNextAvailableAmbulance();
        if (availableAmbulance == null) {
            throw new ServiceException("No ambulances available at the moment");
        }
        
        // Update request
        request.setAmbulance(availableAmbulance);
        request.setDispatchTime(LocalDateTime.now());
        request.setStatus("DISPATCHED");
        requestService.updateRequest(request);
        
        // Mark ambulance as unavailable
        ambulanceService.markAmbulanceUnavailable(availableAmbulance.getId());
    }
}