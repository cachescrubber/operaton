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
package org.operaton.bpm.dmn.engine.impl;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.model.dmn.DmnModelInstance;

public class DefaultDmnEngine implements DmnEngine {

  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected DefaultDmnEngineConfiguration dmnEngineConfiguration;
  protected DmnTransformer transformer;

  public DefaultDmnEngine(DefaultDmnEngineConfiguration dmnEngineConfiguration) {
    this.dmnEngineConfiguration = dmnEngineConfiguration;
    this.transformer = dmnEngineConfiguration.getTransformer();
  }

  @Override
  public DmnEngineConfiguration getConfiguration() {
    return dmnEngineConfiguration;
  }

  @Override
  public List<DmnDecision> parseDecisions(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    return transformer.createTransform()
      .modelInstance(inputStream)
      .transformDecisions();
  }

  @Override
  public List<DmnDecision> parseDecisions(DmnModelInstance dmnModelInstance) {
    ensureNotNull("dmnModelInstance", dmnModelInstance);
    return transformer.createTransform()
      .modelInstance(dmnModelInstance)
      .transformDecisions();
  }

  @Override
  public DmnDecision parseDecision(String decisionKey, InputStream inputStream) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return decision;
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

  @Override
  public DmnDecision parseDecision(String decisionKey, DmnModelInstance dmnModelInstance) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return decision;
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

  @Override
  public DmnDecisionRequirementsGraph parseDecisionRequirementsGraph(InputStream inputStream) {
    ensureNotNull("inputStream", inputStream);
    return transformer.createTransform()
      .modelInstance(inputStream)
      .transformDecisionRequirementsGraph();
  }

  @Override
  public DmnDecisionRequirementsGraph parseDecisionRequirementsGraph(DmnModelInstance dmnModelInstance) {
    ensureNotNull("dmnModelInstance", dmnModelInstance);
    return transformer.createTransform()
      .modelInstance(dmnModelInstance)
      .transformDecisionRequirementsGraph();
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(DmnDecision decision, Map<String, Object> variables) {
    ensureNotNull("decision", decision);
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decision, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(DmnDecision decision, VariableContext variableContext) {
    ensureNotNull("decision", decision);
    ensureNotNull("variableContext", variableContext);

    if (decision instanceof DmnDecisionImpl && decision.isDecisionTable()) {
      DefaultDmnDecisionContext decisionContext = new DefaultDmnDecisionContext(dmnEngineConfiguration);

      DmnDecisionResult decisionResult = decisionContext.evaluateDecision(decision, variableContext);
      return DmnDecisionTableResultImpl.wrap(decisionResult);
    }
    else {
      throw LOG.decisionIsNotADecisionTable(decision);
    }
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, InputStream inputStream, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decisionKey, inputStream, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, InputStream inputStream, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecisionTable(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, DmnModelInstance dmnModelInstance, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecisionTable(decisionKey, dmnModelInstance, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTable(String decisionKey, DmnModelInstance dmnModelInstance, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecisionTable(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

  @Override
  public DmnDecisionResult evaluateDecision(DmnDecision decision, Map<String, Object> variables) {
    ensureNotNull("decision", decision);
    ensureNotNull("variables", variables);
    return evaluateDecision(decision, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionResult evaluateDecision(DmnDecision decision, VariableContext variableContext) {
    ensureNotNull("decision", decision);
    ensureNotNull("variableContext", variableContext);

    if (decision instanceof DmnDecisionImpl) {
      DefaultDmnDecisionContext decisionContext = new DefaultDmnDecisionContext(dmnEngineConfiguration);
      return decisionContext.evaluateDecision(decision, variableContext);
    }
    else {
      throw LOG.decisionTypeNotSupported(decision);
    }
  }

  @Override
  public DmnDecisionResult evaluateDecision(String decisionKey, InputStream inputStream, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecision(decisionKey, inputStream, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionResult evaluateDecision(String decisionKey, InputStream inputStream, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(inputStream);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecision(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

  @Override
  public DmnDecisionResult evaluateDecision(String decisionKey, DmnModelInstance dmnModelInstance, Map<String, Object> variables) {
    ensureNotNull("variables", variables);
    return evaluateDecision(decisionKey, dmnModelInstance, Variables.fromMap(variables).asVariableContext());
  }

  @Override
  public DmnDecisionResult evaluateDecision(String decisionKey, DmnModelInstance dmnModelInstance, VariableContext variableContext) {
    ensureNotNull("decisionKey", decisionKey);
    List<DmnDecision> decisions = parseDecisions(dmnModelInstance);
    for (DmnDecision decision : decisions) {
      if (decisionKey.equals(decision.getKey())) {
        return evaluateDecision(decision, variableContext);
      }
    }
    throw LOG.unableToFindDecisionWithKey(decisionKey);
  }

}
