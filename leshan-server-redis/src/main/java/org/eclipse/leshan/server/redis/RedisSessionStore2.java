/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
 package org.eclipse.leshan.server.redis;

import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.californium.scandium.dtls.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link SessionStore} implementation for Redis.
 * <p>
 * Warning : this version only support PSK.
 *
 */
public class RedisSessionStore2 implements SessionStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSessionStore2.class);

    private static final byte[] SESSION_ID_PREFIX = "SS:SID:SES:".getBytes(UTF_8); // session ID => session
    private static final byte[] IDENTITY_PREFIX = "SS:CLID:SID:".getBytes(UTF_8); // client identity => session ID

    // default redis expiration (ensuring entries won't stay forever)
    private int REDIS_EXP_SECONDS = 7 * 24 * 3600; // 7 days

    private final Pool<Jedis> pool;

    public RedisSessionStore2(Pool<Jedis> pool) {
        this.pool = pool;
    }

    @Override
    public void put(DTLSSession session) {

        byte[] clientIdKey;
        

        if (session.getSessionIdentifier() == null || session.getSessionIdentifier().isEmpty()) {
            LOG.warn("session store was called for an empty session !!!");
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Put DTLSSession id: {}, for client id: {}", session.getSessionIdentifier().getAsString(),
                    session.getPeerIdentity());
        }
        
        clientIdKey = toClientIdentityKey(( session.getPeerIdentity()).getName().getBytes());
        
        try (Jedis j = pool.getResource()) {
            DatagramWriter writer = new DatagramWriter();
            session.writeTo(writer);
            byte[] sessionId = session.getSessionIdentifier().getAsString().getBytes();
            j.setex(toSessionIdKey(sessionId), REDIS_EXP_SECONDS, writer.toByteArray());

            // Add an index by peer identity to try to keep a single session per device.
            // The implementation is not thread-safe but it should not be critical if a few sessions are not cleared

            byte[] previousSessionId = j.get(clientIdKey);
            if (previousSessionId != null && previousSessionId.length > 0
                    && !Arrays.equals(sessionId, previousSessionId)) {
                j.del(toSessionIdKey(previousSessionId));
            }
            j.setex(clientIdKey, REDIS_EXP_SECONDS, sessionId);

        } catch (RuntimeException e) {
            LOG.error("Error while storing DTLS session in cache", e);
        }
    }

    @Override
    public DTLSSession get(final SessionId id) {
        
        LOG.debug("Get DTLSSession id: {}", id.getAsString());
        
        try (Jedis j = pool.getResource()) {
            
            byte[] sessionIdKey = toSessionIdKey(id.getAsString().getBytes());
            j.expire(sessionIdKey, REDIS_EXP_SECONDS); // refresh expiration if the key exists

            byte[] st = j.get(sessionIdKey);
            if (st == null || st.length == 0) {
                return null;
            }
            DTLSSession session = DTLSSession.fromReader(new DatagramReader(st));

            return session;

        } catch (RuntimeException e) {
            LOG.error("Error while reading DTLS session from cache", e);
            return null;
        }
    }

    @Override
    public void remove(SessionId id) {
        
        LOG.debug("Remove DTLSSession id: {}", id.getAsString());
        
        try (Jedis j = pool.getResource()) {
            j.del(toSessionIdKey(id.getAsString().getBytes()));

            // We don't care about clearing the index here.
            // It will be overridden with the next session or removed by the TTL
        } catch (RuntimeException e) {
            LOG.error("Error while deleting DTLS session from cache", e);
        }
    }

    private byte[] toSessionIdKey(byte[] sessionId) {
        return toKey(SESSION_ID_PREFIX, sessionId);
    }

    private byte[] toClientIdentityKey(byte[] clientIdentity) {
        return toKey(IDENTITY_PREFIX, clientIdentity);
    }

    private byte[] toKey(byte[] prefix, byte[] key) {
        byte[] result = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(key, 0, result, prefix.length, key.length);
        return result;
    }

}