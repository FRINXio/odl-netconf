/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.impl.mapping.operations;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.impl.NetconfServerSession;
import org.opendaylight.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCloseSession extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCloseSession.class);

    public static final String CLOSE_SESSION = "close-session";

    private final AutoCloseable sessionResources;
    private NetconfServerSession session;

    public DefaultCloseSession(final String netconfSessionIdForReporting, final AutoCloseable sessionResources) {
        super(netconfSessionIdForReporting);
        this.sessionResources = sessionResources;
    }

    @Override
    protected String getOperationName() {
        return CLOSE_SESSION;
    }

    /**
     * Close netconf operation router associated to this session, which in turn
     * closes NetconfOperationServiceSnapshot with all NetconfOperationService
     * instances.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement)
            throws DocumentedException {
        try {
            sessionResources.close();
            requireNonNull(session, "Session was not set").delayedClose();
            LOG.info("Session {} closing", session.getSessionId());
        } catch (final Exception e) {
            throw new DocumentedException("Unable to properly close session " + getNetconfSessionIdForReporting(), e,
                    ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, ErrorSeverity.ERROR,
                    // FIXME: i.e. <error>exception.toString()</error>? That looks wrong on a few levels.
                    Map.of(ErrorSeverity.ERROR.elementBody(), e.getMessage()));
        }
        return document.createElement(XmlNetconfConstants.OK);
    }

    @Override
    public void setNetconfSession(final NetconfServerSession netconfServerSession) {
        this.session = netconfServerSession;
    }
}
