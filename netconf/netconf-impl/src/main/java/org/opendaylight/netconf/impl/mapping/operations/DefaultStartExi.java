/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.impl.mapping.operations;

import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.impl.NetconfServerSession;
import org.opendaylight.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultStartExi extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {
    public static final String START_EXI = "start-exi";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultStartExi.class);
    private NetconfServerSession netconfSession;

    public DefaultStartExi(final String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    public Document handle(final Document message,
                           final NetconfOperationChainedExecution subsequentOperation) throws DocumentedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received start-exi message {} ", XmlUtil.toString(message));
        }

        try {
            netconfSession.startExiCommunication(new NetconfMessage(message));
        } catch (final IllegalArgumentException e) {
            throw new DocumentedException("Failed to parse EXI parameters", e, ErrorType.PROTOCOL,
                    ErrorTag.OPERATION_FAILED, ErrorSeverity.ERROR);
        }

        return super.handle(message, subsequentOperation);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document,
                                                       final XmlElement operationElement) {
        final Element getSchemaResult = document.createElementNS(
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, XmlNetconfConstants.OK);
        LOG.trace("{} operation successful", START_EXI);
        return getSchemaResult;
    }

    @Override
    protected String getOperationName() {
        return START_EXI;
    }

    @Override
    protected String getOperationNamespace() {
        return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0;
    }

    @Override
    public void setNetconfSession(final NetconfServerSession netconfServerSession) {
        netconfSession = netconfServerSession;
    }
}
