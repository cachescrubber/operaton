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
package org.operaton.bpm.engine.repository;

/**
 * Stores position and dimensions of a diagram node.
 *
 * @author Falko Menge
 */
public class DiagramNode extends DiagramElement {

  private static final long serialVersionUID = 1L;

  private Double x = null;
  private Double y = null;
  private Double width = null;
  private Double height = null;

  public DiagramNode() {
    super();
  }

  public DiagramNode(String id) {
    super(id);
  }

  public DiagramNode(String id, Double x, Double y, Double width, Double height) {
    super(id);
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public Double getX() {
    return x;
  }

  public void setX(Double x) {
    this.x = x;
  }

  public Double getY() {
    return y;
  }

  public void setY(Double y) {
    this.y = y;
  }

  public Double getWidth() {
    return width;
  }

  public void setWidth(Double width) {
    this.width = width;
  }

  public Double getHeight() {
    return height;
  }

  public void setHeight(Double height) {
    this.height = height;
  }

  @Override
  public String toString() {
    return super.toString() + ", x=" + getX() + ", y=" + getY() + ", width=" + getWidth() + ", height=" + getHeight();
  }

  @Override
  public boolean isNode() {
    return true;
  }

  @Override
  public boolean isEdge() {
    return false;
  }

}
