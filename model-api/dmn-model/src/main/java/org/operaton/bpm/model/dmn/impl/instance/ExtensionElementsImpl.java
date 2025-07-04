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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_EXTENSION_ELEMENTS;

import java.util.Collection;

import org.operaton.bpm.model.dmn.Query;
import org.operaton.bpm.model.dmn.impl.QueryImpl;
import org.operaton.bpm.model.dmn.instance.ExtensionElements;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The DMN extensionElements element
 */
public class ExtensionElementsImpl extends DmnModelElementInstanceImpl implements ExtensionElements {

  public static void registerType(ModelBuilder modelBuilder) {

    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ExtensionElements.class, DMN_ELEMENT_EXTENSION_ELEMENTS)
      .namespaceUri(LATEST_DMN_NS)
      .instanceProvider(ExtensionElementsImpl::new);

    typeBuilder.build();
  }

  public ExtensionElementsImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public Collection<ModelElementInstance> getElements() {
    return ModelUtil.getModelElementCollection(getDomElement().getChildElements(), modelInstance);
  }

  @Override
  public Query<ModelElementInstance> getElementsQuery() {
    return new QueryImpl<>(getElements());
  }

  @Override
  public ModelElementInstance addExtensionElement(String namespaceUri, String localName) {
    ModelElementType extensionElementType = modelInstance.registerGenericType(namespaceUri, localName);
    ModelElementInstance extensionElement = extensionElementType.newInstance(modelInstance);
    addChildElement(extensionElement);
    return extensionElement;
  }

  @Override
  public <T extends ModelElementInstance> T addExtensionElement(Class<T> extensionElementClass) {
    ModelElementInstance extensionElement = modelInstance.newInstance(extensionElementClass);
    addChildElement(extensionElement);
    return extensionElementClass.cast(extensionElement);
  }

  @Override
  public void addChildElement(ModelElementInstance extensionElement) {
    getDomElement().appendChild(extensionElement.getDomElement());
  }

}
