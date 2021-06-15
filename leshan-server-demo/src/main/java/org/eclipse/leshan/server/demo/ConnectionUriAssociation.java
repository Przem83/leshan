package org.eclipse.leshan.server.demo;

import org.eclipse.leshan.core.request.Identity;

import java.net.URI;

public interface ConnectionUriAssociation {
    URI getConnectionUri(Identity identity);
}
