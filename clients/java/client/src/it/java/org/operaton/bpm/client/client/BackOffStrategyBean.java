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
package org.operaton.bpm.client.client;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.task.ExternalTask;

import java.util.List;

/**
 * @author Tassilo Weidner
 */
public class BackOffStrategyBean implements BackoffStrategy {

  @Override
  public void reconfigure(List<ExternalTask> externalTasks) {
  }

  @Override
  public long calculateBackoffTime() {
    return 2000L;
  }
}
