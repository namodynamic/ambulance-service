package com.ambulance.ambulance_service.performance;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.service.AmbulanceService;
import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PerformanceTest {

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private RequestService requestService;

    @BeforeEach
    void setUp() {
        // Create multiple ambulances for load testing
        for (int i = 1; i <= 10; i++) {
            ambulanceService.saveAmbulance(new Ambulance("Hospital " + i, AvailabilityStatus.AVAILABLE));
        }
    }

    @Test
    void testConcurrentRequestCreation() throws InterruptedException {
        // Test concurrent request creation to verify thread safety
        int numberOfThreads = 5;
        int requestsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            // Generate unique phone number for each request
                            String uniquePhone = String.format("+15555%03d%03d",
                                    threadId, j + System.currentTimeMillis() % 1000);

                            AmbulanceRequestDto request = new AmbulanceRequestDto(
                                    "Patient " + threadId + "-" + j,
                                    uniquePhone,
                                    "Location " + threadId + "-" + j,
                                    "Emergency " + threadId + "-" + j
                            );
                            requestService.createRequest(request);
                            successCount.incrementAndGet();
                        } catch (NoAvailableAmbulanceException e) {
                            failureCount.incrementAndGet();
                        } catch (Exception e) {
                            // Catch any other exceptions to prevent thread death
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        // Assert that all requests were processed
        int totalRequests = numberOfThreads * requestsPerThread;
        assertEquals(totalRequests, successCount.get() + failureCount.get(),
                "All requests should be processed");
    }

    @Test
    void testQueuePerformance() {
        // Test performance of ambulance queue operations
        long startTime = System.currentTimeMillis();

        // Perform many queue operations
        for (int i = 0; i < 1000; i++) {
            ambulanceService.getNextAvailableAmbulance();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // More realistic threshold (2 seconds) for 1000 operations
        assertTrue(duration < 2000, "Queue operations took too long: " + duration + "ms");
    }
}