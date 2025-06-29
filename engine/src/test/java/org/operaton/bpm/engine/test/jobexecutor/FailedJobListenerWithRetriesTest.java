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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.DefaultJobRetryCmd;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

@Parameterized
public class FailedJobListenerWithRetriesTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @Parameter(0)
  public int failedRetriesNumber;

  @Parameter(1)
  public int jobRetries;

  @Parameter(2)
  public boolean jobLocked;

  @BeforeEach
  void init() {
    processEngineConfiguration.setFailedJobCommandFactory(new OLEFailedJobCommandFactory());
    processEngineConfiguration.setFailedJobListenerMaxRetries(5);
  }

  @Parameters
  public static Collection<Object[]> scenarios() {
    return Arrays.asList(new Object[][] {
        { 4, 0, false },
        //all retries are depleted without success -> the job is still locked
        { 5, 1, true }
    });
  }

  @TestTemplate
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  void testFailedJobListenerRetries() {
    //given
    runtimeService.startProcessInstanceByKey("failingProcess");

    //when the job is run several times till the incident creation
    Job job = getJob();
    while (job.getRetries() > 0 && ((JobEntity)job).getLockOwner() == null ) {
      try {
        lockTheJob(job.getId());
        engineRule.getManagementService().executeJob(job.getId());
      } catch (Exception ex) {
      }
      job = getJob();
    }

    //then
    JobEntity jobFinalState = (JobEntity)engineRule.getManagementService().createJobQuery().jobId(job.getId()).list().get(0);
    assertThat(jobFinalState.getRetries()).isEqualTo(jobRetries);
    if (jobLocked) {
      assertThat(jobFinalState.getLockOwner()).isNotNull();
      assertThat(jobFinalState.getLockExpirationTime()).isNotNull();
    } else {
      assertThat(jobFinalState.getLockOwner()).isNull();
      assertThat(jobFinalState.getLockExpirationTime()).isNull();
    }
  }

  void lockTheJob(final String jobId) {
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequiresNew().execute(commandContext -> {
      final JobEntity job = commandContext.getJobManager().findJobById(jobId);
      job.setLockOwner("someLockOwner");
      job.setLockExpirationTime(DateUtils.addHours(ClockUtil.getCurrentTime(), 1));
      return null;
    });
  }

  private Job getJob() {
    List<Job> jobs = engineRule.getManagementService().createJobQuery().list();
    assertThat(jobs).hasSize(1);
    return jobs.get(0);
  }

  public class OLEFailedJobCommandFactory extends DefaultFailedJobCommandFactory {

    private Map<String, OLEFoxJobRetryCmd> oleFoxJobRetryCmds = new HashMap<>();

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
      return getOleFoxJobRetryCmds(jobId, exception);
    }

    public OLEFoxJobRetryCmd getOleFoxJobRetryCmds(String jobId, Throwable exception) {
      if (!oleFoxJobRetryCmds.containsKey(jobId)) {
        oleFoxJobRetryCmds.put(jobId, new OLEFoxJobRetryCmd(jobId, exception));
      }
      return oleFoxJobRetryCmds.get(jobId);
    }
  }

  public class OLEFoxJobRetryCmd extends DefaultJobRetryCmd {

    private int countRuns = 0;

    public OLEFoxJobRetryCmd(String jobId, Throwable exception) {
      super(jobId, exception);
    }

    @Override
    public Object execute(CommandContext commandContext) {
      Job job = getJob();
      //on last attempt the incident will be created, we imitate OLE
      if (job.getRetries() == 1) {
        countRuns++;
        if (countRuns <= failedRetriesNumber) {
          super.execute(commandContext);
          throw new OptimisticLockingException("OLE");
        }
      }
      return super.execute(commandContext);
    }
  }
}
