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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;

@Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/JobExecutorAcquireJobsForPriorityRangeTest.jobPrioProcess.bpmn20.xml")
class JobExecutorAcquireJobsForPriorityRangeTest extends AbstractJobExecutorAcquireJobsTest {

  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension();

  @BeforeEach
  void setUp() {
    configuration.setJobExecutorAcquireByPriority(true);
    // create 10 jobs, 5 with prio 5 and 5 with prio 10
    createJobs();
  }

  @Test
  void shouldAcquireAllJobs() {
    // given
    configuration.setJobExecutorPriorityRangeMin(0);
    configuration.setJobExecutorPriorityRangeMax(Long.MAX_VALUE);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).hasSize(10);
    for (int i = 0; i < 5; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(10);
    }
    for (int i = 5; i < 10; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(5);
    }
  }

  @Test
  void shouldAcquireOnlyJobsInRangeWithUpperBound() {
    // given
    configuration.setJobExecutorPriorityRangeMin(0);
    configuration.setJobExecutorPriorityRangeMax(7L);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).hasSize(5);
    for (int i = 0; i < 5; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(5);
    }
  }

  @Test
  void shouldAcquireOnlyJobsInRangeWithLowerBound() {
    // given
    configuration.setJobExecutorPriorityRangeMin(7L);
    configuration.setJobExecutorPriorityRangeMax(Long.MAX_VALUE);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).hasSize(5);
    for (int i = 0; i < 5; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(10);
    }
  }

  @Test
  void shouldAcquireOnlyJobsInBoundWithUpperAndLowerBound() {
    // given
    configuration.setJobExecutorPriorityRangeMin(7L);
    configuration.setJobExecutorPriorityRangeMax(12L);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).hasSize(5);
    for (int i = 0; i < 5; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(10);
    }
  }

  @Test
  void shouldAcquireOnlyJobsInBoundWithUpperAndLowerBoundNoJobsFound() {
    // given
    configuration.setJobExecutorPriorityRangeMin(12L);
    configuration.setJobExecutorPriorityRangeMax(15L);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).isEmpty();
  }

  @Test
  void shouldAcquireJobsWithNegativePriorityInRange() {
    // given
    configuration.setJobExecutorPriorityRangeMin(-5);
    startProcess("jobPrioProcess", "task3", 1);

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    Job jobWithNegativePrio = managementService.createJobQuery().priorityLowerThanOrEquals(-5).singleResult();
    assertThat(acquirableJobs).extracting("id").contains(jobWithNegativePrio.getId());
  }

  @Test
  void shouldSetDefaultPriorityRange() {
    // given standard configuration

    // when

    // then
    // no configuration exception
    assertThat(configuration.getJobExecutorPriorityRangeMin()).isEqualTo(Long.MIN_VALUE);
    assertThat(configuration.getJobExecutorPriorityRangeMax()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void shouldAcquireAllJobsWhenDefaultPriorityRange() {
    // given default configuration

    // when
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then
    assertThat(acquirableJobs).hasSize(10);
  }

  @Test
  @WatchLogger(loggerNames = "org.operaton.bpm.engine.impl.persistence.entity.JobEntity", level = "debug")
  void shouldDisableRangeCheckInQueryWhenDefaultConfig() {
    // given default configuration
    configuration.setJobExecutorAcquireByPriority(false);

    // when
    findAcquirableJobs();

    // then
    List<ILoggingEvent> log = loggingRule.getFilteredLog("RES.PRIORITY_ >= ? and RES.PRIORITY_ <= ?");
    assertThat(log).isEmpty();
  }

  @Test
  @WatchLogger(loggerNames = "org.operaton.bpm.engine.impl.persistence.entity.JobEntity", level = "debug")
  void shouldEnableRangeCheckInQueryWhenUsingCustomMinBoundaryConfig() {
    // given default configuration
    configuration.setJobExecutorPriorityRangeMin(6);

    // when
    findAcquirableJobs();

    // then
    List<ILoggingEvent> logRangeMinCheck = loggingRule.getFilteredLog("RES.PRIORITY_ >= ?");
    assertThat(logRangeMinCheck).hasSize(1);
    List<ILoggingEvent> logRangeMaxCheck = loggingRule.getFilteredLog("RES.PRIORITY_ <= ?");
    assertThat(logRangeMaxCheck).isEmpty();
  }

  @Test
  @WatchLogger(loggerNames = "org.operaton.bpm.engine.impl.persistence.entity.JobEntity", level = "debug")
  void shouldEnableRangeCheckInQueryWhenUsingCustomMaxBoundaryConfig() {
    // given default configuration
    configuration.setJobExecutorPriorityRangeMax(11);

    // when
    findAcquirableJobs();

    // then
    List<ILoggingEvent> logRangeMinCheck = loggingRule.getFilteredLog("RES.PRIORITY_ >= ?");
    assertThat(logRangeMinCheck).isEmpty();
    List<ILoggingEvent> logRangeMaxCheck = loggingRule.getFilteredLog("RES.PRIORITY_ <= ?");
    assertThat(logRangeMaxCheck).hasSize(1);
  }

  private void createJobs() {
    // jobs with priority 10
    startProcess("jobPrioProcess", "task1", 5);

    // jobs with priority 5
    startProcess("jobPrioProcess", "task2", 5);
  }
}
