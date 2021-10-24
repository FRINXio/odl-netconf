/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.rests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFluentFuture;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.netconf.dom.api.NetconfDataTreeService;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.common.errors.RestconfError;
import org.opendaylight.restconf.nb.rfc8040.ContentParameter;
import org.opendaylight.restconf.nb.rfc8040.DepthParameter;
import org.opendaylight.restconf.nb.rfc8040.WithDefaultsParameter;
import org.opendaylight.restconf.nb.rfc8040.legacy.QueryParameters;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.MdsalRestconfStrategy;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.NetconfRestconfStrategy;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.RestconfStrategy;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ReadDataTransactionUtilTest {

    private static final TestData DATA = new TestData();
    private static final NodeIdentifier NODE_IDENTIFIER =
        new NodeIdentifier(QName.create("ns", "2016-02-28", "container"));

    private RestconfStrategy mdsalStrategy;
    private RestconfStrategy netconfStrategy;
    @Mock
    private NetconfDataTreeService netconfService;
    @Mock
    private InstanceIdentifierContext<ContainerSchemaNode> context;
    @Mock
    private DOMDataTreeReadTransaction read;
    @Mock
    private EffectiveModelContext schemaContext;
    @Mock
    private ContainerSchemaNode containerSchemaNode;
    @Mock
    private LeafSchemaNode containerChildNode;
    private QName containerChildQName;

    @Before
    public void setUp() {
        containerChildQName = QName.create("ns", "2016-02-28", "container-child");

        when(context.getSchemaContext()).thenReturn(schemaContext);
        when(context.getSchemaNode()).thenReturn(containerSchemaNode);
        when(containerSchemaNode.getQName()).thenReturn(NODE_IDENTIFIER.getNodeType());
        when(containerChildNode.getQName()).thenReturn(containerChildQName);
        when(containerSchemaNode.dataChildByName(containerChildQName)).thenReturn(containerChildNode);

        DOMDataBroker mockDataBroker = mock(DOMDataBroker.class);
        doReturn(read).when(mockDataBroker).newReadOnlyTransaction();
        mdsalStrategy = new MdsalRestconfStrategy(mockDataBroker);
        netconfStrategy = new NetconfRestconfStrategy(netconfService);
    }

    @Test
    public void readDataConfigTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(netconfService).getConfig(DATA.path);
        NormalizedNode normalizedNode = readData(ContentParameter.CONFIG, DATA.path, mdsalStrategy);
        assertEquals(DATA.data3, normalizedNode);

        normalizedNode = readData(ContentParameter.CONFIG, DATA.path, netconfStrategy);
        assertEquals(DATA.data3, normalizedNode);
    }

    @Test
    public void readAllHavingOnlyConfigTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path);
        doReturn(immediateFluentFuture(Optional.empty())).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(netconfService).getConfig(DATA.path);
        doReturn(immediateFluentFuture(Optional.empty())).when(netconfService).get(DATA.path);
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path, mdsalStrategy);
        assertEquals(DATA.data3, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path, netconfStrategy);
        assertEquals(DATA.data3, normalizedNode);
    }

    @Test
    public void readAllHavingOnlyNonConfigTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data2))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path2);
        doReturn(immediateFluentFuture(Optional.empty())).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path2);
        doReturn(immediateFluentFuture(Optional.of(DATA.data2))).when(netconfService).get(DATA.path2);
        doReturn(immediateFluentFuture(Optional.empty())).when(netconfService).getConfig(DATA.path2);
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path2, mdsalStrategy);
        assertEquals(DATA.data2, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path2, netconfStrategy);
        assertEquals(DATA.data2, normalizedNode);
    }

    @Test
    public void readDataNonConfigTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data2))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path2);
        doReturn(immediateFluentFuture(Optional.of(DATA.data2))).when(netconfService).get(DATA.path2);
        NormalizedNode normalizedNode = readData(ContentParameter.NONCONFIG, DATA.path2, mdsalStrategy);
        assertEquals(DATA.data2, normalizedNode);

        normalizedNode = readData(ContentParameter.NONCONFIG, DATA.path2, netconfStrategy);
        assertEquals(DATA.data2, normalizedNode);
    }

    @Test
    public void readContainerDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data4))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(netconfService).getConfig(DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data4))).when(netconfService).get(DATA.path);
        final ContainerNode checkingData = Builders
                .containerBuilder()
                .withNodeIdentifier(NODE_IDENTIFIER)
                .withChild(DATA.contentLeaf)
                .withChild(DATA.contentLeaf2)
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path, mdsalStrategy);
        assertEquals(checkingData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path, netconfStrategy);
        assertEquals(checkingData, normalizedNode);
    }

    @Test
    public void readContainerDataConfigNoValueOfContentTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data4))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data3))).when(netconfService).getConfig(DATA.path);
        doReturn(immediateFluentFuture(Optional.of(DATA.data4))).when(netconfService).get(DATA.path);
        final ContainerNode checkingData = Builders
                .containerBuilder()
                .withNodeIdentifier(NODE_IDENTIFIER)
                .withChild(DATA.contentLeaf)
                .withChild(DATA.contentLeaf2)
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path, mdsalStrategy);
        assertEquals(checkingData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path, netconfStrategy);
        assertEquals(checkingData, normalizedNode);
    }

    @Test
    public void readListDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.listData))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.listData2))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.listData))).when(netconfService).get(DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.listData2))).when(netconfService).getConfig(DATA.path3);
        final MapNode checkingData = Builders
                .mapBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create("ns", "2016-02-28", "list")))
                .withChild(DATA.checkData)
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path3, mdsalStrategy);
        assertEquals(checkingData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path3, netconfStrategy);
        assertEquals(checkingData, normalizedNode);
    }

    @Test
    public void readOrderedListDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedMapNode1))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedMapNode2))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedMapNode1))).when(netconfService).get(DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedMapNode2))).when(netconfService)
                .getConfig(DATA.path3);
        final MapNode expectedData = Builders.orderedMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(DATA.listQname))
                .withChild(DATA.checkData)
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path3,
                mdsalStrategy);
        assertEquals(expectedData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path3, netconfStrategy);
        assertEquals(expectedData, normalizedNode);
    }

    @Test
    public void readUnkeyedListDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.unkeyedListNode1))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.unkeyedListNode2))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.unkeyedListNode1))).when(netconfService).get(DATA.path3);
        doReturn(immediateFluentFuture(Optional.of(DATA.unkeyedListNode2))).when(netconfService)
                .getConfig(DATA.path3);
        final UnkeyedListNode expectedData = Builders.unkeyedListBuilder()
                .withNodeIdentifier(new NodeIdentifier(DATA.listQname))
                .withChild(Builders.unkeyedListEntryBuilder()
                        .withNodeIdentifier(new NodeIdentifier(DATA.listQname))
                        .withChild(DATA.unkeyedListEntryNode1.body().iterator().next())
                        .withChild(DATA.unkeyedListEntryNode2.body().iterator().next()).build()).build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.path3, mdsalStrategy);
        assertEquals(expectedData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.path3, netconfStrategy);
        assertEquals(expectedData, normalizedNode);
    }

    @Test
    public void readLeafListDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.leafSetNode1))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.leafSetNode2))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.leafSetNode1))).when(netconfService)
                .get(DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.leafSetNode2))).when(netconfService)
                .getConfig(DATA.leafSetNodePath);
        final LeafSetNode<String> expectedData = Builders.<String>leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(DATA.leafListQname))
                .withValue(ImmutableList.<LeafSetEntryNode<String>>builder()
                        .addAll(DATA.leafSetNode1.body())
                        .addAll(DATA.leafSetNode2.body())
                        .build())
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.leafSetNodePath,
                mdsalStrategy);
        assertEquals(expectedData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.leafSetNodePath, netconfStrategy);
        assertEquals(expectedData, normalizedNode);
    }

    @Test
    public void readOrderedLeafListDataAllTest() {
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedLeafSetNode1))).when(read)
                .read(LogicalDatastoreType.OPERATIONAL, DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedLeafSetNode2))).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedLeafSetNode1))).when(netconfService)
                .get(DATA.leafSetNodePath);
        doReturn(immediateFluentFuture(Optional.of(DATA.orderedLeafSetNode2))).when(netconfService)
                .getConfig(DATA.leafSetNodePath);
        final LeafSetNode<String> expectedData = Builders.<String>orderedLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(DATA.leafListQname))
                .withValue(ImmutableList.<LeafSetEntryNode<String>>builder()
                        .addAll(DATA.orderedLeafSetNode1.body())
                        .addAll(DATA.orderedLeafSetNode2.body())
                        .build())
                .build();
        NormalizedNode normalizedNode = readData(ContentParameter.ALL, DATA.leafSetNodePath,
                mdsalStrategy);
        assertEquals(expectedData, normalizedNode);

        normalizedNode = readData(ContentParameter.ALL, DATA.leafSetNodePath, netconfStrategy);
        assertEquals(expectedData, normalizedNode);
    }

    @Test
    public void readDataWrongPathOrNoContentTest() {
        doReturn(immediateFluentFuture(Optional.empty())).when(read)
                .read(LogicalDatastoreType.CONFIGURATION, DATA.path2);
        doReturn(immediateFluentFuture(Optional.empty())).when(netconfService).getConfig(DATA.path2);
        NormalizedNode normalizedNode = readData(ContentParameter.CONFIG, DATA.path2, mdsalStrategy);
        assertNull(normalizedNode);

        normalizedNode = readData(ContentParameter.CONFIG, DATA.path2, netconfStrategy);
        assertNull(normalizedNode);
    }

    /**
     * Test of parsing default parameters from URI request.
     */
    @Test
    public void parseUriParametersDefaultTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();

        // no parameters, default values should be used
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final QueryParameters parsedParameters = ReadDataTransactionUtil.parseUriParameters(context, uriInfo);

        assertEquals(ContentParameter.ALL, parsedParameters.getContent());
        assertNull(parsedParameters.getDepth());
        assertNull(parsedParameters.getFields());
    }

    /**
     * Test of parsing user defined parameters from URI request.
     */
    @Test
    public void parseUriParametersUserDefinedTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("content", "config");
        parameters.putSingle("depth", "10");
        parameters.putSingle("fields", containerChildQName.getLocalName());

        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final QueryParameters parsedParameters = ReadDataTransactionUtil.parseUriParameters(context, uriInfo);

        // content
        assertEquals(ContentParameter.CONFIG, parsedParameters.getContent());

        // depth
        final DepthParameter depth = parsedParameters.getDepth();
        assertNotNull(depth);
        assertEquals(10, depth.value());

        // fields
        assertNotNull(parsedParameters.getFields());
        assertEquals(1, parsedParameters.getFields().size());
        assertEquals(1, parsedParameters.getFields().get(0).size());
        assertEquals(containerChildQName, parsedParameters.getFields().get(0).iterator().next());
    }

    /**
     * Negative test of parsing request URI parameters when content parameter has not allowed value.
     */
    @Test
    public void parseUriParametersContentParameterNegativeTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("content", "not-allowed-parameter-value");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.parseUriParameters(context, uriInfo));
        // Bad request
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, ex.getErrors().get(0).getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, ex.getErrors().get(0).getErrorTag());
    }

    /**
     * Negative test of parsing request URI parameters when depth parameter has not allowed value.
     */
    @Test
    public void parseUriParametersDepthParameterNegativeTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();

        // inserted value is not allowed
        parameters.putSingle("depth", "bounded");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.parseUriParameters(context, uriInfo));
        // Bad request
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, ex.getErrors().get(0).getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, ex.getErrors().get(0).getErrorTag());
    }

    /**
     * Negative test of parsing request URI parameters when depth parameter has not allowed value (less than minimum).
     */
    @Test
    public void parseUriParametersDepthMinimalParameterNegativeTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();

        // inserted value is too low
        parameters.putSingle("depth", "0");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.parseUriParameters(context, uriInfo));
        // Bad request
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, ex.getErrors().get(0).getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, ex.getErrors().get(0).getErrorTag());
    }

    /**
     * Negative test of parsing request URI parameters when depth parameter has not allowed value (more than maximum).
     */
    @Test
    public void parseUriParametersDepthMaximalParameterNegativeTest() {
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();

        // inserted value is too high
        parameters.putSingle("depth", "65536");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.parseUriParameters(context, uriInfo));
        // Bad request
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, ex.getErrors().get(0).getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, ex.getErrors().get(0).getErrorTag());
    }

    /**
     * Testing parsing of with-defaults parameter which value doesn't match report-all or report-all-tagged patterns
     * - non-reporting setting.
     */
    @Test
    public void parseUriParametersWithDefaultAndNonTaggedTest() {
        // preparation of input data
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("with-defaults", "explicit");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final QueryParameters writerParameters = ReadDataTransactionUtil.parseUriParameters(context, uriInfo);
        assertSame(WithDefaultsParameter.EXPLICIT, writerParameters.getWithDefault());
        assertFalse(writerParameters.isTagged());
    }

    /**
     * Testing parsing of with-defaults parameter which value which is not supported.
     */
    @Test
    public void parseUriParametersWithDefaultInvalidTest() {
        // preparation of input data
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("with-defaults", "invalid");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.parseUriParameters(context, uriInfo));
        final List<RestconfError> errors = ex.getErrors();
        assertEquals(1, errors.size());
        assertEquals(ErrorTag.INVALID_VALUE, errors.get(0).getErrorTag());
    }

    /**
     * Testing parsing of with-defaults parameter which value matches 'report-all-tagged' setting - default value should
     * be set to {@code null} and tagged flag should be set to {@code true}.
     */
    @Test
    public void parseUriParametersWithDefaultAndTaggedTest() {
        // preparation of input data
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("with-defaults", "report-all-tagged");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final QueryParameters writerParameters = ReadDataTransactionUtil.parseUriParameters(context, uriInfo);
        assertNull(writerParameters.getWithDefault());
        assertTrue(writerParameters.isTagged());
    }

    /**
     * Testing parsing of with-defaults parameter which value matches 'report-all' setting - default value should
     * be set to {@code null} and tagged flag should be set to {@code false}.
     */
    @Test
    public void parseUriParametersWithDefaultAndReportAllTest() {
        // preparation of input data
        final UriInfo uriInfo = mock(UriInfo.class);
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("with-defaults", "report-all");
        when(uriInfo.getQueryParameters()).thenReturn(parameters);

        final QueryParameters writerParameters = ReadDataTransactionUtil.parseUriParameters(context, uriInfo);
        assertNull(writerParameters.getWithDefault());
        assertFalse(writerParameters.isTagged());
    }

    /**
     * Test when parameter is present at most once.
     */
    @Test
    public void getSingleParameterTest() {
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle(ContentParameter.uriName(), "all");
        assertEquals("all", ReadDataTransactionUtil.getSingleParameter(parameters, ContentParameter.uriName()));
    }

    /**
     * Test when parameter is present more than once.
     */
    @Test
    public void getSingleParameterNegativeTest() {
        final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.put(ContentParameter.uriName(), List.of("config", "nonconfig", "all"));

        final RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.getSingleParameter(parameters, ContentParameter.uriName()));
        final List<RestconfError> errors = ex.getErrors();
        assertEquals(1, errors.size());

        final RestconfError error = errors.get(0);
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, error.getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, error.getErrorTag());
    }

    /**
     * Test when all parameters are allowed.
     */
    @Test
    public void checkParametersTypesTest() {
        ReadDataTransactionUtil.checkParametersTypes(Set.of("content"),
            Set.of(ContentParameter.uriName(), DepthParameter.uriName()));
    }

    /**
     * Test when not allowed parameter type is used.
     */
    @Test
    public void checkParametersTypesNegativeTest() {
        final RestconfDocumentedException ex = assertThrows(RestconfDocumentedException.class,
            () -> ReadDataTransactionUtil.checkParametersTypes(Set.of("not-allowed-parameter"),
                Set.of(ContentParameter.uriName(), DepthParameter.uriName())));
        final List<RestconfError> errors = ex.getErrors();
        assertEquals(1, errors.size());

        final RestconfError error = errors.get(0);
        assertEquals("Error type is not correct", ErrorType.PROTOCOL, error.getErrorType());
        assertEquals("Error tag is not correct", ErrorTag.INVALID_VALUE, error.getErrorTag());
    }

    /**
     * Read specific type of data from data store via transaction.
     *
     * @param content        type of data to read (config, state, all)
     * @param strategy       {@link RestconfStrategy} - wrapper for variables
     * @return {@link NormalizedNode}
     */
    private @Nullable NormalizedNode readData(final @NonNull ContentParameter content,
            final YangInstanceIdentifier path, final @NonNull RestconfStrategy strategy) {
        return ReadDataTransactionUtil.readData(content, path, strategy, null, schemaContext);
    }
}
