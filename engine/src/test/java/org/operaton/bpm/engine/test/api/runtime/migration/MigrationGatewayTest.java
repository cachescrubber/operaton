/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;

import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.GatewayModels;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationGatewayTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected MigrationTestRule testHelper = new MigrationTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testHelper);

  @Test
  public void testParallelGatewayContinueExecution() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(1);
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    testHelper.completeTask("parallel1");
    testHelper.completeTask("afterJoin");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testParallelGatewayAssertTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("parallel1").noScope().concurrent().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("parallel1")).up()
          .child("join").noScope().concurrent().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("join"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("parallel1", testHelper.getSingleActivityInstanceBeforeMigration("parallel1").getId())
          .activity("join", testHelper.getSingleActivityInstanceBeforeMigration("join").getId())
        .done());
  }

  @Test
  public void testParallelGatewayAddScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW_IN_SUBPROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(1);
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    testHelper.completeTask("parallel1");
    testHelper.completeTask("afterJoin");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testInclusiveGatewayContinueExecution() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(1);
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    testHelper.completeTask("parallel1");
    testHelper.completeTask("afterJoin");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testInclusiveGatewayAssertTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("parallel1").noScope().concurrent().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("parallel1")).up()
          .child("join").noScope().concurrent().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("join"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("parallel1", testHelper.getSingleActivityInstanceBeforeMigration("parallel1").getId())
          .activity("join", testHelper.getSingleActivityInstanceBeforeMigration("join").getId())
        .done());
  }

  @Test
  public void testInclusiveGatewayAddScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW_IN_SUBPROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("parallel1", "parallel1")
      .mapActivities("join", "join")
      .build();

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(1);
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    testHelper.completeTask("parallel1");
    testHelper.completeTask("afterJoin");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testCannotMigrateParallelToInclusiveGateway() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("join", "join");

    try {
      runtimeService.build();
      fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("join",
        "Activities have incompatible types "
        + "(ParallelGatewayActivityBehavior is not compatible with InclusiveGatewayActivityBehavior)"
      );
    }
  }

  @Test
  public void testCannotMigrateInclusiveToParallelGateway() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.INCLUSIVE_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("join", "join");

    try {
      runtimeService.build();
      fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("join",
        "Activities have incompatible types "
        + "(InclusiveGatewayActivityBehavior is not compatible with ParallelGatewayActivityBehavior)"
      );
    }
  }

  /**
   * Ensures that situations are avoided in which more tokens end up at the target gateway
   * than it has incoming flows
   */
  @Test
  public void testCannotRemoveGatewayIncomingSequenceFlow() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(GatewayModels.PARALLEL_GW)
        .removeFlowNode("parallel2"));
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("join", "join");

    try {
      runtimeService.build();
      fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("join",
        "The target gateway must have at least the same number of incoming sequence flows that the source gateway has"
      );
    }
  }

  /**
   * Ensures that situations are avoided in which more tokens end up at the target gateway
   * than it has incoming flows
   */
  @Test
  public void testAddGatewayIncomingSequenceFlow() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(GatewayModels.PARALLEL_GW)
        .flowNodeBuilder("fork")
        .userTask("parallel3")
        .connectTo("join")
        .done());

    ProcessInstance processInstance = rule
        .getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("parallel2");

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("parallel1", "parallel1")
        .mapActivities("join", "join")
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(1);
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    rule.getRuntimeService().createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("join")
      .execute();
    assertThat(rule.getTaskService().createTaskQuery().taskDefinitionKey("afterJoin").count()).isZero();

    testHelper.completeTask("parallel1");
    testHelper.completeTask("afterJoin");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  /**
   * Ensures that situations are avoided in which more tokens end up at the target gateway
   * than it has incoming flows
   */
  @Test
  public void testCannotRemoveParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW_IN_SUBPROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("join", "join");

    try {
      runtimeService.build();
      fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("join",
        "The gateway's flow scope 'subProcess' must be mapped"
      );
    }
  }

  /**
   * Ensures that situations are avoided in which more tokens end up at the target gateway
   * than it has incoming flows
   */
  @Test
  public void testCannotMapMultipleGatewaysToOne() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(GatewayModels.PARALLEL_GW);
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("join", "join")
        .mapActivities("fork", "join");

    try {
      runtimeService.build();
      fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("join",
        "Only one gateway can be mapped to gateway 'join'"
       );
    }
  }
}

