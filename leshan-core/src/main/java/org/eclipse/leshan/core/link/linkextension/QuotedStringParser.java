/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link.linkextension;

import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.Parser;

/**
 * A CoRE Link quoted-string parser interface defined in RFC2616
 * (https://datatracker.ietf.org/doc/html/rfc2616#section-2.2).
 */
public interface QuotedStringParser extends Parser<LinkParamValue> {
}
