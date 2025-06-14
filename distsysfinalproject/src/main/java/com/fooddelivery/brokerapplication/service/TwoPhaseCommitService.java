package com.fooddelivery.brokerapplication.service;

import com.fooddelivery.brokerapplication.dto.PrepareItemDTO;
import com.fooddelivery.brokerapplication.model.CartItem;
import com.fooddelivery.brokerapplication.model.ParticipantLog;
import com.fooddelivery.brokerapplication.model.ParticipantStatus;
import com.fooddelivery.brokerapplication.model.TransactionLog;
import com.fooddelivery.brokerapplication.model.TransactionStatus;
import com.fooddelivery.brokerapplication.repository.ParticipantLogRepository;
import com.fooddelivery.brokerapplication.repository.TransactionLogRepository;
import com.fooddelivery.brokerapplication.model.PrepareRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;


import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementing the Two-Phase Commit (2PC) protocol as the coordinator (broker).
 *
 * - Groups CartItem by restaurantId and sends a single prepare per restaurant, with all items for that restaurant.
 * - Uses PrepareRequest DTO to send transactionId and items list.
 * - Persists TransactionLog and ParticipantLog entries in H2 (or Azure SQL) for durability.
 * - Logs each significant step to console (SLF4J).
 *
 * Configuration:
 * - Expects property "restaurant.service.base-urls" listing base URLs (comma-separated)
 *   for restaurant services, e.g. "http://localhost:8081,http://localhost:8082".
 *   Array index corresponds to restaurantId - 1.
 * - Appends specific paths for 2PC endpoints: "/transaction/prepare", "/transaction/commit", "/transaction/abort".
 */
@Service
public class TwoPhaseCommitService {

    private static final Logger logger = LoggerFactory.getLogger(TwoPhaseCommitService.class);

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private ParticipantLogRepository participantLogRepository;

    @Autowired
    private RestTemplate restTemplate;

    public TwoPhaseCommitService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Base URLs for restaurant services, configured in application.properties:
     * restaurant.service.base-urls=http://localhost:8081,http://localhost:8082
     * Array index 0 → restaurantId=1, index 1 → restaurantId=2, etc.
     */
    @Value("${restaurant.service.base-urls}")
    private String[] restaurantServiceBaseUrls;

    /**
     * Expected number of restaurants. Adjust if you have more restaurants.
     * For this assignment with two restaurants, set to 2.
     */
    private static final int EXPECTED_RESTAURANT_COUNT = 2;

    /**
     * Validate configuration on startup: ensure we have at least EXPECTED_RESTAURANT_COUNT base URLs.
     */
    @PostConstruct
    public void validateRestaurantBaseUrls() {
        if (restaurantServiceBaseUrls == null) {
            logger.error("Property 'restaurant.service.base-urls' is not set or empty.");
            throw new IllegalStateException("Missing configuration: restaurant.service.base-urls");
        }
        if (restaurantServiceBaseUrls.length < EXPECTED_RESTAURANT_COUNT) {
            logger.error("Insufficient restaurant.service.base-urls entries: expected at least {}, but found {}",
                    EXPECTED_RESTAURANT_COUNT, restaurantServiceBaseUrls.length);
            throw new IllegalStateException(String.format(
                    "Expected at least %d restaurant.service.base-urls entries but found %d",
                    EXPECTED_RESTAURANT_COUNT, restaurantServiceBaseUrls.length));
        }
        logger.info("Configured restaurant service base URLs: {}", Arrays.toString(restaurantServiceBaseUrls));
    }

    /**
     * Execute a distributed transaction for the given list of CartItem.
     * Groups items per restaurant and performs 2PC across restaurants.
     *
     * @param cartItems List of CartItem from the user’s session/cart.
     * @throws RuntimeException if the transaction must be aborted.
     */
    public void executeTransaction(List<CartItem> cartItems) {

        //DEBUG STATEMENT
        for (CartItem item : cartItems) {
            logger.info("[2PC] CartItem: mealId={}, quantity={}, restaurantId={}",
                    item.getMealId(), item.getQuantity(), item.getRestaurantId());
        }


        // 1) Generate unique transaction ID
        String txId = UUID.randomUUID().toString();
        logger.info("[2PC][{}] Starting distributed transaction for {} cart item(s)", txId, cartItems.size());

        // 2) Persist initial TransactionLog with status INIT
        TransactionLog txLog = new TransactionLog(txId, TransactionStatus.INIT);
        transactionLogRepository.save(txLog);
        logger.info("[2PC][{}] Persisted TransactionLog status={}", txId, txLog.getStatus());

        // 3) Group items by restaurantId
        Map<Integer, List<CartItem>> itemsByRestaurant = cartItems.stream()
                .collect(Collectors.groupingBy(CartItem::getRestaurantId));

        boolean abortFlag = false;

        // 4) Phase 1: Prepare (voting) per restaurant
        for (Map.Entry<Integer, List<CartItem>> entry : itemsByRestaurant.entrySet()) {
            int restId = entry.getKey();
            List<CartItem> itemsForThisRestaurant = entry.getValue();

            // Determine restaurant base URL
            String restaurantBase = determineRestaurantBaseUrl(restId);
            String prepareUrl = restaurantBase + "/transaction/prepare/" + txId;

            logger.info("[2PC][{}] Sending PREPARE to restaurantId={} at {} with {} item(s)",
                    txId, restId, prepareUrl, itemsForThisRestaurant.size());

            // Persist ParticipantLog for this restaurant
            ParticipantLog pLog = new ParticipantLog(txId, restaurantBase);
            pLog.setStatus(ParticipantStatus.PARTICIPATING);
            pLog.setCreatedAt(LocalDateTime.now());
            pLog.setUpdatedAt(LocalDateTime.now());
            participantLogRepository.save(pLog);
            logger.debug("[2PC][{}] Created ParticipantLog id={} for restaurantId={} status={}",
                    txId, pLog.getId(), restId, pLog.getStatus());

            // Build PrepareRequest containing transactionId and items
            List<PrepareItemDTO> prepareItems = itemsForThisRestaurant.stream()
                    .map(item -> new PrepareItemDTO(
                            item.getMealId(),
                            item.getQuantity()))
                    .collect(Collectors.toList());

            PrepareRequest prepareRequest = new PrepareRequest(txId, prepareItems);

            //DEBUG PRINT
            // Log JSON body
            try {
                ObjectMapper mapper = new ObjectMapper();
                String jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prepareRequest);
                logger.info("[2PC][{}] Outgoing PREPARE JSON body to restaurantId {}:\n{}", txId, restId, jsonBody);
            } catch (JsonProcessingException e) {
                logger.error("[2PC][{}] Failed to serialize PrepareRequest for logging", txId, e);
            }

            //PrepareRequest prepareRequest = new PrepareRequest(txId, itemsForThisRestaurant);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<PrepareRequest> requestEntity = new HttpEntity<>(prepareRequest, headers);

                ResponseEntity<Void> response = restTemplate.postForEntity(prepareUrl, requestEntity, Void.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    // Participant voted YES (prepared)
                    pLog.setStatus(ParticipantStatus.PREPARED);
                    logger.info("[2PC][{}] Restaurant {} PREPARED successfully for {} items",
                            txId, restaurantBase, itemsForThisRestaurant.size());
                } else {
                    // Non-200: treat as NO vote
                    pLog.setStatus(ParticipantStatus.ABORTED);
                    abortFlag = true;
                    logger.warn("[2PC][{}] Restaurant {} returned status {} => voting NO",
                            txId, restaurantBase, response.getStatusCode());
                }
            } catch (Exception ex) {
                // Exception (timeout, connection error, malformed response): treat as NO vote
                pLog.setStatus(ParticipantStatus.ABORTED);
                abortFlag = true;
                logger.error("[2PC][{}] Exception during PREPARE at {}: {} => voting NO",
                        txId, restaurantBase, ex.getMessage());
            }

            // Persist updated ParticipantLog
            pLog.setUpdatedAt(LocalDateTime.now());
            participantLogRepository.save(pLog);
            logger.debug("[2PC][{}] Updated ParticipantLog id={} to status={}", txId, pLog.getId(), pLog.getStatus());

            if (abortFlag) {
                // Early abort: update global transaction status to ABORTED, persist, and break
                txLog.setStatus(TransactionStatus.ABORTED);
                txLog.setUpdatedAt(LocalDateTime.now());
                transactionLogRepository.save(txLog);
                logger.warn("[2PC][{}] Transaction aborting early due to prepare failure at restaurant {}", txId, restaurantBase);
                break;
            }
        } // end Prepare per restaurant

        // 5) Phase 2: Commit or Abort
        if (!abortFlag) {
            // All restaurants prepared successfully
            txLog.setStatus(TransactionStatus.PREPARED);
            txLog.setUpdatedAt(LocalDateTime.now());
            transactionLogRepository.save(txLog);
            logger.info("[2PC][{}] All participants PREPARED; moving to COMMITTING", txId);

            // Update status to COMMITTING
            txLog.setStatus(TransactionStatus.COMMITTING);
            txLog.setUpdatedAt(LocalDateTime.now());
            transactionLogRepository.save(txLog);
            logger.info("[2PC][{}] Persisted TransactionLog status={}", txId, txLog.getStatus());

            // Retrieve participant logs for this transaction
            List<ParticipantLog> participants = participantLogRepository.findByTransactionId(txId);
            for (ParticipantLog p : participants) {
                if (p.getStatus() == ParticipantStatus.PREPARED) {
                    // Send commit
                    String commitUrl = p.getParticipant() + "/transaction/commit/" + txId;
                    logger.info("[2PC][{}] Sending COMMIT to {}", txId, commitUrl);
                    try {
                        restTemplate.postForEntity(commitUrl, null, Void.class);
                        p.setStatus(ParticipantStatus.COMMITTED);
                        logger.info("[2PC][{}] Participant {} COMMITTED", txId, p.getParticipant());
                    } catch (Exception ex) {
                        // Commit failure: mark aborted. In production, consider retry logic.
                        p.setStatus(ParticipantStatus.ABORTED);
                        abortFlag = true;
                        logger.error("[2PC][{}] Exception during COMMIT at {}: {}",
                                txId, p.getParticipant(), ex.getMessage());
                    }
                    p.setUpdatedAt(LocalDateTime.now());
                    participantLogRepository.save(p);
                } else {
                    logger.debug("[2PC][{}] Skipping COMMIT for participant {} with status {}",
                            txId, p.getParticipant(), p.getStatus());
                }
            }

            // Finalize global transaction status
            if (!abortFlag) {
                txLog.setStatus(TransactionStatus.COMMITTED);
                logger.info("[2PC][{}] Transaction COMMITTED successfully", txId);
            } else {
                txLog.setStatus(TransactionStatus.ABORTED);
                logger.warn("[2PC][{}] Transaction aborted during commit phase", txId);
            }
            txLog.setUpdatedAt(LocalDateTime.now());
            transactionLogRepository.save(txLog);

        } else {
            // Abort phase: notify any prepared restaurants to abort
            logger.info("[2PC][{}] Entering ABORT phase: notifying prepared participants", txId);
            List<ParticipantLog> participants = participantLogRepository.findByTransactionId(txId);
            for (ParticipantLog p : participants) {
                if (p.getStatus() == ParticipantStatus.PREPARED) {
                    String abortUrl = p.getParticipant() + "/transaction/rollback/" + txId;
                    logger.info("[2PC][{}] Sending ABORT to {}", txId, abortUrl);
                    try {
                        restTemplate.postForEntity(abortUrl, null, Void.class);
                        logger.info("[2PC][{}] Participant {} ABORTED", txId, p.getParticipant());
                    } catch (Exception ex) {
                        logger.error("[2PC][{}] Exception during ABORT at {}: {}",
                                txId, p.getParticipant(), ex.getMessage());
                    }
                    p.setStatus(ParticipantStatus.ABORTED);
                    p.setUpdatedAt(LocalDateTime.now());
                    participantLogRepository.save(p);
                } else {
                    logger.debug("[2PC][{}] No ABORT needed for participant {} with status {}",
                            txId, p.getParticipant(), p.getStatus());
                }
            }
            // Global status already set to ABORTED in prepare phase
        }

        logger.info("[2PC][{}] Transaction finished with status {}", txId, txLog.getStatus());

        // If aborted, throw exception so calling code can handle it
        if (txLog.getStatus() == TransactionStatus.ABORTED) {
            throw new RuntimeException("Distributed transaction " + txId + " aborted");
        }
    }

    /**
     * Determine the base URL for the restaurant service handling this restaurantId.
     *
     * @param restaurantId the ID of the restaurant (1-based)
     * @return base URL for that restaurant’s endpoints (e.g., "http://localhost:8081")
     * @throws IllegalArgumentException if restaurantId is invalid or misconfigured
     */
    private String determineRestaurantBaseUrl(int restaurantId) {
        if (restaurantServiceBaseUrls == null
                || restaurantId <= 0
                || restaurantId > restaurantServiceBaseUrls.length) {
            String msg = String.format("Invalid restaurantId %d or misconfigured restaurant.service.base-urls", restaurantId);
            logger.error("[2PC] " + msg);
            throw new IllegalArgumentException(msg);
        }
        // Arrays are 0-based, restaurant IDs assumed 1-based
        return restaurantServiceBaseUrls[restaurantId - 1];
    }
}
