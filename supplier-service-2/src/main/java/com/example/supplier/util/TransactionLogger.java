package com.example.supplier.util;

import com.example.supplier.model.TransactionLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionLogger {
    // Relative paths; will print absolute location at runtime
    private static final String LOG_DIR = "logs";
    private static final String TRANSACTION_LOG = LOG_DIR + "/transaction.log";
    private static final String PENDING_LOG = LOG_DIR + "/pending.log";

    private final ObjectMapper mapper;

    public TransactionLogger() {
        // Create and configure ObjectMapper with JavaTimeModule
        mapper = new ObjectMapper();
        // Register JavaTimeModule to handle Instant, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());
        // Optionally: write dates as ISO-strings rather than timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Print out to verify
        System.out.println("TransactionLogger: Configured ObjectMapper with JavaTimeModule. "
                + "WRITE_DATES_AS_TIMESTAMPS = "
                + mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        // Print working directory
        try {
            Path wd = Paths.get(".").toRealPath();
            System.out.println("TransactionLogger: Working directory is: " + wd.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("TransactionLogger: Could not resolve working directory: " + e.getMessage());
        }

        // Ensure log directory exists
        try {
            Path logDirPath = Paths.get(LOG_DIR);
            if (Files.notExists(logDirPath)) {
                Files.createDirectories(logDirPath);
                System.out.println("TransactionLogger: Created log directory at " + logDirPath.toAbsolutePath());
            } else {
                System.out.println("TransactionLogger: Log directory exists at " + logDirPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("TransactionLogger: Failed to create or check log directory: " + e);
        }

        // Ensure files exist
        ensureFileExists(TRANSACTION_LOG);
        ensureFileExists(PENDING_LOG);
    }

    private void ensureFileExists(String relativePath) {
        Path path = Paths.get(relativePath);
        try {
            if (Files.notExists(path)) {
                Files.createFile(path);
                System.out.println("TransactionLogger: Created file " + path.toAbsolutePath());
            } else {
                System.out.println("TransactionLogger: File already exists: " + path.toAbsolutePath() +
                        ", size=" + Files.size(path) + " bytes");
            }
        } catch (IOException e) {
            System.err.println("TransactionLogger: Error creating/checking file " + path + ": " + e);
        }
    }

    public synchronized void logTransaction(TransactionLogEntry entry) {
        String line;
        try {
            line = mapper.writeValueAsString(entry) + System.lineSeparator();
        } catch (Exception e) {
            System.err.println("TransactionLogger: Failed to serialize entry for transaction.log: " + e);
            return;
        }
        Path path = Paths.get(TRANSACTION_LOG);
        try {
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("TransactionLogger: Appended to " + path.toAbsolutePath() + ": " + entry);
            // Print new size
            try {
                long newSize = Files.size(path);
                System.out.println("TransactionLogger: transaction.log new size: " + newSize + " bytes");
            } catch (IOException ignore) {}
        } catch (IOException e) {
            System.err.println("TransactionLogger: Failed to write to transaction.log: " + e);
        }
    }

    public synchronized void logPending(TransactionLogEntry entry) {
        String line;
        try {
            line = mapper.writeValueAsString(entry) + System.lineSeparator();
        } catch (Exception e) {
            System.err.println("TransactionLogger: Failed to serialize entry for pending.log: " + e);
            return;
        }
        Path path = Paths.get(PENDING_LOG);
        try {
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("TransactionLogger: Appended to " + path.toAbsolutePath() + ": " + entry);
            try {
                long newSize = Files.size(path);
                System.out.println("TransactionLogger: pending.log new size: " + newSize + " bytes");
            } catch (IOException ignore) {}
        } catch (IOException e) {
            System.err.println("TransactionLogger: Failed to write to pending.log: " + e);
        }
    }

    public synchronized void removePending(String transactionId) {
        Path path = Paths.get(PENDING_LOG);
        try {
            if (Files.notExists(path)) {
                System.out.println("TransactionLogger: pending.log does not exist when removing pending.");
                return;
            }
            List<String> lines = Files.readAllLines(path);
            List<String> filtered = lines.stream()
                    .filter(line -> {
                        try {
                            TransactionLogEntry entry = mapper.readValue(line, TransactionLogEntry.class);
                            return !entry.getTransactionId().equals(transactionId);
                        } catch (IOException e) {
                            // keep malformed lines so we don't lose data
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
            Files.write(path, filtered, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            System.out.println("TransactionLogger: Removed pending entries for txn " + transactionId +
                    "; pending.log size now: " + Files.size(path) + " bytes");
        } catch (IOException e) {
            System.err.println("TransactionLogger: Failed to remove pending for " + transactionId + ": " + e);
        }
    }

    public synchronized List<TransactionLogEntry> readPendingTransactions() {
        List<TransactionLogEntry> result = new ArrayList<>();
        Path path = Paths.get(PENDING_LOG);
        if (Files.notExists(path)) {
            System.out.println("TransactionLogger: pending.log does not exist when reading pending.");
            return result;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    TransactionLogEntry entry = mapper.readValue(line, TransactionLogEntry.class);
                    result.add(entry);
                } catch (IOException e) {
                    System.err.println("TransactionLogger: Skipping malformed pending.log line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("TransactionLogger: Error reading pending.log: " + e);
        }
        System.out.println("TransactionLogger: readPendingTransactions found " + result.size() + " entries");
        return result;
    }
}
