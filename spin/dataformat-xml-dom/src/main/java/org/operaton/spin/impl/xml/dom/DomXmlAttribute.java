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
package org.operaton.spin.impl.xml.dom;

import java.io.IOException;
import java.io.Writer;

import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.spi.DataFormatMapper;
import org.operaton.spin.xml.SpinXmlAttribute;
import org.operaton.spin.xml.SpinXmlElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * Wrapper of a xml dom attribute.
 *
 * @author Sebastian Menski
 */
public class DomXmlAttribute extends SpinXmlAttribute {

  private static final DomXmlLogger LOG = DomXmlLogger.XML_DOM_LOGGER;

  protected final Attr attributeNode;

  protected final DomXmlDataFormat dataFormat;

  /**
   * Create a new wrapper.
   *
   * @param attributeNode the dom xml attribute to wrap
   * @param dataFormat the xml dom data format
   */
  public DomXmlAttribute(Attr attributeNode, DomXmlDataFormat dataFormat) {
    this.attributeNode = attributeNode;
    this.dataFormat = dataFormat;
  }

  @Override
  public String getDataFormatName() {
    return dataFormat.getName();
  }

  @Override
  public Attr unwrap() {
    return attributeNode;
  }

  @Override
  public String name() {
    return attributeNode.getLocalName();
  }

  @Override
  public String namespace() {
    return attributeNode.getNamespaceURI();
  }

  @Override
  public String prefix() {
    return attributeNode.getPrefix();
  }

  @Override
  public boolean hasPrefix(String prefix) {
    String attributePrefix = attributeNode.getPrefix();
    if(attributePrefix == null) {
      return prefix == null;
    } else {
      return attributePrefix.equals(prefix);
    }
  }

  @Override
  public boolean hasNamespace(String namespace) {
    String attributeNamespace = attributeNode.getNamespaceURI();
    if (attributeNamespace == null) {
      return namespace == null;
    }
    else {
      return attributeNamespace.equals(namespace);
    }
  }

  @Override
  public String value() {
    return attributeNode.getValue();
  }

  @Override
  public SpinXmlAttribute value(String value) {
    if (value == null) {
      throw LOG.unableToSetAttributeValueToNull(namespace(), name());
    }
    attributeNode.setValue(value);
    return this;
  }

  @Override
  public SpinXmlElement remove() {
    Element ownerElement = attributeNode.getOwnerElement();
    ownerElement.removeAttributeNode(attributeNode);
    return dataFormat.createElementWrapper(ownerElement);
  }

  @Override
  public String toString() {
    return value();
  }

  @Override
  public void writeToWriter(Writer writer) {
    try {
      writer.write(toString());
    } catch (IOException e) {
      throw LOG.unableToWriteAttribute(this, e);
    }
  }

  @Override
  public <C> C mapTo(Class<C> javaClass) {
    DataFormatMapper mapper = dataFormat.getMapper();
    return mapper.mapInternalToJava(this, javaClass);
  }

  @Override
  public <C> C mapTo(String javaClass) {
    DataFormatMapper mapper = dataFormat.getMapper();
    return mapper.mapInternalToJava(this, javaClass);
  }

}
