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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@ExtendWith(ProcessEngineExtension.class)
class ProcessInstanceQueryOrTest {

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;

  List<String> deploymentIds = new ArrayList<>();

  @AfterEach
  void deleteDeployments() {
    for (String deploymentId : deploymentIds) {
      repositoryService.deleteDeployment(deploymentId, true);
    }
  }

  @Test
  void shouldThrowExceptionByMissingStartOr() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().or().endOr();

    // when/then
    assertThatThrownBy(processInstanceQuery::endOr)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set endOr() before or()");
  }

  @Test
  void shouldThrowExceptionByNesting() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().or();

    // when/then
    assertThatThrownBy(processInstanceQuery::or)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set or() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByProcessInstanceId() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery()
      .or();

    // when/then
    assertThatThrownBy(processInstanceQuery::orderByProcessInstanceId)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByProcessDefinitionId() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery()
      .or();

    // when/then
    assertThatThrownBy(processInstanceQuery::orderByProcessDefinitionId)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByProcessDefinitionKey() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery()
      .or();

    // when/then
    assertThatThrownBy(processInstanceQuery::orderByProcessDefinitionKey)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionKey() within 'or' query");

  }

  @Test
  void shouldThrowExceptionOnOrderByTenantId() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().or();

    // when/then
    assertThatThrownBy(processInstanceQuery::orderByTenantId)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTenantId() within 'or' query");
  }

  @Test
  void shouldThrowExceptionOnOrderByBusinessKey() {
    // given
    var processInstanceQuery = runtimeService.createProcessInstanceQuery().or();

    // when/then
    assertThatThrownBy(processInstanceQuery::orderByBusinessKey)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByBusinessKey() within 'or' query");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstWithEmptyOrQuery() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstWithVarValue1OrVarValue2() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .variableValueEquals("stringVar", "abcdef")
          .variableValueEquals("stringVar", "ghijkl")
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstWithMultipleOrCriteria() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance2.getProcessInstanceId(), "aVarName", "varValue");

    vars = new HashMap<>();
    vars.put("stringVar2", "aaabbbaaa");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance3.getProcessInstanceId(), "bVarName", "bTestb");

    vars = new HashMap<>();
    vars.put("stringVar2", "cccbbbccc");
    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance4.getProcessInstanceId(), "bVarName", "aTesta");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .variableValueEquals("stringVar", "abcdef")
          .variableValueLike("stringVar2", "%bbb%")
          .processInstanceId(processInstance1.getId())
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(4);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstFilteredByMultipleOrAndCriteria() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 12345L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .variableValueEquals("longVar", 56789L)
          .processInstanceId(processInstance1.getId())
        .endOr()
        .variableValueEquals("stringVar", "abcdef")
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstFilteredByMultipleOrQueries() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 56789L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 12345L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    vars.put("boolVar", false);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    vars.put("boolVar", false);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .variableValueEquals("stringVar", "abcdef")
          .variableValueEquals("longVar", 12345L)
        .endOr()
        .or()
          .variableValueEquals("boolVar", true)
          .variableValueEquals("longVar", 12345L)
        .endOr()
        .or()
          .variableValueEquals("stringVar", "ghijkl")
          .variableValueEquals("longVar", 56789L)
        .endOr()
        .or()
          .variableValueEquals("stringVar", "ghijkl")
          .variableValueEquals("boolVar", false)
          .variableValueEquals("longVar", 56789L)
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstWhereSameCriterionWasAppliedThreeTimesInOneQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .processInstanceId(processInstance1.getId())
          .processInstanceId(processInstance2.getId())
          .processInstanceId(processInstance3.getId())
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnProcInstWithVariableValueEqualsOrVariableValueGreaterThan() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("longVar", 12345L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longerVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longerVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
        .or()
          .variableValueEquals("longVar", 12345L)
          .variableValueGreaterThan("longerVar", 20000L)
        .endOr();

    // then
    assertThat(query.count()).isEqualTo(3);
  }

  @Test
  void shouldReturnProcInstWithProcessDefinitionNameOrProcessDefinitionKey() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
        .name("process1")
        .startEvent()
          .userTask()
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("aProcessDefinition");

    BpmnModelInstance anotherProcessDefinition = Bpmn.createExecutableProcess("anotherProcessDefinition")
        .startEvent()
          .userTask()
        .endEvent()
        .done();

    deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", anotherProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("anotherProcessDefinition");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .processDefinitionId(processInstance1.getProcessDefinitionId())
          .processDefinitionKey("anotherProcessDefinition")
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  void shouldReturnProcInstWithBusinessKeyOrBusinessKeyLike() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
        .startEvent()
          .userTask()
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("aProcessDefinition", "aBusinessKey");

    BpmnModelInstance anotherProcessDefinition = Bpmn.createExecutableProcess("anotherProcessDefinition")
        .startEvent()
          .userTask()
        .endEvent()
        .done();

    deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", anotherProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("anotherProcessDefinition", "anotherBusinessKey");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .processInstanceBusinessKey("aBusinessKey")
          .processInstanceBusinessKeyLike("anotherBusinessKey")
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  void shouldReturnProcInstByVariableAndActiveProcesses() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("oneTaskProcess")
        .startEvent()
          .userTask("testQuerySuspensionStateTask")
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    // start two process instance and leave them active
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // start one process instance and suspend it
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", 0);
    ProcessInstance suspendedProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    runtimeService.suspendProcessInstanceById(suspendedProcessInstance.getProcessInstanceId());

    List<ProcessInstance> activeProcessInstances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("oneTaskProcess")
        .active()
        .list();

    List<ProcessInstance> suspendedProcessInstances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("oneTaskProcess")
        .suspended()
        .list();

    // assume
    assertThat(activeProcessInstances).hasSize(2);
    assertThat(suspendedProcessInstances).hasSize(1);

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .active()
          .variableValueEquals("foo", 0)
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(3);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnByProcessDefinitionKeyOrActivityId() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("process")
        .startEvent()
          .userTask("aUserTask")
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("process");

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .activityIdIn("theTask")
          .processDefinitionKey("process")
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void shouldReturnByProcessDefinitionIdOrIncidentType() {
    // given
    String processDefinitionId = runtimeService.startProcessInstanceByKey("oneTaskProcess")
        .getProcessDefinitionId();

    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("process")
        .startEvent().operatonAsyncBefore()
          .userTask("aUserTask")
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    managementService.setJobRetries(jobId, 0);

    // when
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .or()
          .incidentType("failedJob")
          .processDefinitionId(processDefinitionId)
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

}
