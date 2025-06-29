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
package org.operaton.bpm.qa.largedata.util;

import org.operaton.bpm.engine.ProcessEngineProvider;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.test.api.runtime.BatchHelper;

public class BatchModificationJobHelper extends BatchHelper {

  public BatchModificationJobHelper(ProcessEngineProvider processEngineProvider) {
    super(processEngineProvider);
  }

  @Override
  public JobDefinition getExecutionJobDefinition(Batch batch) {
    return getManagementService()
        .createJobDefinitionQuery()
        .jobDefinitionId(batch.getBatchJobDefinitionId())
        .jobType(Batch.TYPE_SET_JOB_RETRIES)
        .singleResult();
  }
}
