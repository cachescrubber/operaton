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
package org.operaton.bpm.engine.impl.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.form.FormField;
import org.operaton.bpm.engine.form.FormFieldValidationConstraint;
import org.operaton.bpm.engine.form.FormType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 *
 * @author Daniel Meyer
 *
 */
public class FormFieldImpl implements FormField {

  protected boolean businessKey;
  protected String id;
  protected String label;
  protected FormType type;
  protected Object defaultValue;
  protected TypedValue value;
  protected List<FormFieldValidationConstraint> validationConstraints = new ArrayList<>();
  protected Map<String, String> properties = new HashMap<>();

  // getters / setters ///////////////////////////////////////////

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public FormType getType() {
    return type;
  }

  @Override
  public String getTypeName() {
    return type.getName();
  }

  public void setType(FormType type) {
    this.type = type;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public TypedValue getValue() {
    return value;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setValue(TypedValue value) {
    this.value = value;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public List<FormFieldValidationConstraint> getValidationConstraints() {
    return validationConstraints;
  }

  public void setValidationConstraints(List<FormFieldValidationConstraint> validationConstraints) {
    this.validationConstraints = validationConstraints;
  }

  @Override
  public boolean isBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(boolean businessKey) {
    this.businessKey = businessKey;
  }
}
