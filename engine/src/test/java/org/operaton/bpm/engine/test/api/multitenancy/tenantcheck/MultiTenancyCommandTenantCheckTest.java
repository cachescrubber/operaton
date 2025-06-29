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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

class MultiTenancyCommandTenantCheckTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;

  @BeforeEach
  void init() {
    identityService.setAuthentication("user", null, null);
  }

  @Test
  void disableTenantCheckForProcessEngine() {
    // disable tenant check for process engine
    processEngineConfiguration.setTenantCheckEnabled(false);

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      // cannot enable tenant check for command when it is disabled for process engine
      commandContext.enableTenantCheck();
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isFalse();

      return null;
    });
  }

  @Test
  void disableTenantCheckForCommand() {

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      // disable tenant check for the current command
      commandContext.disableTenantCheck();
      assertThat(commandContext.isTenantCheckEnabled()).isFalse();
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isFalse();

      return null;
    });

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      // assert that it is enabled again for further commands
      assertThat(commandContext.isTenantCheckEnabled()).isTrue();
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isTrue();

      return null;
    });
  }

  @Test
  void disableAndEnableTenantCheckForCommand() {

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      commandContext.disableTenantCheck();
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isFalse();

      commandContext.enableTenantCheck();
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isTrue();

      return null;
    });
  }

  @Test
  void disableTenantCheckForOperatonAdmin() {
    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      // operaton-admin should access data from all tenants
      assertThat(commandContext.getTenantManager().isTenantCheckEnabled()).isFalse();

      return null;
    });
  }

}
