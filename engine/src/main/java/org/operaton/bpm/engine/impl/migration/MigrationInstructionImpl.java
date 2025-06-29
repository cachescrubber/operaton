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
package org.operaton.bpm.engine.impl.migration;

import org.operaton.bpm.engine.migration.MigrationInstruction;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationInstructionImpl implements MigrationInstruction {

  protected String sourceActivityId;
  protected String targetActivityId;

  protected boolean updateEventTrigger = false;

  public MigrationInstructionImpl(String sourceActivityId, String targetActivityId) {
    this(sourceActivityId, targetActivityId, false);
  }

  public MigrationInstructionImpl(String sourceActivityId, String targetActivityId, boolean updateEventTrigger) {
    this.sourceActivityId = sourceActivityId;
    this.targetActivityId = targetActivityId;
    this.updateEventTrigger = updateEventTrigger;
  }

  @Override
  public String getSourceActivityId() {
    return sourceActivityId;
  }

  @Override
  public String getTargetActivityId() {
    return targetActivityId;
  }

  @Override
  public boolean isUpdateEventTrigger() {
    return updateEventTrigger;
  }

  public void setUpdateEventTrigger(boolean updateEventTrigger) {
    this.updateEventTrigger = updateEventTrigger;
  }

  @Override
  public String toString() {
    return "MigrationInstructionImpl{" +
      "sourceActivityId='" + sourceActivityId + '\'' +
      ", targetActivityId='" + targetActivityId + '\'' +
      ", updateEventTrigger='" + updateEventTrigger + '\'' +
      '}';
  }

}
