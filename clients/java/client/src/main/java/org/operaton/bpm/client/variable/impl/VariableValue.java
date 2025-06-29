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
package org.operaton.bpm.client.variable.impl;

import org.operaton.bpm.client.variable.impl.value.DeferredFileValueImpl;
import org.operaton.bpm.engine.variable.value.SerializableValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class VariableValue<T extends TypedValue> {

  protected String executionId;
  protected String variableName;
  protected TypedValueField typedValueField;
  protected ValueMappers<T> mappers;

  protected ValueMapper<T> serializer;
  protected T cachedValue;

  public VariableValue(String executionId, String variableName, TypedValueField typedValueField, ValueMappers<T> mappers) {
    this.executionId = executionId;
    this.variableName = variableName;
    this.typedValueField = typedValueField;
    this.mappers = mappers;
  }

  public Object getValue() {
    TypedValue typedValue = getTypedValue();
    if (typedValue != null) {
      return typedValue.getValue();
    } else {
      return null;
    }
  }

  public T getTypedValue() {
    return getTypedValue(true);
  }

  public T getTypedValue(boolean deserializeValue) {
    if (deserializeValue && cachedValue instanceof SerializableValue serializableValue && !serializableValue.isDeserialized()) {
      cachedValue = null;
    }

    if (cachedValue == null) {
      cachedValue = getSerializer().readValue(typedValueField, deserializeValue);

      if (cachedValue instanceof DeferredFileValueImpl fileValue) {
        fileValue.setExecutionId(executionId);
        fileValue.setVariableName(variableName);
      }
    }

    return cachedValue;
  }

  public ValueMapper<T> getSerializer() {
    if (serializer == null) {
      serializer = mappers.findMapperForTypedValueField(typedValueField);
    }
    return serializer;
  }

  @Override
  public String toString() {
    return "VariableValue ["
        + "cachedValue=" + cachedValue + ", "
        + "executionId=" + executionId + ", "
        + "variableName=" + variableName + ", "
        + "typedValueField=" + typedValueField + "]";
  }


}
