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
package org.operaton.bpm.tasklist.impl.plugin;

import java.util.HashSet;
import java.util.Set;

import org.operaton.bpm.tasklist.impl.plugin.resources.TasklistPluginsRootResource;
import org.operaton.bpm.tasklist.plugin.spi.impl.AbstractTasklistPlugin;

/**
 *
 * @author Roman Smirnov
 */
public class TasklistPlugins extends AbstractTasklistPlugin {

  public static final String ID = "tasklistPlugins";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public Set<Class<?>> getResourceClasses() {
    HashSet<Class<?>> classes = new HashSet<>();

    classes.add(TasklistPluginsRootResource.class);

    return classes;
  }

  @Override
  public String getAssetDirectory() {
    return "plugin/tasklist";
  }

}
