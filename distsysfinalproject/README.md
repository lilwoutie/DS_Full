Broker-side implementation of two phase commit protocol that ensures distributed orders across restaurant services satisfy ACID properties. The broker groups cart items per restaurant, sends Prepare/Commit/Abort requests, logs durable state in H2, and persists the final order only upon global commit.

1. Configuration

File: src/main/resources/application.properties

    H2 setup for local persistence of logs and orders:

        spring.datasource.url=jdbc:h2:mem:brokerdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

        spring.jpa.hibernate.ddl-auto=update

        spring.h2.console.enabled=true, path /h2-console

    Logging levels: DEBUG for com.fooddelivery.brokerapplication to show 2PC steps.

    Restaurant endpoints:
    restaurant.service.urls=http://localhost:8081/transaction,http://localhost:8082/transaction
    An array where index = restaurantId−1.

2. Model / Entities

    CartItem (CartItem.java)

        JPA entity already present; fields: Long id, Long mealId, int restaurantId, String name, double price, int quantity.

        Used for 2PC messaging (sent to restaurants) and final-order persistence.

    PrepareRequest (PrepareRequest.java, new)

        DTO with String transactionId and List<CartItem> items.

        Sent in Phase 1 to each restaurant: JSON body.

    TransactionLog (TransactionLog.java, new)

        JPA entity:

            id = transactionId (UUID string) PRIMARY KEY.

            status: TransactionStatus enum (INIT, PREPARED, COMMITTING, COMMITTED, ABORTED).

            createdAt, updatedAt: timestamps.

        Persists global transaction state at each transition for durability and potential recovery.

    ParticipantLog (ParticipantLog.java, new)

        JPA entity:

            Auto-generated Long id.

            transactionId: foreign key to TransactionLog.id.

            participant: identifier (base URL string).

            status: ParticipantStatus enum (PARTICIPATING, PREPARED, COMMITTED, ABORTED).

            createdAt, updatedAt.

        Records per-restaurant state, enabling commit/abort tracking and recovery.

    Enums (TransactionStatus.java, ParticipantStatus.java, new)

        Define allowed states in 2PC protocol; stored as strings for readability.

3. Repositories

    TransactionLogRepository (TransactionLogRepository.java, new)

        extends JpaRepository<TransactionLog, String>.

        Used to save/update global transaction entries.

    ParticipantLogRepository (ParticipantLogRepository.java, new)

        extends JpaRepository<ParticipantLog, Long>.

        Adds List<ParticipantLog> findByTransactionId(String transactionId).

        Used to fetch all participants for commit/abort.

4. Service: Coordinator Logic

File: TwoPhaseCommitService.java (new)

    Core method: executeTransaction(List<CartItem> cartItems)

        Generate a UUID transactionId; log start.

        Persist TransactionLog with status INIT.

        Group cart items by restaurantId.

        Phase 1 – Prepare for each restaurant:

            Determine base URL via restaurant.service.urls array (determineRestaurantUrl).

            Persist a new ParticipantLog in status PARTICIPATING.

            Build PrepareRequest(transactionId, itemsForThisRestaurant).

            Send POST {baseUrl}/prepare?transactionId=... with JSON body.

            On HTTP 200 → mark ParticipantLog PREPARED; else or exception → mark ABORTED, set abortFlag.

            Persist updated ParticipantLog; if abortFlag, update global TransactionLog to ABORTED and break.

        Phase 2 – Commit or Abort:

            If no abortFlag:

                Update global to PREPARED then COMMITTING (persisting each).

                Fetch all ParticipantLog entries by transactionId; for each with status PREPARED:

                    Send POST {participant}/commit?transactionId=....

                    On success → mark COMMITTED; on exception → mark ABORTED and set abortFlag.

                    Persist updated ParticipantLog.

                After loop: if no abortFlag → global → COMMITTED; else → global → ABORTED.

            If abortFlag from Phase 1 or commit-phase failure:

                Log entering Abort phase.

                Fetch all participants; for any with status PREPARED:

                    Send POST {participant}/abort?transactionId=...; log exceptions but continue.

                    Mark participant ABORTED and persist.

                Global already set to ABORTED in Phase 1 or commit-phase.

        Final logging: log transaction finished with status.

        Exception: if global status is ABORTED, throw RuntimeException("Distributed transaction ... aborted"), so controller can handle.

    Helper: determineRestaurantUrl(int restaurantId)

        Validates restaurantServiceUrls length and restaurantId bounds; returns correct base URL for calls.

    Logging: SLF4J at INFO/DEBUG/WARN/ERROR for all steps.

    HTTP calls: via injected RestTemplate, with configured timeouts to detect unresponsiveness.

    Durable Logging: JPA saves ensure visibility in H2 console and durability within application lifetime.

5. RestTemplate Configuration

    Bean Definition: e.g., in a @Configuration class

    Role:

        Ensures HTTP calls to restaurant services time out rather than hang indefinitely.

        Helps broker detect unresponsive participants and trigger abort.

6. Controllers & Integration
6.1 CheckoutController

File: CheckoutController.java (modified)

    GET /checkout: show form if cart not empty.

    POST /checkout:

        Retrieve List<CartItem> and total.

        Log start: logger.info("Starting checkout for {} items, total={}", ...).

        Call twoPhaseCommitService.executeTransaction(items).

            On success: build Order entity, persist via OrderService, clear cart, redirect to confirmation.

            On exception: catch, log warning, add errorMessage to model, return to checkout page (cart intact).

    GET /confirmation: display order total.

Role in 2PC:

    Integrates coordinator into user flow: only persist order if 2PC succeeds.

    Provides user feedback on failure without losing cart data.

Role:

    Ensures broker shows current menu and correct meal IDs/prices for 2PC.

6.3 AdminController

File: AdminController.java (modified)

    GET /admin/orders: fetches persisted orders from OrderService and adds to model for display.

    extension: also fetch TransactionLog and ParticipantLog entries and add to model, so admin can view 2PC history alongside orders.

Role:

    Allows admin to see final orders and, if extended, transaction logs for auditing distributed transactions.


7. Logging & Inspection

    Console Logs (SLF4J):

        2PC start, each Prepare/Commit/Abort request and response, status updates, errors/timeouts, final status.

        Checkout start, success or abort warning.

    H2 Console (http://localhost:8080/h2-console):

        Tables:

            TRANSACTION_LOG: shows each transactionId with its statuses and timestamps.

            PARTICIPANT_LOG: shows per-participant entries linked to transactionIds, statuses, timestamps.

            ORDERS, CART_ITEM: persisted orders after successful transactions.

        Use to verify flows and for manual recovery analysis if needed.

8. Failure Handling & Fault Tolerance

    Prepare Phase:

        If a restaurant responds non-200 or call throws exception (timeout, connection error, malformed response), treat as vote NO → mark that participant ABORTED, set abortFlag, update global to ABORTED, break out of prepare loop.

    Commit Phase:

        If all participants PREPARED: broker updates global to COMMITTING, then for each PREPARED participant sends commit. If commit call fails (exception), mark that participant ABORTED, set abortFlag.

        After loop: if abortFlag, global → ABORTED, else → COMMITTED.

        If abortFlag, in Abort phase broker notifies any previously PREPARED participants to abort.

    Abort Phase:

        For participants with status PREPARED (i.e., reserved earlier), send POST /abort?transactionId=... to instruct rollback.

        Errors during abort calls are logged but do not crash broker.

    RestTemplate timeouts detect unresponsive services.

    Broker crash recovery: persisted logs allow manual inspection; automated recovery can be added later.

    User-level handling: CheckoutController catches exception, shows error message; cart remains intact so user can retry.

9. How to Inspect & Test

    Start Broker:

        Run BrokerApplication on port 8080.

    Start Restaurant Services (with their 2PC endpoints) on configured ports (e.g., 8081, 8082).

    H2 Console:

        Navigate to http://localhost:8080/h2-console, connect to JDBC URL jdbc:h2:mem:brokerdb, inspect tables.

    Place Orders via UI:

        Browse to broker home, add items from one or more restaurants, proceed to checkout.

        Observe console logs for 2PC steps.

        On success: order saved; inspect ORDERS table.

        On failure (e.g., stop one restaurant), observe abort logs; checkout page shows error.

    Simulate Failures:

        Stop a restaurant before Prepare → broker aborts.

        Stop a restaurant after Prepare but before Commit → on commit attempt, broker detects failure → aborts, notifies others.

        Crash broker after Prepare but before Commit: with persistent logs (file-based H2), on restart you can inspect logs; manual steps could resume commit.

    Admin View:

        Access /admin/orders to see persisted orders. Extend to view TRANSACTION_LOG and PARTICIPANT_LOG for auditing.
