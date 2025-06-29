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
package org.operaton.bpm.engine.impl.cmd;

import java.util.Collections;
import java.util.List;

import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

/**
 * Represents the command to set the priority of an existing external task.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class SetExternalTaskPriorityCmd extends ExternalTaskCmd {

  /**
   * The priority that should set on the external task.
   */
  protected long priority;

  public SetExternalTaskPriorityCmd(String externalTaskId, long priority) {
    super(externalTaskId);
    this.priority = priority;
  }

  @Override
  protected void execute(ExternalTaskEntity externalTask) {
    externalTask.setPriority(priority);
  }

  @Override
  protected void validateInput() {
    // no input validation needed
  }

  @Override
  protected String getUserOperationLogOperationType() {
    return UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY;
  }

  @Override
  protected List<PropertyChange> getUserOperationLogPropertyChanges(ExternalTaskEntity externalTask) {
    return Collections.singletonList(new PropertyChange("priority", externalTask.getPriority(), priority));
  }
}
