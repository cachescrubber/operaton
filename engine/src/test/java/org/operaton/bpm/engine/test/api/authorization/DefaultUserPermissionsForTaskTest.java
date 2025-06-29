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
package org.operaton.bpm.engine.test.api.authorization;

import static org.operaton.bpm.engine.authorization.Permissions.TASK_WORK;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class DefaultUserPermissionsForTaskTest extends AuthorizationTest {

  protected String userId2 = "demo";
  protected User user2;

  protected String groupId2 = "accounting2";
  protected Group group2;

  protected String defaultTaskPermissionValue;

  @AfterEach
  @Override
  public void tearDown() {
    // reset default permission
    processEngineConfiguration.setDefaultUserPermissionForTask(UPDATE);
    super.tearDown();
  }

  @Test
  void testShouldGrantTaskWorkOnAssign() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(TASK_WORK);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().setAssignee(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.TASK_WORK, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isFalse();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantUpdateOnAssign() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(UPDATE);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().setAssignee(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantTaskWorkOnSetCandidateUser() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(TASK_WORK);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().addCandidateUser(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.TASK_WORK, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isFalse();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantUpdateOnSetCandidateUser() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(UPDATE);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().addCandidateUser(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isTrue();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantTaskWorkOnSetOwner() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(TASK_WORK);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().setOwner(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.TASK_WORK, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isFalse();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantUpdateOnSetOwner() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(UPDATE);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().setOwner(taskId, userId2);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, null, Permissions.UPDATE, Resources.TASK, taskId)).isTrue();

    deleteTask(taskId, true);
  }


  @Test
  void testShouldGrantTaskWorkOnSetCandidateGroup() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(TASK_WORK);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().addCandidateGroup(taskId, groupId);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, List.of(groupId), Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, List.of(groupId), Permissions.TASK_WORK, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, List.of(groupId), Permissions.UPDATE, Resources.TASK, taskId)).isFalse();

    deleteTask(taskId, true);
  }

  @Test
  void testShouldGrantUpdateOnSetCandidateGroup() {

    // given
    processEngineConfiguration.setDefaultUserPermissionForTask(UPDATE);

    String taskId = "myTask";
    createTask(taskId);
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    processEngine.getTaskService().addCandidateGroup(taskId, groupId);

    // then
    assertThat(authorizationService.isUserAuthorized(userId2, List.of(groupId), Permissions.READ, Resources.TASK, taskId)).isTrue();
    assertThat(authorizationService.isUserAuthorized(userId2, List.of(groupId), Permissions.UPDATE, Resources.TASK, taskId)).isTrue();

    deleteTask(taskId, true);
  }

}
