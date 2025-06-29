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

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 * @author Thorben Lindhauer
 * @author Christopher Zell
 */
public class SetExternalTaskRetriesCmd extends ExternalTaskCmd {

  protected int retries;
  protected boolean shouldWriteUserOperationLog;

  public SetExternalTaskRetriesCmd(String externalTaskId, int retries, boolean shouldWriteUserOperationLog) {
    super(externalTaskId);
    this.retries = retries;
    this.shouldWriteUserOperationLog = shouldWriteUserOperationLog;
  }

  @Override
  protected void validateInput() {
    EnsureUtil.ensureGreaterThanOrEqual(BadUserRequestException.class, "The number of retries cannot be negative", "retries", retries, 0);
  }

  @Override
  protected void execute(ExternalTaskEntity externalTask) {
    externalTask.setRetriesAndManageIncidents(retries);
  }

  @Override
  protected String getUserOperationLogOperationType() {
    if (shouldWriteUserOperationLog) {
      return UserOperationLogEntry.OPERATION_TYPE_SET_EXTERNAL_TASK_RETRIES;
    }
    return super.getUserOperationLogOperationType();
  }

  @Override
  protected List<PropertyChange> getUserOperationLogPropertyChanges(ExternalTaskEntity externalTask) {
    if (shouldWriteUserOperationLog) {
      return Collections.singletonList(new PropertyChange("retries", externalTask.getRetries(), retries));
    }
    return super.getUserOperationLogPropertyChanges(externalTask);
  }
}
