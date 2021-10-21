package org.eclipse.leshan.server.redis;

import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.californium.scandium.dtls.SessionStore;

import org.eclipse.californium.scandium.util.SecretUtil;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RedissonSessionStore implements SessionStore {

    private static final int DTLS_SESSION_EXP_TIME = 86400; // 60*60*24 = 86400 = 24 h
    private static final String DTLS_SESSION_PFX = "SS:SID";

    private static final Logger LOG = LoggerFactory.getLogger(RedissonSessionStore.class);
    private RedissonClient client;

    public RedissonSessionStore(String stringRedisUrl) {

        Config redissonConfig = new Config();
        redissonConfig.setCodec(new SerializationCodec());
        redissonConfig.useSingleServer().setAddress(stringRedisUrl);
        client = Redisson.create(redissonConfig);
    }

    @Override
    public void put(final DTLSSession session) {

        if (session == null || session.getSessionIdentifier().isEmpty()) {
            return;
        }

        String sessionId = session.getSessionIdentifier().getAsString();
        LOG.debug("Put DTLSSession id: {}", sessionId);

        DTLSSession sessionclone = new DTLSSession(session);

        RBucket<DTLSSession> bucket = client.getBucket(redisKey(sessionId));
        bucket.set(sessionclone, DTLS_SESSION_EXP_TIME, TimeUnit.SECONDS);

        SecretUtil.destroy(sessionclone);

    }

    @Override
    public DTLSSession get(final SessionId id) {

        String sessionId = id.getAsString();
        LOG.debug("Get DTLSSession id: {}", sessionId);
        
        DTLSSession session = null;
        
        try {
            session = (DTLSSession) client.getBucket(redisKey(sessionId)).get();
        } catch (Exception e) {
            LOG.error("Internal error, DTLS Session deserialization problem, consider using a different codec");
        }

        return session == null ? null : new DTLSSession(session);

    }

    @Override
    public void remove(final SessionId id) {

        String sessionId = id.getAsString();
        LOG.debug("Remove DTLSSession id: {}", sessionId);

        client.getBucket(redisKey(sessionId)).delete();

    }

    private String redisKey(String strKey) {
        return DTLS_SESSION_PFX + ":" + strKey;
    }

    public void close() {
        client.shutdown();
    }

}
