/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.impl;

import java.io.IOException;
import org.opendaylight.netconf.sal.rest.api.RestconfNormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * This class just delegates all of the functionality to Yangtools normalized node writer.
 */
public final class RestconfDelegatingNormalizedNodeWriter implements RestconfNormalizedNodeWriter {
    private final NormalizedNodeWriter delegNNWriter;

    private RestconfDelegatingNormalizedNodeWriter(final NormalizedNodeStreamWriter streamWriter, final boolean
            orderKeyLeaves) {
        this.delegNNWriter = NormalizedNodeWriter.forStreamWriter(streamWriter, orderKeyLeaves);
    }

    public static RestconfDelegatingNormalizedNodeWriter forStreamWriter(final NormalizedNodeStreamWriter writer) {
        return forStreamWriter(writer, true);
    }

    public static RestconfDelegatingNormalizedNodeWriter forStreamWriter(final NormalizedNodeStreamWriter writer,
                                                                         final boolean orderKeyLeaves) {
        return new RestconfDelegatingNormalizedNodeWriter(writer, orderKeyLeaves);
    }

    @Override
    public RestconfDelegatingNormalizedNodeWriter write(final NormalizedNode node) throws IOException {
        delegNNWriter.write(node);
        return this;
    }

    @Override
    public void flush() throws IOException {
        delegNNWriter.flush();
    }

    @Override
    public void close() throws IOException {
        delegNNWriter.close();
    }
}
