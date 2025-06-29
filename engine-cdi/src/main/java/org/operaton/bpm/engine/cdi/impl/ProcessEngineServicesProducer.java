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
package org.operaton.bpm.engine.cdi.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;

import java.util.List;

/**
 * Makes the managed process engine and the provided services available for injection
 *
 * @author Daniel Meyer
 * @author Falko Menge
 */
public class ProcessEngineServicesProducer {

  @Produces
  @Named
  @ApplicationScoped
  public ProcessEngine processEngine() {

    ProcessEngine processEngine =  BpmPlatform.getProcessEngineService().getDefaultProcessEngine();
    if(processEngine != null) {
      return processEngine;
    } else {
      List<ProcessEngine> processEngines = BpmPlatform.getProcessEngineService().getProcessEngines();
      if (processEngines != null && processEngines.size() == 1) {
        return processEngines.get(0);
      } else {
        return ProcessEngines.getDefaultProcessEngine(false);
      }
    }

  }

  @Produces @Named @ApplicationScoped public RuntimeService runtimeService() { return processEngine().getRuntimeService(); }

  @Produces @Named @ApplicationScoped public TaskService taskService() { return processEngine().getTaskService(); }

  @Produces @Named @ApplicationScoped public RepositoryService repositoryService() { return processEngine().getRepositoryService(); }

  @Produces @Named @ApplicationScoped public FormService formService() { return processEngine().getFormService(); }

  @Produces @Named @ApplicationScoped public HistoryService historyService() { return processEngine().getHistoryService(); }

  @Produces @Named @ApplicationScoped public IdentityService identityService() { return processEngine().getIdentityService(); }

  @Produces @Named @ApplicationScoped public ManagementService managementService() { return processEngine().getManagementService(); }

  @Produces @Named @ApplicationScoped public AuthorizationService authorizationService() { return processEngine().getAuthorizationService(); }

  @Produces @Named @ApplicationScoped public FilterService filterService() { return processEngine().getFilterService(); }

  @Produces @Named @ApplicationScoped public ExternalTaskService externalTaskService() { return processEngine().getExternalTaskService(); }

  @Produces @Named @ApplicationScoped public CaseService caseService() { return processEngine().getCaseService(); }

  @Produces @Named @ApplicationScoped public DecisionService decisionService() { return processEngine().getDecisionService(); }

}
