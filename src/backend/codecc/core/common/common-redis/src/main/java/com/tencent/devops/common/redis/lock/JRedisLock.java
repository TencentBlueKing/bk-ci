package com.tencent.devops.common.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

/**
 * Redis distributed lock implementation
 */
public class JRedisLock
{
    private static Logger logger = LoggerFactory.getLogger(JRedisLock.class);

    private static final Lock NO_LOCK = new Lock(new UUID(0L, 0L), 0L);
    public static final int ONE_SECOND = 1000;
    public static final int DEFAULT_EXPIRY_TIME_MILLIS = Integer.getInteger("lock.expiry.millis", 60 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRE_TIMEOUT_MILLIS = Integer.getInteger("lock.acquiry.millis", 10 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = Integer.getInteger("lock.acquiry.resolution.millis", 100);

    private final StringRedisTemplate redisTemplate;

    private final String lockKeyPath;

    private final int lockExpiryInMillis;
    private final int acquiryTimeoutInMillis;
    private final UUID lockUUID;

    private Lock lock = null;

    protected static class Lock
    {
        private UUID uuid;
        private long expiryTime;

        protected Lock(UUID uuid, long expiryTimeInMillis)
        {
            this.uuid = uuid;
            this.expiryTime = expiryTimeInMillis;
        }

        protected static Lock fromString(String text)
        {
            try
            {
                String[] parts = text.split(":");
                UUID theUUID = UUID.fromString(parts[0]);
                long theTime = Long.parseLong(parts[1]);
                return new Lock(theUUID, theTime);
            }
            catch (Exception any)
            {
                return NO_LOCK;
            }
        }

        public UUID getUUID()
        {
            return uuid;
        }

        public long getExpiryTime()
        {
            return expiryTime;
        }

        @Override
        public String toString()
        {
            return uuid.toString() + ":" + expiryTime;
        }

        boolean isExpired()
        {
            return getExpiryTime() < System.currentTimeMillis();
        }

        boolean isExpiredOrMine(UUID otherUUID)
        {
            return this.isExpired() || this.getUUID().equals(otherUUID);
        }
    }


    /**
     * Detailed constructor with default acquire timeout 10000 msecs and lock
     * expiration of 60000 msecs.
     *
     * @param redisTemplate
     * @param lockKey       lock key (ex. account:1, ...)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey)
    {
        this(redisTemplate, lockKey, DEFAULT_ACQUIRE_TIMEOUT_MILLIS, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    /**
     * Detailed constructor with default lock expiration of 60000 msecs.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis)
    {
        this(redisTemplate, lockKey, acquireTimeoutMillis, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    /**
     * Detailed constructor.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis     lock expiration in miliseconds (default: 60000 msecs)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis)
    {
        this(redisTemplate, lockKey, acquireTimeoutMillis, expiryTimeMillis, UUID.randomUUID());
    }

    /**
     * Detailed constructor.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis     lock expiration in miliseconds (default: 60000 msecs)
     * @param uuid                 unique identification of this lock
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis, UUID uuid)
    {
        this.redisTemplate = redisTemplate;
        this.lockKeyPath = lockKey;
        this.acquiryTimeoutInMillis = acquireTimeoutMillis;
        this.lockExpiryInMillis = expiryTimeMillis + 1;
        this.lockUUID = uuid;
        ;
    }

    /**
     * @return lock uuid
     */
    public UUID getLockUUID()
    {
        return lockUUID;
    }

    /**
     * @return lock key path
     */
    public String getLockKeyPath()
    {
        return lockKeyPath;
    }

    /**
     * Acquire lock.
     *
     * @return true if lock is acquired, false acquire timeouted
     */
    public synchronized boolean acquire()
    {
        return acquire(redisTemplate);
    }

    /**
     * Acquire lock.
     *
     * @param redisTemplate
     * @return true if lock is acquired, false acquire timeouted
     */
    protected synchronized boolean acquire(StringRedisTemplate redisTemplate)
    {
        int timeout = acquiryTimeoutInMillis;
        while (timeout >= 0)
        {

            final Lock newLock = asLock(System.currentTimeMillis() + lockExpiryInMillis);
            if (redisTemplate.opsForValue().setIfAbsent(lockKeyPath, newLock.toString()))
            {
                this.lock = newLock;
                return true;
            }

            final String currentValueStr = redisTemplate.opsForValue().get(lockKeyPath);
            final Lock currentLock = Lock.fromString(currentValueStr);
            if (currentLock.isExpiredOrMine(lockUUID))
            {
                String oldValueStr = redisTemplate.opsForValue().getAndSet(lockKeyPath, newLock.toString());
                if (oldValueStr != null && oldValueStr.equals(currentValueStr))
                {
                    this.lock = newLock;
                    return true;
                }
            }

            timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;

            try
            {
                Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
            }
            catch (InterruptedException e)
            {
                logger.error("Get lock[{}] failed!", this.getLockKeyPath(), e);
            }
        }

        logger.error("Get lock[{}] failed!", this.getLockKeyPath());
        return false;
    }

    /**
     * Renew lock.
     *
     * @return true if lock is acquired, false otherwise
     */
    public boolean renew()
    {
        final Lock lock = Lock.fromString(redisTemplate.opsForValue().get(lockKeyPath));
        if (!lock.isExpiredOrMine(lockUUID))
        {
            return false;
        }

        return acquire(redisTemplate);
    }

    /**
     * Acquired lock release.
     */
    public synchronized void release()
    {
        release(redisTemplate);
    }

    /**
     * Acquired lock release.
     *
     * @param redisTemplate
     */
    protected synchronized void release(StringRedisTemplate redisTemplate)
    {
        if (isLocked())
        {
            redisTemplate.opsForValue().getOperations().delete(lockKeyPath);
            this.lock = null;
        }
    }

    /**
     * Check if owns the lock
     *
     * @return true if lock owned
     */
    public synchronized boolean isLocked()
    {
        return this.lock != null;
    }

    /**
     * Returns the expiry time of this lock
     *
     * @return the expiry time in millis (or null if not locked)
     */
    public synchronized long getLockExpiryTimeInMillis()
    {
        return this.lock.getExpiryTime();
    }


    private Lock asLock(long expires)
    {
        return new Lock(lockUUID, expires);
    }


}
