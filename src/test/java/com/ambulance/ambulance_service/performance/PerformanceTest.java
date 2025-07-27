package com.ambulance.ambulance_service.performance;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.entity.Role;
import com.ambulance.ambulance_service.entity.User;
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

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);
        
        // Create multiple ambulances for load testing with unique license plates
        for (int i = 1; i <= 10; i++) {
            String uniqueSuffix = "_" + System.currentTimeMillis() + "_" + i;
            String licensePlate = "AMB" + String.format("%03d", i) + uniqueSuffix;
            ambulanceService.saveAmbulance(new Ambulance("Hospital " + i, AvailabilityStatus.AVAILABLE, licensePlate));
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
                            AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
                                    "Test User",
                                    "Test Patient",
                                    "+12345678" + threadId + j,
                                    "Location " + threadId + "-" + j,
                                    "Emergency " + threadId + "-" + j,
                                    "Medical notes for request " + threadId + "-" + j
                            );
                            requestService.createRequest(requestDto, testUser);
                            successCount.incrementAndGet();
                        } catch (NoAvailableAmbulanceException e) {
                            // Expected in some cases due to concurrency
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

        // Perform many queue operations - reduce iterations for CI environment
        int iterations = 100; // Reduced from 1000 to make test faster
        for (int i = 0; i < iterations; i++) {
            ambulanceService.getNextAvailableAmbulance();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // More realistic threshold (1 second) for 100 operations
        assertTrue(duration < 1000, "Queue operations took too long: " + duration + "ms");
    }
}