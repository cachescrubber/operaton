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
package org.operaton.bpm.engine.rest;


import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.dmn.DecisionsEvaluationBuilder;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.MockDecisionResultBuilder;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.sub.repository.impl.ProcessDefinitionResourceImpl;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class DecisionDefinitionRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String DECISION_DEFINITION_URL = TEST_RESOURCE_ROOT_PATH + "/decision-definition";
  protected static final String SINGLE_DECISION_DEFINITION_URL = DECISION_DEFINITION_URL + "/{id}";
  protected static final String SINGLE_DECISION_DEFINITION_BY_KEY_URL = DECISION_DEFINITION_URL + "/key/{key}";
  protected static final String SINGLE_DECISION_DEFINITION_BY_KEY_AND_TENANT_ID_URL = DECISION_DEFINITION_URL + "/key/{key}/tenant-id/{tenant-id}";

  protected static final String XML_DEFINITION_URL = SINGLE_DECISION_DEFINITION_URL + "/xml";
  protected static final String XML_DEFINITION_BY_KEY_URL = SINGLE_DECISION_DEFINITION_BY_KEY_URL + "/xml";

  protected static final String DIAGRAM_DEFINITION_URL = SINGLE_DECISION_DEFINITION_URL + "/diagram";

  protected static final String EVALUATE_DECISION_URL = SINGLE_DECISION_DEFINITION_URL + "/evaluate";
  protected static final String EVALUATE_DECISION_BY_KEY_URL = SINGLE_DECISION_DEFINITION_BY_KEY_URL + "/evaluate";
  protected static final String EVALUATE_DECISION_BY_KEY_AND_TENANT_ID_URL = SINGLE_DECISION_DEFINITION_BY_KEY_AND_TENANT_ID_URL + "/evaluate";
  protected static final String UPDATE_HISTORY_TIME_TO_LIVE_URL = SINGLE_DECISION_DEFINITION_URL + "/history-time-to-live";

  private RepositoryService repositoryServiceMock;
  private DecisionDefinitionQuery decisionDefinitionQueryMock;
  private DecisionsEvaluationBuilder decisionEvaluationBuilderMock;

  @BeforeEach
  void setUpRuntime() {
    DecisionDefinition mockDecisionDefinition = MockProvider.createMockDecisionDefinition();

    setUpRuntimeData(mockDecisionDefinition);
    setUpDecisionService();
  }

  private void setUpRuntimeData(DecisionDefinition mockDecisionDefinition) {
    repositoryServiceMock = mock(RepositoryService.class);

    when(processEngine.getRepositoryService()).thenReturn(repositoryServiceMock);
    when(repositoryServiceMock.getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID)).thenReturn(mockDecisionDefinition);
    when(repositoryServiceMock.getDecisionModel(MockProvider.EXAMPLE_DECISION_DEFINITION_ID)).thenReturn(createMockDecisionDefinitionDmnXml());

    decisionDefinitionQueryMock = mock(DecisionDefinitionQuery.class);
    when(decisionDefinitionQueryMock.decisionDefinitionKey(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.tenantIdIn(anyString())).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.withoutTenantId()).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.latestVersion()).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.singleResult()).thenReturn(mockDecisionDefinition);
    when(decisionDefinitionQueryMock.list()).thenReturn(Collections.singletonList(mockDecisionDefinition));
    when(repositoryServiceMock.createDecisionDefinitionQuery()).thenReturn(decisionDefinitionQueryMock);
  }

  private InputStream createMockDecisionDefinitionDmnXml() {
    // do not close the input stream, will be done in implementation
    InputStream dmnXmlInputStream = ReflectUtil.getResourceAsStream("decisions/decision-model.dmn");
    assertThat(dmnXmlInputStream).isNotNull();
    return dmnXmlInputStream;
  }

  private void setUpDecisionService() {
    decisionEvaluationBuilderMock = mock(DecisionsEvaluationBuilder.class);
    when(decisionEvaluationBuilderMock.variables(anyMap())).thenReturn(decisionEvaluationBuilderMock);

    DecisionService decisionServiceMock = mock(DecisionService.class);
    when(decisionServiceMock.evaluateDecisionById(MockProvider.EXAMPLE_DECISION_DEFINITION_ID)).thenReturn(decisionEvaluationBuilderMock);
    when(decisionServiceMock.evaluateDecisionByKey(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)).thenReturn(decisionEvaluationBuilderMock);

    when(processEngine.getDecisionService()).thenReturn(decisionServiceMock);
  }

  @Test
  void testDecisionDefinitionDmnXmlRetrieval() {
    Response response = given()
        .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(XML_DEFINITION_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains("<?xml")
      .contains(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testDefinitionRetrieval() {
    given()
      .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_RESOURCE_NAME))
        .body("decisionRequirementsDefinitionId", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("decisionRequirementsDefinitionKey", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_DECISION_DEFINITION_URL);

    verify(repositoryServiceMock).getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testDecisionDefinitionDmnXmlRetrieval_ByKey() {
    Response response = given()
        .pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
      .when()
        .get(XML_DEFINITION_BY_KEY_URL);

    String responseContent = response.asString();
    assertThat(responseContent)
      .contains("<?xml")
      .contains(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testDefinitionRetrieval_ByKey() {
    given()
      .pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_RESOURCE_NAME))
        .body("decisionRequirementsDefinitionId", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("decisionRequirementsDefinitionKey", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("tenantId", equalTo(null))
    .when()
      .get(SINGLE_DECISION_DEFINITION_BY_KEY_URL);

    verify(decisionDefinitionQueryMock).withoutTenantId();
    verify(repositoryServiceMock).getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testNonExistingDecisionDefinitionRetrieval_ByKey() {
    String nonExistingKey = "aNonExistingDefinitionKey";

    when(repositoryServiceMock.createDecisionDefinitionQuery().decisionDefinitionKey(nonExistingKey)).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.latestVersion()).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.singleResult()).thenReturn(null);
    when(decisionDefinitionQueryMock.list()).thenReturn(Collections.emptyList());

    given()
      .pathParam("key", nonExistingKey)
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString("No matching decision definition with key: " + nonExistingKey))
    .when()
      .get(SINGLE_DECISION_DEFINITION_BY_KEY_URL);
  }

  @Test
  void testDefinitionRetrieval_ByKeyAndTenantId() {
    DecisionDefinition mockDefinition = MockProvider.mockDecisionDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeData(mockDefinition);

    given()
      .pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .body("id", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_ID))
        .body("key", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_KEY))
        .body("category", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_CATEGORY))
        .body("name", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_NAME))
        .body("deploymentId", equalTo(MockProvider.EXAMPLE_DEPLOYMENT_ID))
        .body("version", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_VERSION))
        .body("resource", equalTo(MockProvider.EXAMPLE_DECISION_DEFINITION_RESOURCE_NAME))
        .body("decisionRequirementsDefinitionId", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_ID))
        .body("decisionRequirementsDefinitionKey", equalTo(MockProvider.EXAMPLE_DECISION_REQUIREMENTS_DEFINITION_KEY))
        .body("tenantId", equalTo(MockProvider.EXAMPLE_TENANT_ID))
    .when()
      .get(SINGLE_DECISION_DEFINITION_BY_KEY_AND_TENANT_ID_URL);

    verify(decisionDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(repositoryServiceMock).getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testNonExistingDecisionDefinitionRetrieval_ByKeyAndTenantId() {
    String nonExistingKey = "aNonExistingDefinitionKey";
    String nonExistingTenantId = "aNonExistingTenantId";

    when(repositoryServiceMock.createDecisionDefinitionQuery().decisionDefinitionKey(nonExistingKey)).thenReturn(decisionDefinitionQueryMock);
    when(decisionDefinitionQueryMock.singleResult()).thenReturn(null);

    given()
      .pathParam("key", nonExistingKey)
      .pathParam("tenant-id", nonExistingTenantId)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", is(RestException.class.getSimpleName()))
      .body("message", containsString("No matching decision definition with key: " + nonExistingKey + " and tenant-id: " + nonExistingTenantId))
    .when().get(SINGLE_DECISION_DEFINITION_BY_KEY_AND_TENANT_ID_URL);
  }

  @Test
  void testEvaluateDecisionByKeyAndTenantId() {
    DecisionDefinition mockDefinition = MockProvider.mockDecisionDefinition().tenantId(MockProvider.EXAMPLE_TENANT_ID).build();
    setUpRuntimeData(mockDefinition);

    DmnDecisionResult decisionResult = MockProvider.createMockDecisionResult();
    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables",
        VariablesBuilder.create()
          .variable("amount", 420)
          .variable("invoiceCategory", "MISC")
          .getVariables()
    );

    given()
      .pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .pathParam("tenant-id", MockProvider.EXAMPLE_TENANT_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
    .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when().post(EVALUATE_DECISION_BY_KEY_AND_TENANT_ID_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("amount", 420);
    expectedVariables.put("invoiceCategory", "MISC");

    verify(decisionDefinitionQueryMock).tenantIdIn(MockProvider.EXAMPLE_TENANT_ID);
    verify(decisionEvaluationBuilderMock).variables(expectedVariables);
    verify(decisionEvaluationBuilderMock).evaluate();
  }

  @Test
  void testDecisionDiagramRetrieval() throws FileNotFoundException, URISyntaxException {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID))
        .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType("image/png")
          .header("Content-Disposition", "attachment; " +
                  "filename=\"" + MockProvider.EXAMPLE_DECISION_DEFINITION_DIAGRAM_RESOURCE_NAME + "\"; " +
                  "filename*=UTF-8''" + MockProvider.EXAMPLE_DECISION_DEFINITION_DIAGRAM_RESOURCE_NAME)
        .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
    verify(repositoryServiceMock).getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "decision diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testDecisionDiagramNullFilename() throws FileNotFoundException, URISyntaxException {
    // setup additional mock behavior
    File file = getFile("/processes/todo-process.png");
    when(repositoryServiceMock.getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID).getDiagramResourceName())
      .thenReturn(null);
    when(repositoryServiceMock.getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID))
        .thenReturn(new FileInputStream(file));

    // call method
    byte[] actual = given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType("application/octet-stream")
      .header("Content-Disposition", "attachment; " +
              "filename=\"" + null + "\"; "+
              "filename*=UTF-8''" + null)
      .when().get(DIAGRAM_DEFINITION_URL).getBody().asByteArray();

    // verify service interaction
    verify(repositoryServiceMock).getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);

    // compare input stream with response body bytes
    byte[] expected = IoUtil.readInputStream(new FileInputStream(file), "decision diagram");
    assertThat(actual).containsExactly(expected);
  }

  @Test
  void testDecisionDiagramNotExist() {
    // setup additional mock behavior
    when(repositoryServiceMock.getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID)).thenReturn(null);

    // call method
    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .expect().statusCode(Status.NO_CONTENT.getStatusCode())
        .when().get(DIAGRAM_DEFINITION_URL);

    // verify service interaction
    verify(repositoryServiceMock).getDecisionDefinition(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
    verify(repositoryServiceMock).getDecisionDiagram(MockProvider.EXAMPLE_DECISION_DEFINITION_ID);
  }

  @Test
  void testDecisionDiagramMediaType() {
    Assertions.assertEquals("image/png", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.png"));
    Assertions.assertEquals("image/png", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.PNG"));
    Assertions.assertEquals("image/svg+xml", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.svg"));
    Assertions.assertEquals("image/jpeg", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.jpeg"));
    Assertions.assertEquals("image/jpeg", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.jpg"));
    Assertions.assertEquals("image/gif", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.gif"));
    Assertions.assertEquals("image/bmp", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.bmp"));
    Assertions.assertEquals("application/octet-stream", ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix("decision.UNKNOWN"));
  }

  @Test
  void testEvaluateDecisionByKey() {
    DmnDecisionResult decisionResult = MockProvider.createMockDecisionResult();

    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables",
        VariablesBuilder.create()
          .variable("amount", 420)
          .variable("invoiceCategory", "MISC")
          .getVariables()
    );

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when().post(EVALUATE_DECISION_BY_KEY_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("amount", 420);
    expectedVariables.put("invoiceCategory", "MISC");

    verify(decisionEvaluationBuilderMock).variables(expectedVariables);
    verify(decisionEvaluationBuilderMock).evaluate();
  }

  @Test
  void testEvaluateDecisionById() {
    DmnDecisionResult decisionResult = MockProvider.createMockDecisionResult();

    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables",
        VariablesBuilder.create()
          .variable("amount", 420)
          .variable("invoiceCategory", "MISC")
          .getVariables()
    );

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
      .when().post(EVALUATE_DECISION_URL);

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("amount", 420);
    expectedVariables.put("invoiceCategory", "MISC");

    verify(decisionEvaluationBuilderMock).variables(expectedVariables);
    verify(decisionEvaluationBuilderMock).evaluate();
  }

  @Test
  void testEvaluateDecisionSingleDecisionOutput() {
    DmnDecisionResult decisionResult = new MockDecisionResultBuilder()
        .resultEntries()
          .entry("status", Variables.stringValue("gold"))
        .build();

    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("size()", is(1))
        .body("[0].size()", is(1))
        .body("[0].status", is(notNullValue()))
        .body("[0].status.value", is("gold"))
      .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionMultipleDecisionOutputs() {
    DmnDecisionResult decisionResult = new MockDecisionResultBuilder()
        .resultEntries()
          .entry("status", Variables.stringValue("gold"))
        .resultEntries()
          .entry("assignee", Variables.stringValue("manager"))
        .build();

    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("size()", is(2))
        .body("[0].size()", is(1))
        .body("[0].status.value", is("gold"))
        .body("[1].size()", is(1))
        .body("[1].assignee.value", is("manager"))

      .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionMultipleDecisionValues() {
    DmnDecisionResult decisionResult = new MockDecisionResultBuilder()
        .resultEntries()
          .entry("status", Variables.stringValue("gold"))
          .entry("assignee", Variables.stringValue("manager"))
        .build();

    when(decisionEvaluationBuilderMock.evaluate()).thenReturn(decisionResult);

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("size()", is(1))
        .body("[0].size()", is(2))
        .body("[0].status.value", is("gold"))
        .body("[0].assignee.value", is("manager"))

      .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecision_NotFound() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new NotFoundException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionByKey_NotFound() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new NotFoundException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void shouldReturnErrorCode() {
    when(decisionEvaluationBuilderMock.evaluate())
        .thenThrow(new ProcessEngineException("foo", 123));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
        .contentType(POST_JSON_CONTENT_TYPE).body(json)
        .then().expect()
          .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
          .body("type", is(RestException.class.getSimpleName()))
          .body("message", containsString("foo"))
          .body("code", equalTo(123))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void testEvaluateDecision_NotValid() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new NotValidException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionByKey_NotValid() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new NotValidException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(InvalidRequestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void testEvaluateDecision_NotAuthorized() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new AuthorizationException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionByKey_NotAuthorized() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new AuthorizationException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.FORBIDDEN.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void testEvaluateDecision_ProcessEngineException() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new ProcessEngineException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionByKey_ProcessEngineException() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new ProcessEngineException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void testEvaluateDecision_DmnEngineException() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new DmnEngineException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_URL);
  }

  @Test
  void testEvaluateDecisionByKey_DmnEngineException() {
    String message = "expected message";
    when(decisionEvaluationBuilderMock.evaluate()).thenThrow(new DmnEngineException(message));

    Map<String, Object> json = new HashMap<>();
    json.put("variables", Collections.emptyMap());

    given().pathParam("key", MockProvider.EXAMPLE_DECISION_DEFINITION_KEY)
      .contentType(POST_JSON_CONTENT_TYPE).body(json)
      .then().expect()
        .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).contentType(ContentType.JSON)
        .body("type", is(RestException.class.getSimpleName()))
        .body("message", containsString(message))
    .when().post(EVALUATE_DECISION_BY_KEY_URL);
  }

  @Test
  void testUpdateHistoryTimeToLive() {
    given()
        .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(5))
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, 5);
  }

  @Test
  void testUpdateHistoryTimeToLiveNullValue() {
    given()
        .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto())
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.NO_CONTENT.getStatusCode())
        .when()
        .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, null);
  }

  @Test
  void testUpdateHistoryTimeToLiveNegativeValue() {
    String expectedMessage = "expectedMessage";

    doThrow(new BadUserRequestException(expectedMessage))
        .when(repositoryServiceMock)
        .updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, -1);

    given()
        .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(-1))
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.BAD_REQUEST.getStatusCode())
        .body("type", is(BadUserRequestException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
        .when()
        .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, -1);
  }

  @Test
  void testUpdateHistoryTimeToLiveAuthorizationException() {
    String expectedMessage = "expectedMessage";

    doThrow(new AuthorizationException(expectedMessage))
        .when(repositoryServiceMock)
        .updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, 5);

    given()
        .pathParam("id", MockProvider.EXAMPLE_DECISION_DEFINITION_ID)
        .body(new HistoryTimeToLiveDto(5))
        .contentType(ContentType.JSON)
        .then().expect()
        .statusCode(Status.FORBIDDEN.getStatusCode())
        .body("type", is(AuthorizationException.class.getSimpleName()))
        .body("message", containsString(expectedMessage))
        .when()
        .put(UPDATE_HISTORY_TIME_TO_LIVE_URL);

    verify(repositoryServiceMock).updateDecisionDefinitionHistoryTimeToLive(MockProvider.EXAMPLE_DECISION_DEFINITION_ID, 5);
  }

}
