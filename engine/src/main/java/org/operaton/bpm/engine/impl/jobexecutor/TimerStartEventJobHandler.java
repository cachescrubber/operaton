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
package org.operaton.bpm.engine.impl.jobexecutor;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;


public class TimerStartEventJobHandler extends TimerEventJobHandler {

  private static final JobExecutorLogger LOG = ProcessEngineLogger.JOB_EXECUTOR_LOGGER;

  public static final String TYPE = "timer-start-event";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(TimerJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    DeploymentCache deploymentCache = Context
            .getProcessEngineConfiguration()
            .getDeploymentCache();

    String definitionKey = configuration.getTimerElementKey();
    ProcessDefinition processDefinition = deploymentCache.findDeployedLatestProcessDefinitionByKeyAndTenantId(definitionKey, tenantId);

    try {
      startProcessInstance(commandContext, tenantId, processDefinition);
    }
    catch (RuntimeException e) {
      throw e;
    }
  }

  protected void startProcessInstance(CommandContext commandContext, String tenantId, ProcessDefinition processDefinition) {
    if(!processDefinition.isSuspended()) {

      RuntimeService runtimeService = commandContext.getProcessEngineConfiguration().getRuntimeService();
      runtimeService.createProcessInstanceByKey(processDefinition.getKey()).processDefinitionTenantId(tenantId).execute();

    } else {
      LOG.ignoringSuspendedJob(processDefinition);
    }
  }
}
