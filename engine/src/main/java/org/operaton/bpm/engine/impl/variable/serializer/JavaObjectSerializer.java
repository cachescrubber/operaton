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
package org.operaton.bpm.engine.impl.variable.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;

/**
 * Uses default java serialization to serialize java objects as byte streams.
 *
 * @author Daniel Meyer
 * @author Tom Baeyens
 */
public class JavaObjectSerializer extends AbstractObjectValueSerializer {

  public static final String NAME = "serializable";

  public JavaObjectSerializer() {
    super(SerializationDataFormats.JAVA.getName());
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected boolean isSerializationTextBased() {
    return false;
  }

  @Override
  protected Object deserializeFromByteArray(byte[] bytes, String objectTypeName) throws Exception {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ClassloaderAwareObjectInputStream(bais)) {
      return ois.readObject();
    }
  }

  @Override
  protected byte[] serializeToByteArray(Object deserializedObject) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream ois = new ObjectOutputStream(baos)) {
      ois.writeObject(deserializedObject);
      return baos.toByteArray();
    }
  }

  @Override
  protected String getTypeNameForDeserialized(Object deserializedObject) {
    return deserializedObject.getClass().getName();
  }

  @Override
  protected boolean canSerializeValue(Object value) {
    return value instanceof Serializable;
  }

  protected static class ClassloaderAwareObjectInputStream extends ObjectInputStream {

    public ClassloaderAwareObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) {
      return ReflectUtil.loadClass(desc.getName());
    }

  }
}
