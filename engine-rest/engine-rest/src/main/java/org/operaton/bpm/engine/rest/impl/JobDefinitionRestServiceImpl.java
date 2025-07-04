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
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.rest.JobDefinitionRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.management.JobDefinitionDto;
import org.operaton.bpm.engine.rest.dto.management.JobDefinitionQueryDto;
import org.operaton.bpm.engine.rest.dto.management.JobDefinitionSuspensionStateDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.management.JobDefinitionResource;
import org.operaton.bpm.engine.rest.sub.management.JobDefinitionResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;

/**
 * @author roman.smirnov
 */
public class JobDefinitionRestServiceImpl extends AbstractRestProcessEngineAware implements JobDefinitionRestService {

  public JobDefinitionRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public JobDefinitionResource getJobDefinition(String jobDefinitionId) {
    return new JobDefinitionResourceImpl(getProcessEngine(), jobDefinitionId);
  }

  @Override
  public List<JobDefinitionDto> getJobDefinitions(UriInfo uriInfo, Integer firstResult,
      Integer maxResults) {
    JobDefinitionQueryDto queryDto = new JobDefinitionQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryJobDefinitions(queryDto, firstResult, maxResults);

  }

  @Override
  public CountResultDto getJobDefinitionsCount(UriInfo uriInfo) {
    JobDefinitionQueryDto queryDto = new JobDefinitionQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryJobDefinitionsCount(queryDto);
  }

  @Override
  public List<JobDefinitionDto> queryJobDefinitions(JobDefinitionQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(getObjectMapper());
    JobDefinitionQuery query = queryDto.toQuery(getProcessEngine());

    List<JobDefinition> matchingJobDefinitions = QueryUtil.list(query, firstResult, maxResults);

    List<JobDefinitionDto> jobDefinitionResults = new ArrayList<>();
    for (JobDefinition jobDefinition : matchingJobDefinitions) {
      JobDefinitionDto result = JobDefinitionDto.fromJobDefinition(jobDefinition);
      jobDefinitionResults.add(result);
    }

    return jobDefinitionResults;
  }

  @Override
  public CountResultDto queryJobDefinitionsCount(JobDefinitionQueryDto queryDto) {
    queryDto.setObjectMapper(getObjectMapper());
    JobDefinitionQuery query = queryDto.toQuery(getProcessEngine());

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public void updateSuspensionState(JobDefinitionSuspensionStateDto dto) {
    if (dto.getJobDefinitionId() != null) {
      String message = "Either processDefinitionId or processDefinitionKey can be set to update the suspension state.";
      throw new InvalidRequestException(Status.BAD_REQUEST, message);
    }

    try {
      dto.updateSuspensionState(getProcessEngine());

    } catch (IllegalArgumentException e) {
      String message = String.format("Could not update the suspension state of Job Definitions due to: %s", e.getMessage()) ;
      throw new InvalidRequestException(Status.BAD_REQUEST, e, message);
    }
  }

}
