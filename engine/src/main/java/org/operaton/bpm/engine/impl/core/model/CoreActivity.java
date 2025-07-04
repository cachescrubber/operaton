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
package org.operaton.bpm.engine.impl.core.model;

import java.util.List;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.impl.core.delegate.CoreActivityBehavior;
import org.operaton.bpm.engine.impl.core.variable.mapping.IoMapping;

/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 * @author Sebastian Menski
 *
 */
public abstract class CoreActivity extends CoreModelElement {

  private static final long serialVersionUID = 1L;

  protected IoMapping ioMapping;

  protected CoreActivity(String id) {
    super(id);
  }

  /** searches for the activity recursively */
  public CoreActivity findActivity(String activityId) {
    CoreActivity localActivity = getChildActivity(activityId);
    if (localActivity!=null) {
      return localActivity;
    }
    for (CoreActivity activity: getActivities()) {
      CoreActivity nestedActivity = activity.findActivity(activityId);
      if (nestedActivity!=null) {
        return nestedActivity;
      }
    }
    return null;
  }

  public CoreActivity createActivity() {
    return createActivity(null);
  }

  /** searches for the activity locally */
  public abstract CoreActivity getChildActivity(String activityId);

  public abstract CoreActivity createActivity(String activityId);

  public abstract List<? extends CoreActivity> getActivities();

  public abstract CoreActivityBehavior<? extends BaseDelegateExecution> getActivityBehavior();

  public IoMapping getIoMapping() {
    return ioMapping;
  }

  public void setIoMapping(IoMapping ioMapping) {
    this.ioMapping = ioMapping;
  }

  @Override
  public String toString() {
    return "Activity("+id+")";
  }

}
