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

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.runtime.Incident;

/**
 *
 * @author Anna Pazola
 *
 */
public class CreateIncidentCmd implements Command<Incident> {

  protected String incidentType;
  protected String executionId;
  protected String configuration;
  protected String message;

  public CreateIncidentCmd(String incidentType, String executionId, String configuration, String message) {
    this.incidentType = incidentType;
    this.executionId = executionId;
    this.configuration = configuration;
    this.message = message;
  }

  @Override
  public Incident execute(CommandContext commandContext) {
    EnsureUtil.ensureNotNull(BadUserRequestException.class, "Execution id cannot be null", "executionId", executionId);
    EnsureUtil.ensureNotNull(BadUserRequestException.class, "incidentType", incidentType);

    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);
    EnsureUtil.ensureNotNull(BadUserRequestException.class,
        "Cannot find an execution with executionId '" + executionId + "'", "execution", execution);
    EnsureUtil.ensureNotNull(BadUserRequestException.class, "Execution must be related to an activity", "activity",
        execution.getActivity());

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstance(execution);
    }

    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("incidentType", null, incidentType));
    propertyChanges.add(new PropertyChange("configuration", null, configuration));

    commandContext.getOperationLogManager().logProcessInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_CREATE_INCIDENT,
        execution.getProcessInstanceId(), execution.getProcessDefinitionId(), null,
        propertyChanges);

    return execution.createIncident(incidentType, configuration, message);
  }
}
