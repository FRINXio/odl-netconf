/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.restconf.nb.rfc8040.jersey.providers;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

/**
 * Unit test for {@link ParameterAwareNormalizedNodeWriter} used with fields parameter.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ParameterAwareNormalizedNodeWriterFieldsTest {

    @Mock
    private NormalizedNodeStreamWriter writer;
    @Mock
    private ContainerNode containerNodeData;
    @Mock
    private MapNode mapNodeData;
    @Mock
    private MapEntryNode mapEntryNodeData;
    @Mock
    private LeafSetNode<String> leafSetNodeData;
    @Mock
    private LeafSetEntryNode<String> leafSetEntryNodeData;
    @Mock
    private LeafNode<String> keyLeafNodeData;

    private NodeIdentifier containerNodeIdentifier;
    private NodeIdentifier mapNodeIdentifier;
    private NodeIdentifierWithPredicates mapEntryNodeIdentifier;
    private NodeIdentifier leafSetNodeIdentifier;
    private NodeWithValue<String> leafSetEntryNodeIdentifier;
    private NodeIdentifier keyLeafNodeIdentifier;

    private Collection<DataContainerChild> containerNodeValue;
    private Collection<MapEntryNode> mapNodeValue;
    private Collection<DataContainerChild> mapEntryNodeValue;
    private Collection<LeafSetEntryNode<String>> leafSetNodeValue;
    private String leafSetEntryNodeValue;
    private String keyLeafNodeValue;

    @Before
    public void setUp() {
        // identifiers
        containerNodeIdentifier = NodeIdentifier.create(QName.create("namespace", "container"));
        Mockito.when(containerNodeData.getIdentifier()).thenReturn(containerNodeIdentifier);

        mapNodeIdentifier = NodeIdentifier.create(QName.create("namespace", "list"));
        Mockito.when(mapNodeData.getIdentifier()).thenReturn(mapNodeIdentifier);

        final QName leafSetEntryNodeQName = QName.create("namespace", "leaf-set-entry");
        leafSetEntryNodeValue = "leaf-set-value";
        leafSetEntryNodeIdentifier = new NodeWithValue<>(leafSetEntryNodeQName, leafSetEntryNodeValue);
        Mockito.when(leafSetEntryNodeData.getIdentifier()).thenReturn(leafSetEntryNodeIdentifier);

        leafSetNodeIdentifier = NodeIdentifier.create(QName.create("namespace", "leaf-set"));
        Mockito.when(leafSetNodeData.getIdentifier()).thenReturn(leafSetNodeIdentifier);

        final QName mapEntryNodeKey = QName.create("namespace", "key-field");
        keyLeafNodeIdentifier = NodeIdentifier.create(mapEntryNodeKey);
        keyLeafNodeValue = "key-value";

        mapEntryNodeIdentifier = NodeIdentifierWithPredicates.of(
                QName.create("namespace", "list-entry"), mapEntryNodeKey, keyLeafNodeValue);
        Mockito.when(mapEntryNodeData.getIdentifier()).thenReturn(mapEntryNodeIdentifier);
        Mockito.when(mapEntryNodeData.findChildByArg(keyLeafNodeIdentifier)).thenReturn(Optional.of(keyLeafNodeData));

        Mockito.when(keyLeafNodeData.body()).thenReturn(keyLeafNodeValue);
        Mockito.when(keyLeafNodeData.getIdentifier()).thenReturn(keyLeafNodeIdentifier);

        // values
        Mockito.when(leafSetEntryNodeData.body()).thenReturn(leafSetEntryNodeValue);

        leafSetNodeValue = Collections.singletonList(leafSetEntryNodeData);
        Mockito.when(leafSetNodeData.body()).thenReturn(leafSetNodeValue);

        containerNodeValue = Collections.singleton(leafSetNodeData);
        Mockito.when(containerNodeData.body()).thenReturn(containerNodeValue);

        mapEntryNodeValue = Sets.newHashSet(keyLeafNodeData);
        Mockito.when(mapEntryNodeData.body()).thenReturn(mapEntryNodeValue);

        mapNodeValue = Collections.singleton(mapEntryNodeData);
        Mockito.when(mapNodeData.body()).thenReturn(mapNodeValue);
    }

    /**
     * Test write {@link ContainerNode} when children which will be written are limited.
     * Fields parameter selects 0/1 of container children to be written.
     */
    @Test
    public void writeContainerWithLimitedFieldsTest() throws Exception {
        final List<Set<QName>> limitedFields = new ArrayList<>();
        limitedFields.add(new HashSet<>());

        final ParameterAwareNormalizedNodeWriter parameterWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
                writer, null, limitedFields);

        parameterWriter.write(containerNodeData);

        final InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer, Mockito.times(1)).startContainerNode(containerNodeIdentifier, containerNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).endNode();
        Mockito.verifyNoMoreInteractions(writer);
    }

    /**
     * Test write {@link ContainerNode} when all its children are selected to be written.
     * Fields parameter selects 1/1 of container children to be written.
     */
    @Test
    public void writeContainerAllFieldsTest() throws Exception {
        final List<Set<QName>> limitedFields = new ArrayList<>();
        limitedFields.add(Sets.newHashSet(leafSetNodeIdentifier.getNodeType()));

        final ParameterAwareNormalizedNodeWriter parameterWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
                writer, null, limitedFields);

        parameterWriter.write(containerNodeData);

        final InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer, Mockito.times(1)).startContainerNode(containerNodeIdentifier, containerNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).startLeafSet(leafSetNodeIdentifier, leafSetNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).startLeafSetEntryNode(leafSetEntryNodeIdentifier);
        inOrder.verify(writer, Mockito.times(1)).scalarValue(leafSetEntryNodeValue);
        inOrder.verify(writer, Mockito.times(3)).endNode();
        Mockito.verifyNoMoreInteractions(writer);
    }

    /**
     * Test write {@link MapEntryNode} as child of {@link MapNode} when children which will be written are limited.
     * Fields parameter selects 0/1 of map entry node children to be written.
     */
    @Test
    public void writeMapEntryNodeWithLimitedFieldsTest() throws Exception {
        final List<Set<QName>> limitedFields = new ArrayList<>();
        limitedFields.add(new HashSet<>());

        final ParameterAwareNormalizedNodeWriter parameterWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
                writer, null, limitedFields);

        parameterWriter.write(mapNodeData);

        final InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer, Mockito.times(1)).startMapNode(mapNodeIdentifier, mapNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).startMapEntryNode(mapEntryNodeIdentifier, mapEntryNodeValue.size());
        inOrder.verify(writer, Mockito.times(2)).endNode();
        Mockito.verifyNoMoreInteractions(writer);
    }

    /**
     * Test write {@link MapEntryNode} as child of {@link MapNode} when all its children will be written.
     * Fields parameter selects 1/1 of map entry node children to be written.
     */
    @Test
    public void writeMapNodeAllFieldsTest() throws Exception {
        final List<Set<QName>> limitedFields = new ArrayList<>();
        limitedFields.add(Sets.newHashSet(keyLeafNodeData.getIdentifier().getNodeType()));

        final ParameterAwareNormalizedNodeWriter parameterWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
                writer, null, limitedFields);

        parameterWriter.write(mapNodeData);

        final InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer, Mockito.times(1)).startMapNode(mapNodeIdentifier, mapNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).startMapEntryNode(mapEntryNodeIdentifier, mapEntryNodeValue.size());
        inOrder.verify(writer, Mockito.times(1)).startLeafNode(keyLeafNodeIdentifier);
        inOrder.verify(writer, Mockito.times(1)).scalarValue(keyLeafNodeValue);
        inOrder.verify(writer, Mockito.times(3)).endNode();
        Mockito.verifyNoMoreInteractions(writer);
    }
}