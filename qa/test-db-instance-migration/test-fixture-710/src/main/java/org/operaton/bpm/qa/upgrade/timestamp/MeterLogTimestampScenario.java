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
package org.operaton.bpm.qa.upgrade.timestamp;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.MeterLogEntity;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author Nikola Koevski
 */
public class MeterLogTimestampScenario extends AbstractTimestampMigrationScenario {

  protected static final String REPORTER_NAME = "MeterLogTimestampTest";

  @DescribesScenario("initMeterLogTimestamp")
  @Times(1)
  public static ScenarioSetup initMeterLogTimestamp() {
    return new ScenarioSetup() {
      @Override
      public void execute(ProcessEngine processEngine, String s) {

      ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())
        .getCommandExecutorTxRequired()
        .execute(new Command<Void>() {
          @Override
          public Void execute(CommandContext commandContext) {

            commandContext.getMeterLogManager()
              .insert(new MeterLogEntity(REPORTER_NAME, REPORTER_NAME, 1L, TIMESTAMP));

            return null;
          }
        });
      }
    };
  }
}
