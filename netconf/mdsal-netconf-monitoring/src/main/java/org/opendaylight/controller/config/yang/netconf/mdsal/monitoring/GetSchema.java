/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netconf.mdsal.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class GetSchema extends AbstractSingletonNetconfOperation {
    private static final String GET_SCHEMA = "get-schema";
    private static final String IDENTIFIER = "identifier";
    private static final String VERSION = "version";

    private static final Logger LOG = LoggerFactory.getLogger(GetSchema.class);
    private final NetconfMonitoringService cap;

    public GetSchema(final String netconfSessionIdForReporting, final NetconfMonitoringService cap) {
        super(netconfSessionIdForReporting);
        this.cap = cap;
    }

    @Override
    protected String getOperationName() {
        return GET_SCHEMA;
    }

    @Override
    protected String getOperationNamespace() {
        return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement xml)
            throws DocumentedException {
        final GetSchemaEntry entry;

        entry = new GetSchemaEntry(xml);

        final String schema;
        try {
            schema = cap.getSchemaForCapability(entry.identifier, entry.version);
        } catch (final IllegalStateException e) {
            final Map<String, String> errorInfo = new HashMap<>();
            // FIXME: so we have an <operation-failed>e.getMessage()</operation-failed> ??? In which namespace? Why?
            errorInfo.put(ErrorTag.OPERATION_FAILED.elementBody(), e.getMessage());
            LOG.warn("Rpc error: {}", ErrorTag.OPERATION_FAILED, e);
            throw new DocumentedException(e.getMessage(), e, ErrorType.APPLICATION,
                    ErrorTag.OPERATION_FAILED, ErrorSeverity.ERROR, errorInfo);
        }

        final Element getSchemaResult;
        getSchemaResult = XmlUtil.createTextElement(document, XmlNetconfConstants.DATA_KEY, schema,
                Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING));
        LOG.trace("{} operation successful", GET_SCHEMA);

        return getSchemaResult;
    }

    private static final class GetSchemaEntry {
        private final String identifier;
        private final Optional<String> version;

        GetSchemaEntry(final XmlElement getSchemaElement) throws DocumentedException {
            getSchemaElement.checkName(GET_SCHEMA);
            getSchemaElement.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING);

            final XmlElement identifierElement;
            try {
                identifierElement = getSchemaElement.getOnlyChildElementWithSameNamespace(IDENTIFIER);
            } catch (final DocumentedException e) {
                LOG.trace("Can't get identifier element as only child element with same namespace due to ", e);
                throw DocumentedException.wrap(e);
            }
            identifier = identifierElement.getTextContent();
            final Optional<XmlElement> versionElement = getSchemaElement
                    .getOnlyChildElementWithSameNamespaceOptionally(VERSION);
            if (versionElement.isPresent()) {
                version = Optional.of(versionElement.get().getTextContent());
            } else {
                version = Optional.empty();
            }
        }
    }
}
