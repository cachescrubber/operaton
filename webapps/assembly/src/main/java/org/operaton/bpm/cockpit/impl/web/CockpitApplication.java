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
package org.operaton.bpm.cockpit.impl.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.core.Application;
import org.operaton.bpm.cockpit.Cockpit;
import org.operaton.bpm.cockpit.plugin.spi.CockpitPlugin;
import org.operaton.bpm.engine.rest.exception.ExceptionHandler;
import org.operaton.bpm.engine.rest.exception.RestExceptionHandler;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;


/**
 * The cockpit rest api exposed by the application.
 *
 * @author nico.rehwaldt
 */
public class CockpitApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();

    classes.add(JacksonConfigurator.class);
    classes.add(JacksonJsonProvider.class);
    classes.add(ExceptionHandler.class);
    classes.add(RestExceptionHandler.class);

    addPluginResourceClasses(classes);

    return classes;
  }

  private void addPluginResourceClasses(Set<Class<?>> classes) {

    List<CockpitPlugin> plugins = getCockpitPlugins();

    for (CockpitPlugin plugin : plugins) {
      classes.addAll(plugin.getResourceClasses());
    }
  }

  private List<CockpitPlugin> getCockpitPlugins() {
    return Cockpit.getRuntimeDelegate().getPluginRegistry().getPlugins();
  }
}
