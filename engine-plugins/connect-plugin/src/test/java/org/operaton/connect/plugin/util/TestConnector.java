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
package org.operaton.connect.plugin.util;

import org.operaton.connect.impl.AbstractConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Meyer
 *
 */
public class TestConnector extends AbstractConnector<TestConnectorRequest, TestConnectorResponse> {

  public static final String ID = "testConnector";

  public static Map<String, Object> requestParameters;
  public static Map<String, Object> responseParameters = new HashMap<>();

  public TestConnector(String connectorId) {
    super(connectorId);
  }

  @Override
  public TestConnectorRequest createRequest() {
    return new TestConnectorRequest(this);
  }

  public TestConnectorResponse execute(TestConnectorRequest req) {
    // capture request parameters
    requestParameters = req.getRequestParameters();

    TestRequestInvocation testRequestInvocation = new TestRequestInvocation(null, req, requestInterceptors);

    try {
      testRequestInvocation.proceed();
      // use response parameters
      return new TestConnectorResponse(responseParameters);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
