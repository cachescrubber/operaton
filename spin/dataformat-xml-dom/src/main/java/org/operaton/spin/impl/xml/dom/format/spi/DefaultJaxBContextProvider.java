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
package org.operaton.spin.impl.xml.dom.format.spi;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.operaton.spin.impl.xml.dom.DomXmlLogger;

/**
 * Simple implementation for the JaxBContextProvider interface returning a new context
 * each time it is invoked. This implementation does not perform any kind of caching.
 *
 * @author Daniel Meyer
 *
 */
public class DefaultJaxBContextProvider implements JaxBContextProvider {

  private static final DomXmlLogger LOG = DomXmlLogger.XML_DOM_LOGGER;

  public JAXBContext getContext(Class<?>... types) {
    var additionalInfo = new StringBuilder();
    try {
      var serviceLoader = ServiceLoader.load(JAXBContextFactory.class);
      switch ((int) serviceLoader.stream().count()) {
        case 0:
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          logClassLoader(cl, additionalInfo);
          throw new JAXBException("No JAXBContextFactory implementation found");
        case 1:
          return serviceLoader.iterator().next().createContext(types, Collections.emptyMap());
        default:
          throw new JAXBException("Multiple JAXBContextFactory implementations found: " + serviceLoader.stream().map(sl -> sl.get().getClass()));
      }
    }
    catch (JAXBException e) {
      throw LOG.unableToCreateContext(e, additionalInfo.toString());
    }
  }

  private void logClassLoader(ClassLoader cl, StringBuilder sb) {
    if (cl instanceof URLClassLoader urlClassLoader) {
      var urls = Stream.of(urlClassLoader.getURLs()).map(URL::toString).collect(Collectors.joining(","));
      sb.append("java.net.URLClassLoader: ").append(urls);
    } else {
      sb.append(cl.getClass().getName());
    }
    if (cl.getParent() != null) {
      sb.append("\n -> ");
      logClassLoader(cl.getParent(), sb);
    }
  }

  @Override
  public Marshaller createMarshaller(Class<?>... types) {
    try {
      return getContext(types).createMarshaller();
    } catch (JAXBException e) {
      throw LOG.unableToCreateMarshaller(e);
    }
  }

  @Override
  public Unmarshaller createUnmarshaller(Class<?>... types) {
    try {
      return getContext(types).createUnmarshaller();
    } catch (JAXBException e) {
      throw LOG.unableToCreateUnmarshaller(e);
    }
  }

}
