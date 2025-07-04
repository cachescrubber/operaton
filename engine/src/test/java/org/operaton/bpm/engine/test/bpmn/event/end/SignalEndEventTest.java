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
package org.operaton.bpm.engine.test.bpmn.event.end;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Kristin Polenz
 */
class SignalEndEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testCatchSignalEndEventInEmbeddedSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignalEndEventInEmbeddedSubprocess");
    assertThat(processInstance).isNotNull();

    // After process start, usertask in subprocess should exist
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("subprocessTask");

    // After task completion, signal end event is reached and caught
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task after catching the signal");

    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/SignalEndEventTest.catchSignalEndEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/end/SignalEndEventTest.processWithSignalEndEvent.bpmn20.xml"
  })
  @Test
  void testCatchSignalEndEventInCallActivity() {
    // first, start process to wait of the signal event
    ProcessInstance processInstanceCatchEvent = runtimeService.startProcessInstanceByKey("catchSignalEndEvent");
    assertThat(processInstanceCatchEvent).isNotNull();

    // now we have a subscription for the signal event:
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createEventSubscriptionQuery().singleResult().getEventName()).isEqualTo("alert");

    // start process which throw the signal end event
    ProcessInstance processInstanceEndEvent = runtimeService.startProcessInstanceByKey("processWithSignalEndEvent");
    assertThat(processInstanceEndEvent).isNotNull();
    testRule.assertProcessEnded(processInstanceEndEvent.getId());

    // user task of process catchSignalEndEvent
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterSignalCatch");

    // complete user task
    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstanceCatchEvent.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/testPropagateOutputVariablesWhileThrowSignal.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEndEventTest.parent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowSignal() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("SignalParentProcess", variables).getId();

    // when
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/testPropagateOutputVariablesWhileThrowSignal2.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEndEventTest.parent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowSignal2() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("SignalParentProcess", variables).getId();

    // when
    String id = taskService.createTaskQuery().taskName("inside subprocess").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  protected void checkOutput(String processInstanceId) {
    assertThat(taskService.createTaskQuery().taskName("task after catched signal").count()).isEqualTo(1);
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "cancelReason")).isNotNull();
    assertThat(runtimeService.getVariable(processInstanceId, "input")).isEqualTo(42);
  }
}
