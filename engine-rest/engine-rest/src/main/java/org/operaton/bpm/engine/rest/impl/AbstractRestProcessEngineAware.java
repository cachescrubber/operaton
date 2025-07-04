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
package org.operaton.bpm.engine.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response.Status;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.util.EngineUtil;

public abstract class AbstractRestProcessEngineAware {

  /*
   * private to enforce the use of getProcessEngine in subclasses
   * for consistent error handling for unknown process engines
   */
  private final ProcessEngine processEngine;

  protected ObjectMapper objectMapper;

  protected String relativeRootResourcePath = "/";

  protected AbstractRestProcessEngineAware(String engineName, final ObjectMapper objectMapper) {
    this.processEngine = EngineUtil.lookupProcessEngine(engineName);
    this.objectMapper = objectMapper;
  }

  /**
   * Override the root resource path, if this resource is a sub-resource.
   * The relative root resource path is used for generation of links to resources in results.
   *
   * @param relativeRootResourcePath
   */
  public void setRelativeRootResourceUri(String relativeRootResourcePath) {
    this.relativeRootResourcePath = relativeRootResourcePath;
  }

  protected ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  protected ProcessEngine getProcessEngine() {
    if (processEngine == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "No process engine available");
    }
    return processEngine;
  }
}
