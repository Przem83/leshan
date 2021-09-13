package org.eclipse.leshan;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Map;

import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.Bytes;

class FileContext implements EndpointContext {

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public Number getNumber(String key) {
        return null;
    }

    @Override
    public Bytes getBytes(String key) {
        return null;
    }

    @Override
    public Map<String, Object> entries() {
        return null;
    }

    @Override
    public boolean hasCriticalEntries() {
        return false;
    }

    @Override
    public Principal getPeerIdentity() {
        return null;
    }

    @Override
    public InetSocketAddress getPeerAddress() {
        return new InetSocketAddress(0xffff);
    }

    @Override
    public String getVirtualHost() {
        return null;
    }
}
