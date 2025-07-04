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
package org.operaton.bpm.engine.test.api.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.context.DelegateExecutionContext;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;

/**
 * Represents test class to test the delegate execution context.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class DelegateExecutionContextTest {

  protected static final BpmnModelInstance SERVICE_TASK_DELEGATE_PROCESS = Bpmn.createExecutableProcess()
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .serviceTask("serviceTask1")
        .operatonClass(DelegateClass.class.getName())
      .endEvent()
      .done();


  protected static final BpmnModelInstance EXEUCTION_LISTENER_PROCESS = Bpmn.createExecutableProcess()
      .operatonHistoryTimeToLive(180)
      .startEvent()
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ExecutionListenerImpl.class.getName())
      .endEvent()
      .done();

  protected static final BpmnModelInstance SIGNAL_EVENT_PROCESS = Bpmn.createExecutableProcess()
      .operatonHistoryTimeToLive(180)
      .startEvent()
        .intermediateCatchEvent("catchSignal")
          .signal("${delegateExecutionContextBean.getCurrentActivityId()}")
      .endEvent()
      .done();

  protected static final BpmnModelInstance getSingleUserTaskProcessWithSignalStartEventSubprocess() {
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();
    BpmnModelInstance model = processBuilder
        .operatonHistoryTimeToLive(180)
        .startEvent("start")
        .userTask("A")
      .endEvent()
      .done();
    processBuilder.eventSubProcess()
      .startEvent("catchSignal")
        .signal("${delegateExecutionContextBean.getCurrentActivityId()}")
      .userTask("B")
      .endEvent()
    .subProcessDone();
    return model;
  }

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @AfterEach
  void tearDown() {
    Mocks.reset();
  }

  @Test
  void testDelegateExecutionContext() {
    // given
    ProcessDefinition definition = testHelper.deployAndGetDefinition(SERVICE_TASK_DELEGATE_PROCESS);
    // a process instance with a service task and a java delegate
    runtimeService.startProcessInstanceById(definition.getId());

    //then delegation execution context is no more available
    DelegateExecution execution = DelegateExecutionContext.getCurrentDelegationExecution();
    assertThat(execution).isNull();
  }

  @Test
  void testDelegateExecutionContextWithExecutionListener() {
    //given
    ProcessDefinition definition = testHelper.deployAndGetDefinition(EXEUCTION_LISTENER_PROCESS);
    // a process instance with a service task and an execution listener
    runtimeService.startProcessInstanceById(definition.getId());

    //then delegation execution context is no more available
    DelegateExecution execution = DelegateExecutionContext.getCurrentDelegationExecution();
    assertThat(execution).isNull();
  }

  @Test
  void shouldCreateEventSubscription_IntermediateSignalEvent() {
    // given
    // register Bean
    Mocks.register("delegateExecutionContextBean", new DelegateExecutionContextBean());

    ProcessDefinition definition = testHelper.deployAndGetDefinition(SIGNAL_EVENT_PROCESS);

    // when
    // a process instance with a signal event calling delegateExecutionContextBean.getCurrentActivityId() to resolve referenced signal
    runtimeService.startProcessInstanceById(definition.getId());

    // then
    // signal name resolved to the current activity (signal catch event) and event subscription was created
    assertThat(runtimeService.createEventSubscriptionQuery().singleResult().getEventName()).isEqualTo("catchSignal");
  }

  @Test
  void shouldCreateEventSubscription_EventSubprocess() {
    // given
    // register Bean
    Mocks.register("delegateExecutionContextBean", new DelegateExecutionContextBean());

    // a process with a signal start event in an event subprocess
    // the signal start event calls delegateExecutionContextBean.getCurrentActivityId() to resolve the signal name
    ProcessDefinition definition = testHelper.deployAndGetDefinition(getSingleUserTaskProcessWithSignalStartEventSubprocess());

    // when
    // creates scope of event subprocess, including the signal start event
    // expression in the signal start event resolves to the name of the current activity (start event)
    runtimeService.startProcessInstanceById(definition.getId());

    // then
    // signal name resolved to the current activity (start event) and event subscription was created
    assertThat(runtimeService.createEventSubscriptionQuery().singleResult().getEventName()).isEqualTo("start");
  }

  @Deployment
  @Test
  void shouldCreateEventSubscription_ProcessInstanceModification_EventSubprocess() {
    // given
    // register Bean
    Mocks.register("delegateExecutionContextBean", new DelegateExecutionContextBean());

    // a process instance with:
    // 1. a simple process with a user task called 'A'
    // 2. an embedded subprocess containing a user task 'B' and an event subprocess started by a signal start event
    // the signal start event calls delegateExecutionContextBean.getCurrentActivityId() to resolve the signal name
    // expression in the signal event resolves to the name of the current activity (subprocess)
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // when
    // perform an instance migration: cancel token on 'A' and start token inside subprocess at task 'B'
    // this initializes the scope of the event subprocess and should create the event subscription for the start event
    ActivityInstance activityTree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("B")
      .cancelActivityInstance(activityTree.getActivityInstances("A")[0].getId())
      .execute();

    // then
    // signal name resolved to the current activity (subprocess) and event subscription was created
    assertThat(runtimeService.createEventSubscriptionQuery().singleResult().getEventName()).isEqualTo("subprocess");
  }

  @Deployment
  @Test
  void shouldCreateEventSubscription_ProcessIstantiation_EventSubprocess() {
    // given
    // register Bean
    Mocks.register("delegateExecutionContextBean", new DelegateExecutionContextBean());

    // when
    // a process instance with a simple process including user task 'A' and an event subprocess started by a signal start event
    // start process via process instantiation builder
    runtimeService.createProcessInstanceByKey("testProcess").startBeforeActivity("A").execute();

    // then
    // signal name resolved to the current activity (user task 'A') and event subscription was created
    assertThat(runtimeService.createEventSubscriptionQuery().singleResult().getEventName()).isEqualTo("A");

  }

  public static class ExecutionListenerImpl implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      //then delegation execution context is available
      DelegateExecution delegateExecution = DelegateExecutionContext.getCurrentDelegationExecution();
      assertThat(delegateExecution)
              .isNotNull()
              .isEqualTo(execution);
    }
  }

  public static class DelegateClass implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      //then delegation execution context is available
      DelegateExecution delegateExecution = DelegateExecutionContext.getCurrentDelegationExecution();
      assertThat(delegateExecution)
              .isNotNull()
              .isEqualTo(execution);
    }
  }

  public class DelegateExecutionContextBean {

    public String getCurrentActivityId() {
      return DelegateExecutionContext.getCurrentDelegationExecution().getCurrentActivityId();
    }
  }

}
