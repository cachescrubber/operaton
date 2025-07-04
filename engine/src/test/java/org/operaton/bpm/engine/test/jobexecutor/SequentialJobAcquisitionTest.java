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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.util.JobExecutorWaitUtils.waitForJobExecutionRunnablesToFinish;
import static org.operaton.bpm.engine.test.util.JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs;

import java.text.DateFormat.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.util.ClockUtil;

/**
 * @author Daniel Meyer
 */
class SequentialJobAcquisitionTest {

  private static final String RESOURCE_BASE = SequentialJobAcquisitionTest.class.getPackage()
      .getName()
      .replace(".", "/");

  private static final String PROCESS_RESOURCE =
      RESOURCE_BASE + "/IntermediateTimerEventTest.testCatchingTimerEvent.bpmn20.xml";

  private JobExecutor jobExecutor = new DefaultJobExecutor();
  private final List<ProcessEngine> createdProcessEngines = new ArrayList<>();

  @BeforeEach
  void startJobExecutor() {
    jobExecutor = new DefaultJobExecutor();
  }
  
  @AfterEach
  void stopJobExecutor() {
    jobExecutor.shutdown();
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void closeProcessEngines() {
    Iterator<ProcessEngine> iterator = createdProcessEngines.iterator();
    while (iterator.hasNext()) {
      ProcessEngine processEngine = iterator.next();
      processEngine.close();
      ProcessEngines.unregister(processEngine);
      iterator.remove();
    }
  }

  @Test
  void testExecuteJobsForSingleEngine() {
    // configure and build a process engine
    StandaloneProcessEngineConfiguration standaloneProcessEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
    standaloneProcessEngineConfiguration.setProcessEngineName(getClass().getName() + "-engine1");
    standaloneProcessEngineConfiguration.setJdbcUrl("jdbc:h2:mem:jobexecutor-test-engine");
    standaloneProcessEngineConfiguration.setJobExecutorActivate(false);
    standaloneProcessEngineConfiguration.setJobExecutor(jobExecutor);
    standaloneProcessEngineConfiguration.setDbMetricsReporterActivate(false);

    ProcessEngine engine = standaloneProcessEngineConfiguration.buildProcessEngine();

    createdProcessEngines.add(engine);

    engine.getRepositoryService().createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

    jobExecutor.shutdown();

    engine.getRuntimeService().startProcessInstanceByKey("intermediateTimerEventExample");

    assertThat(engine.getManagementService().createJobQuery().count()).isEqualTo(1);

    Calendar calendar = Calendar.getInstance();
    calendar.add(Field.DAY_OF_YEAR.getCalendarField(), 6);
    ClockUtil.setCurrentTime(calendar.getTime());
    jobExecutor.start();
    waitForJobExecutorToProcessAllJobs(10000, 100, jobExecutor, engine.getManagementService());

    assertThat(engine.getManagementService().createJobQuery().count()).isZero();
  }

  @Test
  void testExecuteJobsForTwoEnginesSameAcquisition() {
    // configure and build a process engine
    StandaloneProcessEngineConfiguration engineConfiguration1 = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration1.setProcessEngineName(getClass().getName() + "-engine1");
    engineConfiguration1.setJdbcUrl("jdbc:h2:mem:activiti1");
    engineConfiguration1.setJobExecutorActivate(false);
    engineConfiguration1.setJobExecutor(jobExecutor);
    engineConfiguration1.setDbMetricsReporterActivate(false);

    ProcessEngine engine1 = engineConfiguration1.buildProcessEngine();
    createdProcessEngines.add(engine1);

    // and a second one
    StandaloneProcessEngineConfiguration engineConfiguration2 = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration2.setProcessEngineName(getClass().getName() + "engine2");
    engineConfiguration2.setJdbcUrl("jdbc:h2:mem:activiti2");
    engineConfiguration2.setJobExecutorActivate(false);
    engineConfiguration2.setJobExecutor(jobExecutor);
    engineConfiguration2.setDbMetricsReporterActivate(false);
    engineConfiguration2.setEnforceHistoryTimeToLive(false);

    ProcessEngine engine2 = engineConfiguration2.buildProcessEngine();

    createdProcessEngines.add(engine2);

    // stop the acquisition
    jobExecutor.shutdown();

    // deploy the processes

    engine1.getRepositoryService().createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

    engine2.getRepositoryService().createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

    // start one instance for each engine:

    engine1.getRuntimeService().startProcessInstanceByKey("intermediateTimerEventExample");
    engine2.getRuntimeService().startProcessInstanceByKey("intermediateTimerEventExample");

    assertThat(engine1.getManagementService().createJobQuery().count()).isEqualTo(1);
    assertThat(engine2.getManagementService().createJobQuery().count()).isEqualTo(1);

    Calendar calendar = Calendar.getInstance();
    calendar.add(Field.DAY_OF_YEAR.getCalendarField(), 6);
    ClockUtil.setCurrentTime(calendar.getTime());

    jobExecutor.start();
    // assert task completed for the first engine
    waitForJobExecutorToProcessAllJobs(10000, 100, jobExecutor, engine1.getManagementService());

    jobExecutor.start();
    // assert task completed for the second engine
    waitForJobExecutorToProcessAllJobs(10000, 100, jobExecutor, engine2.getManagementService());

    assertThat(engine1.getManagementService().createJobQuery().count()).isZero();
    assertThat(engine2.getManagementService().createJobQuery().count()).isZero();
  }

  @Test
  void testJobAddedGuardForTwoEnginesSameAcquisition() throws InterruptedException {
    // configure and build a process engine
    StandaloneProcessEngineConfiguration engineConfiguration1 = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration1.setProcessEngineName(getClass().getName() + "-engine1");
    engineConfiguration1.setJdbcUrl("jdbc:h2:mem:activiti1");
    engineConfiguration1.setJobExecutorActivate(false);
    engineConfiguration1.setJobExecutor(jobExecutor);
    engineConfiguration1.setDbMetricsReporterActivate(false);

    ProcessEngine engine1 = engineConfiguration1.buildProcessEngine();
    createdProcessEngines.add(engine1);

    // and a second one
    StandaloneProcessEngineConfiguration engineConfiguration2 = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration2.setProcessEngineName(getClass().getName() + "engine2");
    engineConfiguration2.setJdbcUrl("jdbc:h2:mem:activiti2");
    engineConfiguration2.setJobExecutorActivate(false);
    engineConfiguration2.setJobExecutor(jobExecutor);
    engineConfiguration2.setDbMetricsReporterActivate(false);

    ProcessEngine engine2 = engineConfiguration2.buildProcessEngine();
    createdProcessEngines.add(engine2);

    // stop the acquisition
    jobExecutor.shutdown();

    // deploy the processes

    engine1.getRepositoryService().createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

    engine2.getRepositoryService().createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

    // start one instance for each engine:

    engine1.getRuntimeService().startProcessInstanceByKey("intermediateTimerEventExample");
    engine2.getRuntimeService().startProcessInstanceByKey("intermediateTimerEventExample");

    Calendar calendar = Calendar.getInstance();
    calendar.add(Field.DAY_OF_YEAR.getCalendarField(), 6);
    ClockUtil.setCurrentTime(calendar.getTime());

    assertThat(engine1.getManagementService().createJobQuery().count()).isEqualTo(1);
    assertThat(engine2.getManagementService().createJobQuery().count()).isEqualTo(1);

    // assert task completed for the first engine
    jobExecutor.start();
    waitForJobExecutorToProcessAllJobs(10000, 100, jobExecutor, engine1.getManagementService());

    // assert task completed for the second engine
    jobExecutor.start();

    waitForJobExecutorToProcessAllJobs(10000, 100, jobExecutor, engine2.getManagementService());
    waitForJobExecutionRunnablesToFinish(10000, 100, jobExecutor);

    Thread.sleep(2000);

    assertThat(jobExecutor.getAcquireJobsRunnable().isJobAdded()).isFalse();

    assertThat(engine1.getManagementService().createJobQuery().count()).isZero();
    assertThat(engine2.getManagementService().createJobQuery().count()).isZero();
  }

}
