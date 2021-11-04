package org.eclipse.leshan.integration;

import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.DataStreamReader;
import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;

import org.eclipse.californium.elements.util.SerialExecutor;
import org.eclipse.californium.elements.util.SerializationUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.ConnectionListener;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.ClientHello;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.Random;
import org.eclipse.californium.scandium.dtls.Record;
import org.eclipse.californium.scandium.dtls.ResumptionSupportingConnectionStore;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.californium.scandium.dtls.SessionListener;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyTest {
    @Test
    public void serialize_Connection() throws IOException, ClassNotFoundException {
        InetSocketAddress peerAddress = new InetSocketAddress(0);

        Connection connection = new Connection(peerAddress);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(connection);
        objectOutputStream.flush();
        objectOutputStream.close();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Connection connectonRead = (Connection) objectInputStream.readObject();

        assertEquals(connection, connectonRead);
    }

    @Test
    public void serialize_LeastRecentlyUsedCache() throws IOException, ClassNotFoundException {
        InetSocketAddress peerAddress = new InetSocketAddress(0);

        Connection connection = new Connection(peerAddress);

        LeastRecentlyUsedCache leastRecentlyUsedCache = new LeastRecentlyUsedCache();
        leastRecentlyUsedCache.add("connection", connection);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(leastRecentlyUsedCache);
        objectOutputStream.flush();
        objectOutputStream.close();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        LeastRecentlyUsedCache leastRecentlyUsedCacheRead = (LeastRecentlyUsedCache)objectInputStream.readObject();

        Object connectionRead = leastRecentlyUsedCacheRead.get("connection");

        assertEquals(connection, connectionRead);




    }

    public static final class Connection implements Serializable {

        private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);
        private static final Logger LOGGER_OWNER = LoggerFactory.getLogger(LOGGER.getName() + ".owner");

        private final AtomicReference<Handshaker> ongoingHandshake = new AtomicReference<Handshaker>();
        private final SessionListener sessionListener = new ConnectionSessionListener();

        private volatile ConnectionListener connectionListener;

        /**
         * Identifier of the Client Hello used to start the handshake. Maybe
         * {@code null}, for client side connections.
         *
         * Note: used outside of the serial-execution!
         *
         * @since 3.0
         */
        private volatile ClientHelloIdentifier startingHelloClient;

        private volatile DTLSContext establishedDtlsContext;

        // Used to know when an abbreviated handshake should be initiated
        private volatile boolean resumptionRequired;

        /**
         * Expired real time nanoseconds of the last message send or received.
         */
        private long lastMessageNanos;
        private long lastPeerAddressNanos;
        private SerialExecutor serialExecutor;
        private InetSocketAddress peerAddress;
        private InetSocketAddress router;
        private ConnectionId cid;

        /**
         * Root cause of alert.
         *
         * For some case, the root cause may be hidden and replaced by a general
         * cause when sending an alert message. This keeps the root cause for
         * internal analysis.
         *
         * @since 2.5
         */
        private AlertMessage rootCause;

        /**
         * Creates a new connection to a given peer.
         *
         * @param peerAddress the IP address and port of the peer the connection exists with
         * @throws NullPointerException if the peer address is {@code null}
         */
        public Connection(InetSocketAddress peerAddress) {
            if (peerAddress == null) {
                throw new NullPointerException("Peer address must not be null");
            } else {
                long now = ClockUtil.nanoRealtime();
                this.peerAddress = peerAddress;
                this.lastPeerAddressNanos = now;
                this.lastMessageNanos = now;
            }
        }

        /**
         * Update connection state.
         *
         * Calls {@link ConnectionListener#updateExecution(Connection)}.
         *
         * @since 2.4
         */
        public void updateConnectionState() {
            ConnectionListener listener = this.connectionListener;
            if (listener != null) {
//                listener.updateExecution(this);
            }
        }

        /**
         * Set connector's context.
         *
         * @param executor executor to be used for {@link SerialExecutor}.
         * @param listener connection listener.
         * @return this connection
         * @throws IllegalStateException if the connection is already executing
         * @since 3.0 (combines previous setExecutor and setExecutionListener)
         */
        public Connection setConnectorContext(Executor executor, ConnectionListener listener) {
            if (isExecuting()) {
                throw new IllegalStateException("Executor already available!");
            }
            this.serialExecutor = new SerialExecutor(executor);
            this.connectionListener = listener;
            if (listener == null) {
                serialExecutor.setExecutionListener(null);
            } else {
                serialExecutor.setExecutionListener(new SerialExecutor.ExecutionListener() {

                    @Override
                    public void beforeExecution() {
//                        connectionListener.beforeExecution(this);
                    }

                    @Override
                    public void afterExecution() {
//                        connectionListener.afterExecution(Connection.this);
                    }
                });
            }
            return this;
        }

        /**
         * Gets the serial executor assigned to this connection.
         *
         * @return serial executor. May be {@code null}, if the connection is
         *         restored on startup.
         */
        public SerialExecutor getExecutor() {
            return serialExecutor;
        }

        /**
         * Checks, if the connection has a executing serial executor.
         *
         * @return {@code true}, if the connection has an executing serial executor.
         *         {@code false}, if no serial executor is available, or the
         *         executor is shutdown.
         */
        public boolean isExecuting() {
            return serialExecutor != null && !serialExecutor.isShutdown();
        }

        /**
         * Get session listener of connection.
         *
         * @return session listener.
         */
        public final SessionListener getSessionListener() {
            return sessionListener;
        }

        /**
         * Checks whether this connection is either in use on this node or can be resumed by peers interacting with
         * this node.
         * <p>
         * A connection that is not active is currently being negotiated by means of the <em>ongoingHandshake</em>.
         *
         * @return {@code true} if this connection either already has an established session or
         *         contains a session that it can be resumed from.
         */
        public boolean isActive() {
            return establishedDtlsContext != null;
        }

        /**
         * Check, if this connection expects connection ID for incoming records.
         *
         * @return {@code true}, if connection ID is expected, {@code false},
         *         otherwise
         */
        public boolean expectCid() {
            DTLSContext context = getDtlsContext();
            return context != null && ConnectionId.useConnectionId(context.getReadConnectionId());
        }

        /**
         * Gets the connection id.
         *
         * @return the cid
         */
        public ConnectionId getConnectionId() {
            return cid;
        }

        /**
         * Sets the connection id.
         *
         * @param cid the connection id
         */
        public void  setConnectionId(ConnectionId cid) {
            this.cid = cid;
            updateConnectionState();
        }

        /**
         * Get real time nanoseconds of last
         * {@link #updatePeerAddress(InetSocketAddress)}.
         *
         * @return real time nanoseconds
         * @see ClockUtil#nanoRealtime()
         */
        public long getLastPeerAddressNanos() {
            return lastPeerAddressNanos;
        }

        /**
         * Gets the address of this connection's peer.
         *
         * @return the address
         */
        public InetSocketAddress getPeerAddress() {
            return peerAddress;
        }

        /**
         * Update the address of this connection's peer.
         *
         * If the new address is {@code null}, an ongoing handshake is failed. A
         * non-null address could only be applied, if the dtls context is established.
         *
         * Note: to keep track of the associated address in the connection store,
         * this method must not be called directly. It must be called by calling
         * {@link ResumptionSupportingConnectionStore#update(Connection, InetSocketAddress)}
         * or
         * {@link ResumptionSupportingConnectionStore#remove(Connection, boolean)}.
         *
         * @param peerAddress the address of the peer
         * @throws IllegalArgumentException if the address should be updated with a
         *             non-null value without an established dtls context.
         */
        public void updatePeerAddress(InetSocketAddress peerAddress) {
            if (!equalsPeerAddress(peerAddress)) {
                if (establishedDtlsContext == null && peerAddress != null) {
                    throw new IllegalArgumentException("Address change without established dtls context is not supported!");
                }
                this.lastPeerAddressNanos = ClockUtil.nanoRealtime();
                InetSocketAddress previous = this.peerAddress;
                this.peerAddress = peerAddress;
                if (peerAddress == null) {
                    final Handshaker pendingHandshaker = getOngoingHandshake();
                    if (pendingHandshaker != null) {
                        if (establishedDtlsContext == null
                                || pendingHandshaker.getDtlsContext() != establishedDtlsContext) {
                            // this will only call the listener, if no other cause was set before!
                            pendingHandshaker.handshakeFailed(new IOException(
                                    StringUtil.toDisplayString(previous) + " address reused during handshake!"));
                        }
                    }
                } else {
                    // only update mdc, if address is changed to new one.
                    updateConnectionState();
                }
            }
        }

        /**
         * Check, if the provided address is the peers address.
         *
         * @param peerAddress provided peer address
         * @return {@code true}, if the addresses are equal
         */
        public boolean equalsPeerAddress(InetSocketAddress peerAddress) {
            if (this.peerAddress == peerAddress) {
                return true;
            } else if (this.peerAddress == null) {
                return false;
            }
            return this.peerAddress.equals(peerAddress);
        }

        /**
         * Gets the address of this connection's router.
         *
         * @return the address of the router
         * @since 2.5
         */
        public InetSocketAddress getRouter() {
            return router;
        }

        /**
         * Sets the address of this connection's router.
         *
         * @param router the address of the router
         * @since 2.5
         */
        public void setRouter(InetSocketAddress router) {
            if (this.router != router && (this.router == null || !this.router.equals(router))) {
                this.router = router;
                updateConnectionState();
            }
        }

        /**
         * Get endpoint context for writing messages.
         *
         * @param attributes initial attributes
         * @return endpoint context for writing messages.
         * @throws IllegalStateException if dtls context is not established
         * @since 3.0
         */
        public DtlsEndpointContext getWriteContext(MapBasedEndpointContext.Attributes attributes) {
            if (establishedDtlsContext == null) {
                throw new IllegalStateException("DTLS context must be established!");
            }
            establishedDtlsContext.addWriteEndpointContext(attributes);
            if (router != null) {
                attributes.add(DtlsEndpointContext.KEY_VIA_ROUTER, "dtls-cid-router");
            }
            DTLSSession session = establishedDtlsContext.getSession();
            return new DtlsEndpointContext(peerAddress, session.getHostName(), session.getPeerIdentity(), attributes);
        }

        /**
         * Get endpoint context for reading messages.
         * @param attributes initial attributes
         * @param recordsPeer peer address of record. Only used, if connection has
         *            no {@link #peerAddress}.
         *
         * @return endpoint context for reading messages.
         * @since 3.0
         */
        public DtlsEndpointContext getReadContext(MapBasedEndpointContext.Attributes attributes, InetSocketAddress recordsPeer) {
            if (establishedDtlsContext == null) {
                throw new IllegalStateException("DTLS context must be established!");
            }
            establishedDtlsContext.addReadEndpointContext(attributes);
            if (router != null) {
                attributes.add(DtlsEndpointContext.KEY_VIA_ROUTER, "dtls-cid-router");
            }
            if (peerAddress != null) {
                recordsPeer = peerAddress;
            }
            DTLSSession session = establishedDtlsContext.getSession();
            return new DtlsEndpointContext(recordsPeer, session.getHostName(), session.getPeerIdentity(), attributes);
        }

        /**
         * Gets the session containing the connection's <em>current</em> state.
         *
         * This is the session of the {@link #establishedDtlsContext}, if not
         * {@code null}, or the session negotiated in the {@link #ongoingHandshake}.
         *
         * @return the <em>current</em> session, or {@code null}, if no session exists
         */
        public DTLSSession getSession() {
            DTLSContext dtlsContext = getDtlsContext();
            if (dtlsContext != null) {
                return dtlsContext.getSession();
            }
            return null;
        }

        /**
         * Gets the DTLS session id of an already established DTLS context that
         * exists with this connection's peer.
         *
         * @return the session id, or {@code null}, if no DTLS context has been
         *         established (yet)
         * @since 3.0
         */
        public SessionId getEstablishedSessionIdentifier() {
            DTLSContext context = getEstablishedDtlsContext();
            return context == null ? null : context.getSession().getSessionIdentifier();
        }

        /**
         * Gets the DTLS session of an already established DTLS context that exists with this connection's peer.
         *
         * @return the session, or {@code null}, if no DTLS context has been established (yet)
         */
        public DTLSSession getEstablishedSession() {
            DTLSContext context = getEstablishedDtlsContext();
            return context == null ? null : context.getSession();
        }

        /**
         * Checks, whether a DTLS context has already been established with the peer.
         *
         * @return {@code true}, if a DTLS context has been established, {@code false}, otherwise.
         * @since 3.0 (replaces hasEstablishedSession)
         */
        public boolean hasEstablishedDtlsContext() {
            return establishedDtlsContext != null;
        }

        /**
         * Gets the already established DTLS context that exists with this connection's peer.
         *
         * @return the DTLS context, or {@code null}, if no DTLS context has been established (yet)
         */
        public DTLSContext getEstablishedDtlsContext() {
            return establishedDtlsContext;
        }

        /**
         * Gets the handshaker managing the currently ongoing handshake with the peer.
         *
         * @return the handshaker, or {@code null}, if no handshake is going on
         */
        public Handshaker getOngoingHandshake() {
            return ongoingHandshake.get();
        }

        /**
         * Checks whether there is a handshake going on with the peer.
         *
         * @return {@code true}, if a handshake is going on, {@code false}, otherwise.
         */
        public boolean hasOngoingHandshake() {
            return ongoingHandshake.get() != null;
        }

        /**
         * Get system nanos of starting client hello.
         *
         * @return system nanos, or {@code null}, if prevention is expired or not
         *         used.
         * @since 3.0
         */
        public Long getStartNanos() {
            Connection.ClientHelloIdentifier start = this.startingHelloClient;
            if (start != null) {
                return start.nanos;
            } else {
                return null;
            }
        }

        /**
         * Checks whether this connection is started for the provided CLIENT_HELLO.
         *
         * Use the random and message sequence number contained in the CLIENT_HELLO.
         *
         * Note: called outside of serial-execution and so requires external synchronization!
         *
         * @param clientHello the message to check.
         * @return {@code true} if the given client hello has initially started this
         *         connection.
         * @see #startByClientHello(ClientHello)
         * @throws NullPointerException if client hello is {@code null}.
         */
        public boolean isStartedByClientHello(ClientHello clientHello) {
            if (clientHello == null) {
                throw new NullPointerException("client hello must not be null!");
            }
            Connection.ClientHelloIdentifier start = this.startingHelloClient;
            if (start != null) {
                return start.isStartedByClientHello(clientHello);
            }
            return false;
        }

        /**
         * Set starting CLIENT_HELLO.
         *
         * Use the random and handshake message sequence number contained in the
         * CLIENT_HELLO. Removed, if when the handshake fails or with configurable
         * timeout after handshake completion.
         *
         * Note: called outside of serial-execution and so requires external synchronization!
         *
         * @param clientHello message which starts the connection.
         * @see #isStartedByClientHello(ClientHello)
         */
        public void startByClientHello(ClientHello clientHello) {
            if (clientHello == null) {
                startingHelloClient = null;
            } else {
                startingHelloClient = new Connection.ClientHelloIdentifier(clientHello);
            }
        }

        /**
         * Gets the DTLS context containing the connection's <em>current</em> state for
         * the provided epoch.
         *
         * This is the {@link #establishedDtlsContext}, if not {@code null} and the read
         * epoch is matching. Or the DTLS context negotiated in the
         * {@link #ongoingHandshake}, if not {@code null} and the read epoch is
         * matching. If both are {@code null}, or the read epoch doesn't match,
         * {@code null} is returned.
         *
         * @param readEpoch the read epoch to match.
         * @return the <em>current</em> DTLS context, or {@code null}, if neither an
         *         established DTLS context nor an ongoing handshake exists with an
         *         matching read epoch
         * @since 3.0 (replaces getSession(int))
         */
        public DTLSContext getDtlsContext(int readEpoch) {
            DTLSContext context = establishedDtlsContext;
            if (context != null && context.getReadEpoch() == readEpoch) {
                return context;
            }
            Handshaker handshaker = ongoingHandshake.get();
            if (handshaker != null) {
                context = handshaker.getDtlsContext();
                if (context != null && context.getReadEpoch() == readEpoch) {
                    return context;
                }
            }
            return null;
        }

        /**
         * Gets the DTLS context containing the connection's <em>current</em> state.
         *
         * This is the {@link #establishedDtlsContext}, if not {@code null}, or the
         * DTLS context negotiated in the {@link #ongoingHandshake}.
         *
         * @return the <em>current</em> DTLS context, or {@code null}, if neither an
         *         established DTLS context nor an ongoing handshake exists
         * @since 3.0 (replaces getSession())
         */
        public DTLSContext getDtlsContext() {
            DTLSContext context = establishedDtlsContext;
            if (context == null) {
                Handshaker handshaker = ongoingHandshake.get();
                if (handshaker != null) {
                    context = handshaker.getDtlsContext();
                }
            }
            return context;
        }

        /**
         * Reset DTLS context.
         *
         * Prepare connection for new handshake. Reset established DTLS context or
         * resume session and remove resumption mark.
         *
         * @throws IllegalStateException if neither a established DTLS context nor a
         *             resume session is available
         * @since 3.0 (replaces resetSession())
         */
        public void resetContext() {
            if (establishedDtlsContext == null) {
                throw new IllegalStateException("No established context to resume available!");
            }
            SecretUtil.destroy(establishedDtlsContext);
            establishedDtlsContext = null;
            resumptionRequired = false;
            updateConnectionState();
        }

        /**
         * Check, if connection was closed.
         *
         * @return {@code true}, if connection was closed, {@code false}, otherwise.
         * @since 2.3
         */
        public boolean isClosed() {
            DTLSContext context = establishedDtlsContext;
            return context != null && context.isMarkedAsClosed();
        }

        /**
         * Close connection with record.
         *
         * Mark session as closed. Received records with sequence numbers before
         * will still be processed, others are dropped. No message will be send
         * after this.
         *
         * @param record received close notify record.
         * @since 2.3
         */
        public void close(Record record) {
            DTLSContext context = establishedDtlsContext;
            if (context != null) {
                context.markCloseNotify(record.getEpoch(), record.getSequenceNumber());
            }
        }

        /**
         * Mark record as read in established DTLS context.
         *
         * @param record record to mark as read.
         * @return {@code true}, if the record is newer than the current newest.
         *         {@code false}, if not.
         * @since 3.0
         */
        public boolean markRecordAsRead(Record record) {
            boolean newest = false;
            DTLSContext context = establishedDtlsContext;
            if (context != null) {
                newest = context.markRecordAsRead(record.getEpoch(), record.getSequenceNumber());
            }
            return newest;
        }

        /**
         * Gets the root cause alert.
         *
         * For some case, the root cause may be hidden and replaced by a general
         * cause when sending an alert message. This keeps the root cause for
         * internal analysis.
         *
         * @return root cause alert.
         * @since 2.5
         */
        public AlertMessage getRootCauseAlert() {
            return rootCause;
        }

        /**
         * Sets root cause alert.
         *
         * For some case, the root cause may be hidden and replaced by a general
         * cause when sending an alert message. This keeps the root cause for
         * internal analysis.
         *
         * @param rootCause root cause alert
         * @return {@code true}, if the root cause is set, {@code false}, if the
         *         root cause is already set. (Return value added since 3.0)
         * @since 2.5
         */
        public boolean setRootCause(AlertMessage rootCause) {
            if (this.rootCause == null) {
                this.rootCause = rootCause;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Check, if resumption is required.
         *
         * @return {@code true}, if an abbreviated handshake should be done next time a data
         *         will be sent on this connection.
         */
        public boolean isResumptionRequired() {
            return resumptionRequired;
        }

        /**
         * Check, if the automatic session resumption should be triggered or is
         * already required.
         *
         * @param autoResumptionTimeoutMillis auto resumption timeout in
         *            milliseconds. {@code null}, if auto resumption is not used.
         * @return {@code true}, if the provided autoResumptionTimeoutMillis has
         *         expired without exchanging messages.
         */
        public boolean isAutoResumptionRequired(Long autoResumptionTimeoutMillis) {
            if (!resumptionRequired && autoResumptionTimeoutMillis != null && establishedDtlsContext != null) {
                long now = ClockUtil.nanoRealtime();
                long expires = lastMessageNanos + TimeUnit.MILLISECONDS.toNanos(autoResumptionTimeoutMillis);
                if ((now - expires) > 0) {
                    setResumptionRequired(true);
                }
            }
            return resumptionRequired;
        }

        /**
         * Refresh auto resumption timeout.
         *
         * Uses {@link ClockUtil#nanoRealtime()}.
         *
         * @see #lastMessageNanos
         */
        public void refreshAutoResumptionTime() {
            lastMessageNanos = ClockUtil.nanoRealtime();
        }

        /**
         * Get realtime nanoseconds of last message.
         *
         * @return realtime nanoseconds of last message
         * @since 3.0
         */
        public long getLastMessageNanos() {
            return lastMessageNanos;
        }

        /**
         * Use to force an abbreviated handshake next time a data will be sent on
         * this connection.
         *
         * @param resumptionRequired true to force abbreviated handshake.
         */
        public void setResumptionRequired(boolean resumptionRequired) {
            this.resumptionRequired = resumptionRequired;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((cid == null) ? 0 : cid.hashCode());
            result = prime * result + ((establishedDtlsContext == null) ? 0 : establishedDtlsContext.hashCode());
            result = prime * result + (int) (lastMessageNanos ^ (lastMessageNanos >>> 32));
            result = prime * result + ((peerAddress == null) ? 0 : peerAddress.hashCode());
            result = prime * result + (resumptionRequired ? 1231 : 1237);
            result = prime * result + ((router == null) ? 0 : router.hashCode());
            result = prime * result + ((rootCause == null) ? 0 : rootCause.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Connection other = (Connection) obj;
            if (!Bytes.equals(cid, other.cid)) {
                return false;
            }
            if (resumptionRequired != other.resumptionRequired) {
                return false;
            }
            if (lastMessageNanos != other.lastMessageNanos) {
                return false;
            }
            if (!Objects.equals(establishedDtlsContext, other.establishedDtlsContext)) {
                return false;
            }
            if (!Objects.equals(peerAddress, other.peerAddress)) {
                return false;
            }
            if (!Objects.equals(router, other.router)) {
                return false;
            }
            if (!Objects.equals(rootCause, other.rootCause)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("dtls-con: ");
            if (cid != null) {
                builder.append(cid);
            }
            if (peerAddress != null) {
                builder.append(", ").append(StringUtil.toDisplayString(peerAddress));
                Handshaker handshaker = getOngoingHandshake();
                if (handshaker != null) {
                    builder.append(", ongoing handshake ");
                    SessionId id = handshaker.getDtlsContext().getSession().getSessionIdentifier();
                    if (id != null && !id.isEmpty()) {
                        // during handshake this may by not already set
                        builder.append(StringUtil.byteArray2HexString(id.getBytes(), StringUtil.NO_SEPARATOR, 6));
                    }
                }
                if (isResumptionRequired()) {
                    builder.append(", resumption required");
                } else if (hasEstablishedDtlsContext()) {
                    builder.append(", session established ");
                    SessionId id = getEstablishedSession().getSessionIdentifier();
                    if (id != null && !id.isEmpty()) {
                        builder.append(StringUtil.byteArray2HexString(id.getBytes(), StringUtil.NO_SEPARATOR, 6));
                    }
                }
            }
            if (isExecuting()) {
                builder.append(", is alive");
            }
            return builder.toString();
        }

        /**
         * Identifier of starting client hello.
         *
         * Keeps random and handshake message sequence number to prevent from
         * accidentally starting a handshake again.
         *
         * @since 3.0
         */
        private static class ClientHelloIdentifier {

            private final Random clientHelloRandom;
            private final long nanos;
            private final int clientHelloMessageSeq;

            private ClientHelloIdentifier(ClientHello clientHello) {
                clientHelloMessageSeq = clientHello.getMessageSeq();
                clientHelloRandom = clientHello.getRandom();
                nanos = ClockUtil.nanoRealtime();
            }

            private ClientHelloIdentifier(DatagramReader reader, long nanoShift) {
                clientHelloMessageSeq = reader.read(Short.SIZE);
                byte[] data = reader.readVarBytes(Byte.SIZE);
                if (data != null) {
                    clientHelloRandom = new Random(data);
                } else {
                    clientHelloRandom = null;
                }
                nanos = reader.readLong(Long.SIZE) + nanoShift;
            }

            private boolean isStartedByClientHello(ClientHello clientHello) {
                if (clientHelloRandom.equals(clientHello.getRandom())) {
                    if (clientHelloMessageSeq >= clientHello.getMessageSeq()) {
                        return true;
                    }
                }
                return false;
            }

            private void write(DatagramWriter writer) {
                writer.write(clientHelloMessageSeq, Short.SIZE);
                writer.writeVarBytes(clientHelloRandom, Byte.SIZE);
                writer.writeLong(nanos, Long.SIZE);
            }
        }

        private class ConnectionSessionListener implements SessionListener, Serializable {

            @Override
            public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
                ongoingHandshake.set(handshaker);
                LOGGER.debug("Handshake with [{}] has been started", StringUtil.toLog(peerAddress));
            }

            @Override
            public void contextEstablished(Handshaker handshaker, DTLSContext context) throws HandshakeException {
                establishedDtlsContext = context;
                LOGGER.debug("Session context with [{}] has been established", StringUtil.toLog(peerAddress));
            }

            @Override
            public void handshakeCompleted(Handshaker handshaker) {
                SerialExecutor executor = serialExecutor;
                if (executor != null && !executor.isShutdown() && LOGGER_OWNER.isErrorEnabled()) {
                    try {
                        executor.assertOwner();
                    } catch (ConcurrentModificationException ex) {
                        LOGGER_OWNER.error("on handshake completed: connection {}", ex.getMessage(), ex);
                        if (LOGGER_OWNER.isDebugEnabled()) {
                            throw ex;
                        }
                    }
                }
                if (ongoingHandshake.compareAndSet(handshaker, null)) {
                    LOGGER.debug("Handshake with [{}] has been completed", StringUtil.toLog(peerAddress));
                }
            }

            @Override
            public void handshakeFailed(Handshaker handshaker, Throwable error) {
                SerialExecutor executor = serialExecutor;
                if (executor != null && !executor.isShutdown() && LOGGER_OWNER.isErrorEnabled()) {
                    try {
                        executor.assertOwner();
                    } catch (ConcurrentModificationException ex) {
                        LOGGER_OWNER.error("on handshake failed: connection {}", ex.getMessage(), ex);
                        if (LOGGER_OWNER.isDebugEnabled()) {
                            throw ex;
                        }
                    }
                }
                if (ongoingHandshake.compareAndSet(handshaker, null)) {
                    startingHelloClient = null;
                    LOGGER.debug("Handshake with [{}] has failed", StringUtil.toLog(peerAddress));
                }
            }

            @Override
            public void handshakeFlightRetransmitted(Handshaker handshaker, int flight) {
            }
        }

        /**
         * Version number for serialization.
         */
        private static final int VERSION = 1;

        /**
         * Write connection state.
         *
         * Note: the stream will contain not encrypted critical credentials. It is
         * required to protect this data before exporting it.
         *
         * @param writer writer for connection state
         * @return {@code true}, if connection is written, {@code false}, if not.
         * @since 3.0
         */
        public boolean writeTo(DatagramWriter writer) {
            if (establishedDtlsContext == null || establishedDtlsContext.isMarkedAsClosed() || rootCause != null) {
                return false;
            }
            int position = SerializationUtil.writeStartItem(writer, VERSION, Short.SIZE);
            writer.writeByte(resumptionRequired ? (byte) 1 : (byte) 0);
            writer.writeLong(lastMessageNanos, Long.SIZE);
            writer.writeVarBytes(cid, Byte.SIZE);
            SerializationUtil.write(writer, peerAddress);
            ClientHelloIdentifier start = startingHelloClient;
            if (start == null) {
                writer.writeByte((byte) 0);
            } else {
                writer.writeByte((byte) 1);
                start.write(writer);
            }
            establishedDtlsContext.writeTo(writer);
            writer.writeByte(cid != null && cid.equals(establishedDtlsContext.getReadConnectionId()) ? (byte) 1 : (byte) 0);
            SerializationUtil.writeFinishedItem(writer, position, Short.SIZE);
            return true;
        }

        /**
         * Read connection state.
         *
         * @param reader reader with connection state.
         * @param nanoShift adjusting shift for system time in nanoseconds.
         * @return read connection.
         * @throws IllegalArgumentException if version differs or data is erroneous.
         * @since 3.0
         */
        public static Connection fromReader(DataStreamReader reader, long nanoShift) {
            int length = SerializationUtil.readStartItem(reader, VERSION, Short.SIZE);
            if (0 < length) {
                DatagramReader rangeReader = reader.createRangeReader(length);
                return new Connection(rangeReader, nanoShift);
            } else {
                return null;
            }
        }

        /**
         * Create instance from reader.
         *
         * @param reader reader with connection state.
         * @param nanoShift adjusting shift for system time in nanoseconds.
         * @throws IllegalArgumentException if the data is erroneous
         * @since 3.0
         */
        private Connection(DatagramReader reader, long nanoShift) {
            resumptionRequired = reader.readNextByte() == 1;
            lastMessageNanos = reader.readLong(Long.SIZE) + nanoShift;
            byte[] data = reader.readVarBytes(Byte.SIZE);
            if (data == null) {
                throw new IllegalArgumentException("CID must not be null!");
            }
            cid = new ConnectionId(data);
            peerAddress = SerializationUtil.readAddress(reader);
            if (reader.readNextByte() == 1) {
                startingHelloClient = new ClientHelloIdentifier(reader, nanoShift);
            }
            establishedDtlsContext = DTLSContext.fromReader(reader);
            if (establishedDtlsContext == null) {
                throw new IllegalArgumentException("DTLS Context must not be null!");
            }
            if (reader.readNextByte() == 1) {
//                establishedDtlsContext.setReadConnectionId(cid);
            }
            reader.assertFinished("connection");
        }
    }


    public static class LeastRecentlyUsedCache<K, V> implements Serializable {

        /**
         * The cache's default initial capacity.
         */
        public static final int DEFAULT_INITIAL_CAPACITY = 16;
        /**
         * The default number of seconds after which an entry is considered
         * <em>stale</em> if it hasn't been accessed for that amount of time.
         */
        public static final long DEFAULT_THRESHOLD_SECS = 30 * 60; // 30 minutes
        /**
         * The cache's default maximum capacity.
         */
        public static final int DEFAULT_CAPACITY = 150000;

        private Collection<V> values;
        private final Map<K, LeastRecentlyUsedCache.CacheEntry<K, V>> cache;
        private volatile int capacity;
        private LeastRecentlyUsedCache.CacheEntry<K, V> header;
        /**
         * Threshold for expiration in nanoseconds.
         */
        private volatile long expirationThresholdNanos;
        /**
         * Enables eviction on read access ({@link #get(Object)} and
         * {@link #find(LeastRecentlyUsedCache.Predicate)}). Default is {@code true}.
         */
        private volatile boolean evictOnReadAccess = true;
        /**
         * Enables update last-access time on read access ({@link #get(Object)} and
         * {@link #find(LeastRecentlyUsedCache.Predicate)}). Default is {@code true}.
         */
        private volatile boolean updateOnReadAccess = true;

        private final List<LeastRecentlyUsedCache.EvictionListener<V>> evictionListeners = new LinkedList<>();

        /**
         * Creates a cache with an initial capacity of
         * {@link #DEFAULT_INITIAL_CAPACITY}, a maximum capacity of
         * {@link #DEFAULT_CAPACITY} entries and an expiration threshold of
         * {@link #DEFAULT_THRESHOLD_SECS} seconds.
         */
        public LeastRecentlyUsedCache() {
            this(DEFAULT_INITIAL_CAPACITY, DEFAULT_CAPACITY, DEFAULT_THRESHOLD_SECS, TimeUnit.SECONDS);
        }

        /**
         * Creates a cache based on given configuration parameters.
         * <p>
         * The cache's initial capacity is set to the lesser of
         * {@link #DEFAULT_INITIAL_CAPACITY} and <em>capacity</em>.
         *
         * @param capacity the maximum number of entries the cache can manage
         * @param threshold the period of time of inactivity (in seconds) after
         *            which an entry is considered stale and can be evicted from the
         *            cache if a new entry is to be added to the cache
         */
        public LeastRecentlyUsedCache(final int capacity, final long threshold) {
            this(Math.min(capacity, DEFAULT_INITIAL_CAPACITY), capacity, threshold, TimeUnit.SECONDS);
        }

        /**
         * Creates a cache based on given configuration parameters.
         *
         * @param initialCapacity The initial number of entries the cache will be
         *            initialized to support. The cache's capacity will be doubled
         *            dynamically every time 0.75 percent of its current capacity is
         *            used but it will never exceed <em>maxCapacity</em>.
         * @param maxCapacity The maximum number of entries the cache can manage
         * @param threshold The period of time of inactivity (in seconds) after
         *            which an entry is considered stale and can be evicted from the
         *            cache if a new entry is to be added to the cache
         */
        public LeastRecentlyUsedCache(final int initialCapacity, final int maxCapacity, final long threshold) {
            this(initialCapacity, maxCapacity, threshold, TimeUnit.SECONDS);
        }

        /**
         * Creates a cache based on given configuration parameters.
         *
         * @param initialCapacity The initial number of entries the cache will be
         *            initialized to support. The cache's capacity will be doubled
         *            dynamically every time 0.75 percent of its current capacity is
         *            used but it will never exceed <em>maxCapacity</em>.
         * @param maxCapacity The maximum number of entries the cache can manage
         * @param threshold The period of time of inactivity (in seconds) after
         *            which an entry is considered stale and can be evicted from the
         *            cache if a new entry is to be added to the cache
         * @param unit TimeUnit for threshold
         * @since 3.0
         */
        public LeastRecentlyUsedCache(int initialCapacity, int maxCapacity, long threshold, TimeUnit unit) {

            if (initialCapacity > maxCapacity) {
                throw new IllegalArgumentException("initial capacity must be <= max capacity");
            } else {
                this.capacity = maxCapacity;
                this.cache = new ConcurrentHashMap<>(initialCapacity);
                setExpirationThreshold(threshold, unit);
                initLinkedList();
            }
        }

        private void initLinkedList() {
            header = new LeastRecentlyUsedCache.CacheEntry<>();
            header.after = header.before = header;
        }

        /**
         * Registers a listener to be notified about (stale) entries being evicted
         * from the cache.
         *
         * @param listener the listener
         */
        public void addEvictionListener(
                LeastRecentlyUsedCache.EvictionListener<V> listener) {
            if (listener != null) {
                this.evictionListeners.add(listener);
            }
        }

        /**
         * Get evict mode on read access.
         *
         * Node: if {@code evicting on read access} and
         * {@code updating on read access} are both enabled, the eviction is
         * evaluated first!
         *
         * @return {@code true}, if entries are evicted on read access, when
         *         expired, {@code false}, if not.
         * @see #isUpdatingOnReadAccess()
         */
        public boolean isEvictingOnReadAccess() {
            return evictOnReadAccess;
        }

        /**
         * Set evict mode on read access.
         *
         * Node: if {@code evicting on read access} and
         * {@code updating on read access} are both enabled, the eviction is
         * evaluated first!
         *
         * @param evict {@code true}, if entries are evicted on read access, when
         *            expired, {@code false}, if not.
         * @see #setUpdatingOnReadAccess(boolean)
         */
        public void setEvictingOnReadAccess(boolean evict) {
            evictOnReadAccess = evict;
        }

        /**
         * Get update last-access time mode on read access.
         *
         * Node: if {@code evicting on read access} and
         * {@code updating on read access} are both enabled, the eviction is
         * evaluated first!
         *
         * @return {@code true}, if entries last-access time is updated on read
         *         access, {@code false}, if not.
         * @see #isEvictingOnReadAccess()
         */
        public boolean isUpdatingOnReadAccess() {
            return updateOnReadAccess;
        }

        /**
         * Set update last-access time mode on read access.
         *
         * Node: if {@code evicting on read access} and
         * {@code updating on read access} are both enabled, the eviction is
         * evaluated first!
         *
         * @param update {@code true},if entries last-access time is updated on read
         *            access, {@code false}, if not.
         * @see #setEvictingOnReadAccess(boolean)
         */
        public void setUpdatingOnReadAccess(boolean update) {
            updateOnReadAccess = update;
        }

        /**
         * Gets the period of time after which an entry is considered <em>stale</em>
         * if it hasn't be accessed.
         *
         * @return the threshold in seconds
         */
        public final long getExpirationThreshold() {
            return TimeUnit.NANOSECONDS.toSeconds(expirationThresholdNanos);
        }

        /**
         * Sets the period of time after which an entry is to be considered stale if
         * it hasn't be accessed.
         *
         * <em>NB</em>: invoking this method after creation of the cache does
         * <em>not</em> have an immediate effect, i.e. no (now stale) entries are
         * purged from the cache. This happens only when a new entry is put to the
         * cache or a stale entry is read from the cache.
         *
         * @param newThreshold the threshold in seconds
         * @see #put(Object, Object)
         * @see #get(Object)
         * @see #find(LeastRecentlyUsedCache.Predicate)
         */
        public final void setExpirationThreshold(long newThreshold) {
            setExpirationThreshold(newThreshold, TimeUnit.SECONDS);
        }

        /**
         * Sets the period of time after which an entry is to be considered stale if
         * it hasn't be accessed.
         *
         * <em>NB</em>: invoking this method after creation of the cache does
         * <em>not</em> have an immediate effect, i.e. no (now stale) entries are
         * purged from the cache. This happens only when a new entry is put to the
         * cache or a stale entry is read from the cache.
         *
         * @param newThreshold the threshold
         * @param unit TimeUnit for threshold
         * @see #put(Object, Object)
         * @see #get(Object)
         * @see #find(LeastRecentlyUsedCache.Predicate)
         */
        public final void setExpirationThreshold(long newThreshold, TimeUnit unit) {
            this.expirationThresholdNanos = unit.toNanos(newThreshold);
        }

        /**
         * Gets the maximum number of entries this cache can manage.
         *
         * @return the number of entries
         */
        public final int getCapacity() {
            return capacity;
        }

        /**
         * Sets the maximum number of entries this cache can manage.
         *
         * <em>NB</em>: invoking this method after creation of the cache does
         * <em>not</em> have an immediate effect, i.e. no entries are purged from
         * the cache. This happens only when a new entry is put to the cache or a
         * stale entry is read from the cache.
         *
         * @param capacity the maximum number of entries the cache can manage
         * @see #put(Object, Object)
         * @see #get(Object)
         */
        public final void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        /**
         * Gets the cache's current number of entries.
         *
         * @return the size
         */
        public final int size() {
            return cache.size();
        }

        /**
         * Gets the number of entries that can be added to this cache without the
         * need for removing stale entries.
         *
         * @return The number of entries.
         */
        public final int remainingCapacity() {
            return Math.max(0, capacity - cache.size());
        }

        /**
         * Removes all entries from the cache.
         */
        public final void clear() {
            cache.clear();
            initLinkedList();
        }

        /**
         * Puts an entry to the cache.
         *
         * An entry can be successfully added to the cache if any of the following
         * conditions are met:
         * <ul>
         * <li>The cache's remaining capacity is greater than zero.</li>
         * <li>The cache contains at least one <em>stale</em> entry, i.e. an entry
         * that has not been accessed for at least the cache's <em> expiration
         * threshold</em> period. In such a case the least- recently accessed stale
         * entry gets evicted from the cache to make place for the new entry to be
         * added.</li>
         * </ul>
         *
         * If an entry is evicted this method notifies all registered
         * {@code EvictionListeners}.
         *
         * @param key the key to store the value under
         * @param value the value to store
         * @return {@code true}, if the entry could be added to the cache,
         *         {@code false}, otherwise, e.g. because the cache's remaining
         *         capacity is zero and no stale entries can be evicted
         * @see #addEvictionListener(LeastRecentlyUsedCache.EvictionListener)
         */
        public final boolean put(K key, V value) {

            if (value != null) {
                LeastRecentlyUsedCache.CacheEntry<K, V> existingEntry = cache.get(key);
                if (existingEntry != null) {
                    existingEntry.remove();
                    add(key, value);
                    return true;
                } else if (cache.size() < capacity) {
                    add(key, value);
                    return true;
                } else {
                    LeastRecentlyUsedCache.CacheEntry<K, V> eldest = header.after;
                    if (eldest.isStale(expirationThresholdNanos)) {
                        eldest.remove();
                        cache.remove(eldest.getKey());
                        add(key, value);
                        notifyEvictionListeners(eldest.getValue());
                        return true;
                    }
                }
            }
            return false;
        }

        private void notifyEvictionListeners(V session) {
            for (LeastRecentlyUsedCache.EvictionListener<V> listener : evictionListeners) {
                listener.onEviction(session);
            }
        }

        /**
         * Gets the <em>eldest</em> value in the store.
         *
         * The eldest value is the one that has been used least recently.
         *
         * @return the value
         */
        final V getEldest() {
            LeastRecentlyUsedCache.CacheEntry<K, V> eldest = header.after;
            return eldest.getValue();
        }

        private void add(K key, V value) {
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = new LeastRecentlyUsedCache.CacheEntry<>(key, value);
            cache.put(key, entry);
            entry.addBefore(header);
        }

        /**
         * Puts an entry with last-update-timestamp to the cache.
         *
         * An entry can be successfully added to the cache if any of the following
         * conditions are met:
         * <ul>
         * <li>The cache's remaining capacity is greater than zero.</li>
         * <li>The cache contains at least one <em>stale</em> entry, i.e. an entry
         * that has not been accessed for at least the cache's <em> expiration
         * threshold</em> period. That entry must be before the provided
         * last-update-timestamp. In such a case the least-recently accessed stale
         * entry gets evicted from the cache to make place for the new entry to be
         * added.</li>
         * </ul>
         *
         * Add the entries in ascending last-update-timestamp order for best
         * performance.
         *
         * If an entry is evicted this method notifies all registered
         * {@code EvictionListeners}.
         *
         * @param key the key to store the value under
         * @param value the value to store
         * @param lastUpdate the last-update timestamp to store
         * @return {@code true}, if the entry could be added to the cache,
         *         {@code false}, otherwise.
         * @see #addEvictionListener(LeastRecentlyUsedCache.EvictionListener)
         * @since 3.0
         */
        public final boolean put(K key, V value, long lastUpdate) {
            if (value != null) {
                LeastRecentlyUsedCache.CacheEntry<K, V> existingEntry = cache.get(key);
                if (existingEntry != null) {
                    existingEntry.remove();
                    add(key, value, lastUpdate);
                    return true;
                } else if (cache.size() < capacity) {
                    add(key, value, lastUpdate);
                    return true;
                } else {
                    LeastRecentlyUsedCache.CacheEntry<K, V> eldest = header.after;
                    if (eldest.isStale(expirationThresholdNanos) && (lastUpdate - eldest.lastUpdate) >= 0) {
                        eldest.remove();
                        cache.remove(eldest.getKey());
                        add(key, value, lastUpdate);
                        notifyEvictionListeners(eldest.getValue());
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Add entry with last-update timestamp.
         *
         * Add the entries in ascending last-update timestamp order for best
         * performance.
         *
         * @param key the key to store the value under
         * @param value the value to store
         * @param lastUpdate the last-update timestamp to store
         * @since 3.0
         */
        private void add(K key, V value, long lastUpdate) {
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = new LeastRecentlyUsedCache.CacheEntry<>(key, value, lastUpdate);
            cache.put(key, entry);
            if (header.before == header) {
                // first
                entry.addBefore(header);
            } else {
                LeastRecentlyUsedCache.CacheEntry<K, V> position = header;
                while ((lastUpdate - position.before.lastUpdate) < 0) {
                    position = position.before;
                    if (position == header) {
                        break;
                    }
                }
                entry.addBefore(position);
            }
        }

        /**
         * Gets a value from the cache.
         *
         * @param key the key to look up in the cache
         * @return the value, if the key has been found in the cache and the value
         *         is not stale, {@code null}, otherwise
         */
        public final V get(K key) {
            if (key == null) {
                return null;
            }
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            return access(entry, null);
        }

        /**
         * Gets a timestamped value from the cache.
         *
         * For {@link #updateOnReadAccess}, the timestamp of the entry is updated
         * after access. The returned timestamp is the value before that update.
         *
         * @param key the key to look up in the cache
         * @return the timestamped value, if the key has been found in the cache and
         *         the value is not stale, {@code null}, otherwise
         * @since 3.0
         */
        public final LeastRecentlyUsedCache.Timestamped<V> getTimestamped(K key) {
            if (key == null) {
                return null;
            }
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            LeastRecentlyUsedCache.Timestamped<V> timestamped = entry.getEntry();
            if (access(entry, null) == null) {
                return null;
            }
            return timestamped;
        }

        private final V access(
                LeastRecentlyUsedCache.CacheEntry<K, V> entry, Iterator<LeastRecentlyUsedCache.CacheEntry<K, V>> iterator) {
            if (evictOnReadAccess && expirationThresholdNanos > 0 && entry.isStale(expirationThresholdNanos)) {
                if (iterator != null) {
                    // remove via iterator to prevent
                    // ConcurrentModificationException
                    iterator.remove();
                } else {
                    cache.remove(entry.getKey());
                }
                entry.remove();
                notifyEvictionListeners(entry.getValue());
                return null;
            } else {
                if (updateOnReadAccess) {
                    entry.recordAccess(header);
                }
                return entry.getValue();
            }
        }

        /**
         * Update the last-access time.
         *
         * Intended to be used, if automatic updating the last-access time on
         * read-access is suppressed by {@link #updateOnReadAccess}.
         *
         * @param key the key to update the last-access time.
         * @return {@code true}, if updated, {@code false}, otherwise.
         */
        public final boolean update(K key) {
            if (key == null) {
                return false;
            }
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            entry.recordAccess(header);
            return true;
        }

        /**
         * Removes an entry from the cache.
         *
         * Doesn't call {@code EvictionListeners}.
         *
         * @param key the key of the entry to remove
         * @return the removed value or {@code null}, if the cache does not contain
         *         the key
         */
        public final V remove(K key) {
            if (key == null) {
                return null;
            }
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = cache.remove(key);
            if (entry != null) {
                entry.remove();
                return entry.getValue();
            } else {
                return null;
            }
        }

        /**
         * Removes provided entry from the cache.
         *
         * Doesn't call {@code EvictionListeners}.
         *
         * @param key the key of the entry to remove
         * @param value value of the entry to remove
         * @return the removed value or {@code null}, if the cache does not contain
         *         the key or entry
         */
        public final V remove(K key, V value) {
            if (key == null) {
                return null;
            }
            LeastRecentlyUsedCache.CacheEntry<K, V> entry = cache.get(key);
            if (entry != null) {
                if (entry.getValue() == value) {
                    cache.remove(key);
                    entry.remove();
                    return value;
                }
            }
            return null;
        }

        /**
         * Remove expired entries.
         *
         * @param maxEntries maximum expired entries to remove
         * @return number of removed expired entries.
         * @since 3.0
         */
        public final int removeExpiredEntries(int maxEntries) {
            int counter = 0;
            while (maxEntries == 0 || counter < maxEntries) {
                LeastRecentlyUsedCache.CacheEntry<K, V> eldest = header.after;
                if (header == eldest) {
                    break;
                }
                if (eldest.isStale(expirationThresholdNanos)) {
                    eldest.remove();
                    cache.remove(eldest.getKey());
                    notifyEvictionListeners(eldest.getValue());
                    ++counter;
                } else {
                    break;
                }
            }
            return counter;
        }

        /**
         * Finds a value based on a predicate.
         *
         * Returns the first matching value applying the {@link #evictOnReadAccess}
         * setting. Access the values to provide them as arguments for the predicate
         * only applies the {@link #updateOnReadAccess} for matching values.
         *
         * @param predicate the condition to match. Assumed to match entries in a
         *            unique manner. Therefore stops on first match, even if that
         *            gets evicted on the read access.
         * @return the first value from the cache that matches according to the
         *         given predicate, or {@code null}, if no value matches
         */
        public final V find(LeastRecentlyUsedCache.Predicate<V> predicate) {
            return find(predicate, true);
        }

        /**
         * Finds a value based on a predicate.
         *
         * Returns the first matching value applying the {@link #evictOnReadAccess}
         * setting. Access the values to provide them as arguments for the predicate
         * only applies the {@link #updateOnReadAccess} for matching values.
         *
         * @param predicate the condition to match
         * @param unique {@code true}, if the predicate matches entries in a unique
         *            manner and stops, even if that entry gets evicted on the read
         *            access. {@code false}, if more entries may be matched and so
         *            continue to search, if a matching entry gets evicted on the
         *            read access.
         * @return the first value from the cache that matches according to the
         *         given predicate, or {@code null}, if no value matches
         */
        public final V find(LeastRecentlyUsedCache.Predicate<V> predicate, boolean unique) {
            if (predicate != null) {
                final Iterator<LeastRecentlyUsedCache.CacheEntry<K, V>> iterator = cache.values().iterator();
                while (iterator.hasNext()) {
                    LeastRecentlyUsedCache.CacheEntry<K, V> entry = iterator.next();
                    if (predicate.accept(entry.getValue())) {
                        V value = access(entry, iterator);
                        if (unique || value != null) {
                            return value;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * A predicate to be applied to cache entries to determine the result set
         * when searching for particular values.
         *
         * @param <V> The type of value the predicate can be evaluated on.
         */
        public static interface Predicate<V> {

            /**
             * Applies the predicate to a cache value.
             *
             * @param value The value to evaluate the predicate for.
             * @return {@code true} if the cache entry containing the value is part
             *         of the result set.
             */
            boolean accept(V value);
        }

        /**
         * A callback for getting notified about entries being evicted from the
         * cache.
         *
         * @param <V> The type of entry being evicted.
         */
        public static interface EvictionListener<V> {

            /**
             * Indicates that an entry has been evicted from the cache.
             *
             * @param evictedValue The evicted entry.
             */
            void onEviction(V evictedValue);
        }

        /**
         * Gets iterator over all values contained in this cache.
         * <p>
         * The iterator returned is backed by this cache's underlying
         * {@link ConcurrentHashMap#values()}. The iterator is a "weakly consistent"
         * iterator that will never throw {@link java.util.ConcurrentModificationException},
         * and guarantees to traverse elements as they existed upon construction of
         * the iterator, and may (but is not guaranteed to) reflect any
         * modifications subsequent to construction.
         * </p>
         * <p>
         * The {@link #evictOnReadAccess} and {@link #updateOnReadAccess} are
         * applied on {@link Iterator#hasNext()}.
         * </p>
         * <p>
         * Removal of values from the iterator is unsupported.
         * </p>
         *
         * @return an iterator over all values backed by the underlying map.
         */
        public final Iterator<V> valuesIterator() {
            return valuesIterator(true);
        }

        /**
         * Gets iterator over all values contained in this cache.
         * <p>
         * The iterator returned is backed by this cache's underlying
         * {@link ConcurrentHashMap#values()}. The iterator is a "weakly consistent"
         * iterator that will never throw
         * {@link java.util.ConcurrentModificationException}, and guarantees to
         * traverse elements as they existed upon construction of the iterator, and
         * may (but is not guaranteed to) reflect any modifications subsequent to
         * construction.
         * </p>
         * <p>
         * Removal of values from the iterator is unsupported.
         * </p>
         *
         * @param readAccess {@code true} to enable read access while iterating. The
         *            {@link #evictOnReadAccess} and {@link #updateOnReadAccess} are
         *            applied on {@link Iterator#hasNext()}, if enabled.
         * @return an iterator over all values backed by the underlying map.
         * @since 3.0
         */
        public final Iterator<V> valuesIterator(final boolean readAccess) {
            final Iterator<LeastRecentlyUsedCache.CacheEntry<K, V>> iterator = cache.values().iterator();

            return new Iterator<V>() {

                private boolean hasNextCalled;
                private LeastRecentlyUsedCache.CacheEntry<K, V> nextEntry;

                @Override
                public boolean hasNext() {
                    if (!hasNextCalled) {
                        nextEntry = null;
                        while (iterator.hasNext()) {
                            LeastRecentlyUsedCache.CacheEntry<K, V> entry = iterator.next();
                            if (readAccess) {
                                synchronized (LeastRecentlyUsedCache.this) {
                                    if (access(entry, iterator) != null) {
                                        nextEntry = entry;
                                        break;
                                    }
                                }
                            } else {
                                nextEntry = entry;
                                break;
                            }
                        }
                        hasNextCalled = true;
                    }
                    return nextEntry != null;
                }

                @Override
                public V next() {
                    hasNext();
                    hasNextCalled = false;
                    if (nextEntry == null) {
                        throw new NoSuchElementException();
                    }
                    return nextEntry.value;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Gets all connections contained in this cache.
         *
         * The returned collection is intended to be used as read access, therefore
         * the modifying methods will throw a {@link UnsupportedOperationException}.
         *
         * Note: the {@link #evictOnReadAccess} feature may alter the underlying map
         * even for read access. Therefore the clients should explicitly serialize
         * access to the returned collection from multiple threads. The returned
         * size doesn't reflect potential eviction on read-access.
         *
         * @return an collection of all connections backed by the underlying map.
         */
        public final Collection<V> values() {
            Collection<V> vs = values;
            if (vs == null) {
                vs = new AbstractCollection<V>() {

                    @Override
                    public final int size() {
                        return cache.size();
                    }

                    @Override
                    public final boolean contains(final Object o) {
                        return null != find(new LeastRecentlyUsedCache.Predicate<V>() {

                            @Override
                            public boolean accept(final V value) {
                                return value.equals(o);
                            }
                        }, false);
                    }

                    @Override
                    public final Iterator<V> iterator() {
                        return valuesIterator();
                    }

                    @Override
                    public final boolean add(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public final boolean remove(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public final void clear() {
                        throw new UnsupportedOperationException();
                    }
                };
                values = vs;
            }
            return vs;
        }

        /**
         * Gets iterator over all values with timestamp contained in this cache.
         * <p>
         * The iterator returned is backed by this cache's underlying doubly-linked
         * list. The entries a order according their last update. It's not supported
         * to modify the cache when using the iterator.
         * </p>
         * <p>
         * Removal of values from the iterator is unsupported.
         * </p>
         *
         * @return an iterator over all values backed by the underlying
         *         doubly-linked list.
         * @since 3.0
         */
        public final Iterator<LeastRecentlyUsedCache.Timestamped<V>> timestampedIterator() {
            return new Iterator<LeastRecentlyUsedCache.Timestamped<V>>() {
                final int max = cache.size();
                int counter;
                LeastRecentlyUsedCache.CacheEntry<K, V> current = header;

                @Override
                public boolean hasNext() {
                    return current.after != header && counter < max;
                }

                @Override
                public LeastRecentlyUsedCache.Timestamped<V> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    ++counter;
                    current = current.after;
                    return current.getEntry();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private static class CacheEntry<K, V> implements Serializable {

            private final K key;
            private final V value;
            private long lastUpdate;
            private LeastRecentlyUsedCache.CacheEntry<K, V> after;
            private LeastRecentlyUsedCache.CacheEntry<K, V> before;

            private CacheEntry() {
                this.key = null;
                this.value = null;
                this.lastUpdate = -1;
            }

            private CacheEntry(K key, V value) {
                this(key, value, ClockUtil.nanoRealtime());
            }

            private CacheEntry(K key, V value, long lasUpdate) {
                this.key = key;
                this.value = value;
                this.lastUpdate = lasUpdate;
            }

            private LeastRecentlyUsedCache.Timestamped<V> getEntry() {
                return new LeastRecentlyUsedCache.Timestamped<V>(value, lastUpdate);
            }

            private K getKey() {
                return key;
            }

            private V getValue() {
                return value;
            }

            private boolean isStale(long thresholdNanos) {
                return (ClockUtil.nanoRealtime() - lastUpdate) >= thresholdNanos;
            }

            private void recordAccess(LeastRecentlyUsedCache.CacheEntry<K, V> header) {
                remove();
                lastUpdate = ClockUtil.nanoRealtime();
                addBefore(header);
            }

            private void addBefore(
                    LeastRecentlyUsedCache.CacheEntry<K, V> existingEntry) {
                after = existingEntry;
                before = existingEntry.before;
                before.after = this;
                after.before = this;
            }

            private void remove() {
                before.after = after;
                after.before = before;
            }

            @Override
            public String toString() {
                return new StringBuilder("CacheEntry [key: ").append(key).append(", last access: ").append(lastUpdate)
                        .append("]").toString();
            }
        }

        public static final class Timestamped<V> {
            private final V value;
            private final long lastUpdate;

            public Timestamped(V value, long lastUpdate) {
                this.value = value;
                this.lastUpdate = lastUpdate;
            }

            public V getValue() {
                return value;
            }

            public long getLastUpdate() {
                return lastUpdate;
            }

            @Override
            public int hashCode() {
                int hash = (int) (lastUpdate ^ (lastUpdate >>> 32));
                if (value != null) {
                    return hash + value.hashCode();
                }
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                } else if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                LeastRecentlyUsedCache.Timestamped<?> other = (LeastRecentlyUsedCache.Timestamped<?>) obj;
                if (lastUpdate != other.lastUpdate) {
                    return false;
                }
                if (value == null) {
                    return other.value == null;
                } else {
                    return value.equals(other.value);
                }
            }

            public String toString() {
                return lastUpdate + ": " + value;
            }
        }
    }



}
