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
package org.operaton.bpm.engine.rest.impl.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.CleanableHistoricDecisionInstanceReportDto;
import org.operaton.bpm.engine.rest.dto.history.CleanableHistoricDecisionInstanceReportResultDto;
import org.operaton.bpm.engine.rest.history.HistoricDecisionDefinitionRestService;
import org.operaton.bpm.engine.rest.util.QueryUtil;

public class HistoricDecisionDefinitionRestServiceImpl implements HistoricDecisionDefinitionRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricDecisionDefinitionRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public List<CleanableHistoricDecisionInstanceReportResultDto> getCleanableHistoricDecisionInstanceReport(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    CleanableHistoricDecisionInstanceReportDto queryDto = new CleanableHistoricDecisionInstanceReportDto(objectMapper, uriInfo.getQueryParameters());
    CleanableHistoricDecisionInstanceReport query = queryDto.toQuery(processEngine);

    List<CleanableHistoricDecisionInstanceReportResult> reportResult = QueryUtil.list(query, firstResult, maxResults);

    return CleanableHistoricDecisionInstanceReportResultDto.convert(reportResult);
  }

  @Override
  public CountResultDto getCleanableHistoricDecisionInstanceReportCount(UriInfo uriInfo) {
    CleanableHistoricDecisionInstanceReportDto queryDto = new CleanableHistoricDecisionInstanceReportDto(objectMapper, uriInfo.getQueryParameters());
    queryDto.setObjectMapper(objectMapper);
    CleanableHistoricDecisionInstanceReport query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }
}
