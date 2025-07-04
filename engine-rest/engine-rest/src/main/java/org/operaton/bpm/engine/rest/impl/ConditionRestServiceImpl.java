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
package org.operaton.bpm.engine.rest.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.rest.ConditionRestService;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.condition.EvaluationConditionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.runtime.ConditionEvaluationBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.VariableMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConditionRestServiceImpl extends AbstractRestProcessEngineAware implements ConditionRestService {

  public ConditionRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public List<ProcessInstanceDto> evaluateCondition(EvaluationConditionDto conditionDto) {
    if (conditionDto.getTenantId() != null && conditionDto.isWithoutTenantId()) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Parameter 'tenantId' cannot be used together with parameter 'withoutTenantId'.");
    }
    ConditionEvaluationBuilder builder = createConditionEvaluationBuilder(conditionDto);
    List<ProcessInstance> processInstances = builder.evaluateStartConditions();

    List<ProcessInstanceDto> result = new ArrayList<>();
    for (ProcessInstance processInstance : processInstances) {
      result.add(ProcessInstanceDto.fromProcessInstance(processInstance));
    }
    return result;
  }

  protected ConditionEvaluationBuilder createConditionEvaluationBuilder(EvaluationConditionDto conditionDto) {
    RuntimeService runtimeService = getProcessEngine().getRuntimeService();

    ObjectMapper objectMapper = getObjectMapper();

    VariableMap variables = VariableValueDto.toMap(conditionDto.getVariables(), getProcessEngine(), objectMapper);

    ConditionEvaluationBuilder builder = runtimeService.createConditionEvaluation();

    if (variables != null && !variables.isEmpty()) {
      builder.setVariables(variables);
    }

    if (conditionDto.getBusinessKey() != null) {
      builder.processInstanceBusinessKey(conditionDto.getBusinessKey());
    }

    if (conditionDto.getProcessDefinitionId() != null) {
      builder.processDefinitionId(conditionDto.getProcessDefinitionId());
    }

    if (conditionDto.getTenantId() != null) {
      builder.tenantId(conditionDto.getTenantId());
    } else if (conditionDto.isWithoutTenantId()) {
      builder.withoutTenantId();
    }

    return builder;
  }

}
