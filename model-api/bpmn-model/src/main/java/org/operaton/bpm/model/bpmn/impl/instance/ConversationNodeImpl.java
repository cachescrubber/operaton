/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN conversationNode element
 *
 * @author Sebastian Menski
 */
public abstract class ConversationNodeImpl extends BaseElementImpl implements ConversationNode {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<Participant, ParticipantRef> participantRefCollection;
  protected static ElementReferenceCollection<MessageFlow, MessageFlowRef> messageFlowRefCollection;
  protected static ChildElementCollection<CorrelationKey> correlationKeyCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConversationNode.class, BPMN_ELEMENT_CONVERSATION_NODE)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .abstractType();

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    participantRefCollection = sequenceBuilder.elementCollection(ParticipantRef.class)
      .qNameElementReferenceCollection(Participant.class)
      .build();

    messageFlowRefCollection = sequenceBuilder.elementCollection(MessageFlowRef.class)
      .qNameElementReferenceCollection(MessageFlow.class)
      .build();

    correlationKeyCollection = sequenceBuilder.elementCollection(CorrelationKey.class)
      .build();

    typeBuilder.build();
  }

  protected ConversationNodeImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Collection<Participant> getParticipants() {
    return participantRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<MessageFlow> getMessageFlows() {
    return messageFlowRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<CorrelationKey> getCorrelationKeys() {
    return correlationKeyCollection.get(this);
  }
}
