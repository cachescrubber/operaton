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
package org.operaton.bpm.engine.impl.migration.validation.instance;

import org.operaton.bpm.engine.impl.migration.instance.MigratingCompensationEventSubscriptionInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingEventScopeInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public interface MigratingCompensationInstanceValidator {

  /**
   * @param migratingInstance
   * @param migratingProcessInstance
   * @param ancestorInstanceReport the report of the closest ancestor activity instance;
   *   errors should be added to this report
   */
  void validate(MigratingEventScopeInstance migratingInstance,
      MigratingProcessInstance migratingProcessInstance,
      MigratingActivityInstanceValidationReportImpl ancestorInstanceReport);

  /**
   * @param migratingInstance
   * @param migratingProcessInstance
   * @param ancestorInstanceReport the report of the closest ancestor activity instance;
   *   errors should be added to this report
   */
  void validate(MigratingCompensationEventSubscriptionInstance migratingInstance,
      MigratingProcessInstance migratingProcessInstance,
      MigratingActivityInstanceValidationReportImpl ancestorInstanceReport);


}
