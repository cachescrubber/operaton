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
package org.operaton.bpm.engine.impl;

import java.io.Serializable;

import org.operaton.bpm.engine.ProcessEngineInfo;


/**
 * @author Tom Baeyens
 */
public class ProcessEngineInfoImpl implements Serializable, ProcessEngineInfo {

  private static final long serialVersionUID = 1L;

  String name;
  String resourceUrl;
  String exception;

  public ProcessEngineInfoImpl(String name, String resourceUrl, String exception) {
    this.name = name;
    this.resourceUrl = resourceUrl;
    this.exception = exception;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getResourceUrl() {
    return resourceUrl;
  }

  @Override
  public String getException() {
    return exception;
  }
}
