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
package org.operaton.bpm.tasklist;

/**
 * The tasklist application. Provides access to the tasklist core services.
 *
 * @author Roman Smirnov
 *
 */
public class Tasklist {

  /**
   * The {@link TasklistRuntimeDelegate} is an delegate that will be
   * initialized by bootstrapping operaton admin with an specific
   * instance
   */
  protected static TasklistRuntimeDelegate tasklistRuntimeDelegate;

  private Tasklist() {
  }

  /**
   * Returns an instance of {@link TasklistRuntimeDelegate}
   *
   * @return
   */
  public static TasklistRuntimeDelegate getRuntimeDelegate() {
    return tasklistRuntimeDelegate;
  }

  /**
   * A setter to set the {@link TasklistRuntimeDelegate}.
   * @param tasklistRuntimeDelegate
   */
  public static void setTasklistRuntimeDelegate(TasklistRuntimeDelegate tasklistRuntimeDelegate) {
    Tasklist.tasklistRuntimeDelegate = tasklistRuntimeDelegate;
  }
}
