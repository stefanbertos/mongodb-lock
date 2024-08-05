package com.example.demo.service;

import com.example.demo.dto.Lock;
import com.example.demo.exception.LockAcquisitionException;
import com.example.demo.exception.LockReleaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class LockService {

    private static final long LOCK_EXPIRY_DURATION_SECONDS = 60; // Define lock expiry duration

    @Autowired
    private MongoTemplate mongoTemplate;

    public void acquireLock(String documentId) throws LockAcquisitionException {
        Instant now = Instant.now();
        Instant lockExpiryTime = now.minusSeconds(LOCK_EXPIRY_DURATION_SECONDS);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(documentId)
                .andOperator(
                        Criteria.where("locked").is(false)
                                .orOperator(Criteria.where("lockTimestamp").lt(lockExpiryTime))
                )
        );

        Update update = new Update();
        update.set("locked", true);
        update.set("lockTimestamp", now);

        // Use upsert to create the document if it does not exist and lock it
        Lock lockedDocument = mongoTemplate.findAndModify(query, update,new FindAndModifyOptions().upsert(true), Lock.class);

        if (lockedDocument == null) {
            log.warn("Failed to acquire lock for documentId: {}", documentId);
            throw new LockAcquisitionException("Unable to acquire lock. Document is locked by another process or could not be created.");
        }

        log.info("Successfully acquired lock for documentId: {}", documentId);
    }

    public void releaseLock(String documentId) throws LockReleaseException {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(documentId).and("locked").is(true));

        Update update = new Update();
        update.set("locked", false);
        update.unset("lockTimestamp");

        // Use findAndModify to release the lock
        Lock unlockedDocument = mongoTemplate.findAndModify(query, update, Lock.class);

        if (unlockedDocument == null) {
            log.warn("Failed to release lock for documentId: {}", documentId);
            throw new LockReleaseException("Unable to release lock. Document does not exist or was not locked.");
        }

        log.info("Successfully released lock for documentId: {}", documentId);
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void cleanupExpiredLocks() {
        Instant now = Instant.now();
        Instant lockExpiryTime = now.minusSeconds(LOCK_EXPIRY_DURATION_SECONDS);

        Query query = new Query();
        query.addCriteria(Criteria.where("locked").is(true).and("lockTimestamp").lt(lockExpiryTime));

        long removedCount = mongoTemplate.remove(query, Lock.class).getDeletedCount();

        log.info("Expired locks cleaned up. Number of locks removed: {}", removedCount);
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startup() {
        mongoTemplate.save(new Lock());

    }
}
