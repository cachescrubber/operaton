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
package org.operaton.bpm.model.bpmn.impl.instance.dc;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.dc.Bounds;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The DC bounds element
 *
 * @author Sebastian Menski
 */
public class BoundsImpl extends BpmnModelElementInstanceImpl implements Bounds {

  protected static Attribute<Double> xAttribute;
  protected static Attribute<Double> yAttribute;
  protected static Attribute<Double> widthAttribute;
  protected static Attribute<Double> heightAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Bounds.class, DC_ELEMENT_BOUNDS)
      .namespaceUri(DC_NS)
      .instanceProvider(BoundsImpl::new);

    xAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_X)
      .required()
      .build();

    yAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_Y)
      .required()
      .build();

    widthAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_WIDTH)
      .required()
      .build();

    heightAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_HEIGHT)
      .required()
      .build();

    typeBuilder.build();
  }

  public BoundsImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Double getX() {
    return xAttribute.getValue(this);
  }

  @Override
  public void setX(double x) {
    xAttribute.setValue(this, x);
  }

  @Override
  public Double getY() {
    return yAttribute.getValue(this);
  }

  @Override
  public void setY(double y) {
    yAttribute.setValue(this, y);
  }

  @Override
  public Double getWidth() {
    return widthAttribute.getValue(this);
  }

  @Override
  public void setWidth(double width) {
    widthAttribute.setValue(this, width);
  }

  @Override
  public Double getHeight() {
    return heightAttribute.getValue(this);
  }

  @Override
  public void setHeight(double height) {
    heightAttribute.setValue(this, height);
  }
}
