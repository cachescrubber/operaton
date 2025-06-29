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
package org.operaton.bpm.engine.rest.sub.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.form.OperatonFormRef;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.management.ActivityStatisticsQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.rest.ProcessInstanceRestService;
import org.operaton.bpm.engine.rest.dto.StatisticsResultDto;
import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.repository.ActivityStatisticsResultDto;
import org.operaton.bpm.engine.rest.dto.repository.CalledProcessDefinitionDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDiagramDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceWithVariablesDto;
import org.operaton.bpm.engine.rest.dto.runtime.RestartProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationInstructionDto;
import org.operaton.bpm.engine.rest.dto.task.FormDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.repository.ProcessDefinitionResource;
import org.operaton.bpm.engine.rest.util.ApplicationContextPathUtil;
import org.operaton.bpm.engine.rest.util.ContentTypeUtil;
import org.operaton.bpm.engine.rest.util.EncodingUtil;
import org.operaton.bpm.engine.rest.util.URLEncodingUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.operaton.bpm.engine.runtime.RestartProcessInstanceBuilder;
import org.operaton.bpm.engine.variable.VariableMap;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ProcessDefinitionResourceImpl implements ProcessDefinitionResource {

  public static final String MSG_CANNOT_INSTANTIATE_PROCESS_DEF = "Cannot instantiate process definition %s: %s";
  protected ProcessEngine engine;
  protected String processDefinitionId;
  protected String rootResourcePath;
  protected ObjectMapper objectMapper;

  public ProcessDefinitionResourceImpl(ProcessEngine engine, String processDefinitionId, String rootResourcePath, ObjectMapper objectMapper) {
    this.engine = engine;
    this.processDefinitionId = processDefinitionId;
    this.rootResourcePath = rootResourcePath;
    this.objectMapper = objectMapper;
  }

  @Override
  public ProcessDefinitionDto getProcessDefinition() {
    RepositoryService repoService = engine.getRepositoryService();

    ProcessDefinition definition;
    try {
      definition = repoService.getProcessDefinition(processDefinitionId);
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, "No matching definition with id " + processDefinitionId);
    }

    return ProcessDefinitionDto.fromProcessDefinition(definition);
  }

  @Override
  public Response deleteProcessDefinition(boolean cascade, boolean skipCustomListeners, boolean skipIoMappings) {
    RepositoryService repositoryService = engine.getRepositoryService();

    try {
      repositoryService.deleteProcessDefinition(processDefinitionId, cascade, skipCustomListeners, skipIoMappings);
    } catch (NotFoundException nfe) {
      throw new InvalidRequestException(Status.NOT_FOUND, nfe, nfe.getMessage());
    }
    return Response.ok().build();
  }

  @Override
  public ProcessInstanceDto startProcessInstance(UriInfo context, StartProcessInstanceDto parameters) {
    ProcessInstanceWithVariables instance;
    try {
      instance = startProcessInstanceAtActivities(parameters);
    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      String errorMessage = String.format(MSG_CANNOT_INSTANTIATE_PROCESS_DEF, processDefinitionId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);

    } catch (RestException e) {
      String errorMessage = String.format(MSG_CANNOT_INSTANTIATE_PROCESS_DEF, processDefinitionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    }

    ProcessInstanceDto result;
    if (parameters.isWithVariablesInReturn()) {
      result = ProcessInstanceWithVariablesDto.fromProcessInstance(instance);
    }
    else {
     result = ProcessInstanceDto.fromProcessInstance(instance);
    }

    URI uri = context.getBaseUriBuilder()
      .path(rootResourcePath)
      .path(ProcessInstanceRestService.PATH)
      .path(instance.getId())
      .build();

    result.addReflexiveLink(uri, HttpMethod.GET, "self");

    return result;
  }

  protected ProcessInstanceWithVariables startProcessInstanceAtActivities(StartProcessInstanceDto dto) {
    Map<String, Object> processInstanceVariables = VariableValueDto.toMap(dto.getVariables(), engine, objectMapper);
    String businessKey = dto.getBusinessKey();
    String caseInstanceId = dto.getCaseInstanceId();

    ProcessInstantiationBuilder instantiationBuilder = engine.getRuntimeService()
        .createProcessInstanceById(processDefinitionId)
        .businessKey(businessKey)
        .caseInstanceId(caseInstanceId)
        .setVariables(processInstanceVariables);

    if (dto.getStartInstructions() != null && !dto.getStartInstructions().isEmpty()) {
      for (ProcessInstanceModificationInstructionDto instruction : dto.getStartInstructions()) {
        instruction.applyTo(instantiationBuilder, engine, objectMapper);
      }
    }

    return instantiationBuilder.executeWithVariablesInReturn(dto.isSkipCustomListeners(), dto.isSkipIoMappings());
  }

  @Override
  public ProcessInstanceDto submitForm(UriInfo context, StartProcessInstanceDto parameters) {
    FormService formService = engine.getFormService();

    ProcessInstance instance;
    try {
      Map<String, Object> variables = VariableValueDto.toMap(parameters.getVariables(), engine, objectMapper);
      String businessKey = parameters.getBusinessKey();
      if (businessKey != null) {
        instance = formService.submitStartForm(processDefinitionId, businessKey, variables);
      } else {
        instance = formService.submitStartForm(processDefinitionId, variables);
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (FormFieldValidationException e) {
      String errorMessage = String.format(MSG_CANNOT_INSTANTIATE_PROCESS_DEF, processDefinitionId, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);
    } catch (ProcessEngineException e) {
      String errorMessage = String.format(MSG_CANNOT_INSTANTIATE_PROCESS_DEF, processDefinitionId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    } catch (RestException e) {
      String errorMessage = String.format(MSG_CANNOT_INSTANTIATE_PROCESS_DEF, processDefinitionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);
    }

    ProcessInstanceDto result = ProcessInstanceDto.fromProcessInstance(instance);

    URI uri = context.getBaseUriBuilder()
      .path(rootResourcePath)
      .path(ProcessInstanceRestService.PATH)
      .path(instance.getId())
      .build();

    result.addReflexiveLink(uri, HttpMethod.GET, "self");

    return result;
  }


  @Override
  public List<StatisticsResultDto> getActivityStatistics(Boolean includeFailedJobs, Boolean includeIncidents, String includeIncidentsForType) {
    if (includeIncidents != null && includeIncidentsForType != null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Only one of the query parameter includeIncidents or includeIncidentsForType can be set.");
    }

    ManagementService mgmtService = engine.getManagementService();
    ActivityStatisticsQuery query = mgmtService.createActivityStatisticsQuery(processDefinitionId);

    if (includeFailedJobs != null && includeFailedJobs) {
      query.includeFailedJobs();
    }

    if (includeIncidents != null && includeIncidents) {
      query.includeIncidents();
    } else if (includeIncidentsForType != null) {
      query.includeIncidentsForType(includeIncidentsForType);
    }

    List<ActivityStatistics> queryResults = query.unlimitedList();

    List<StatisticsResultDto> results = new ArrayList<>();
    for (ActivityStatistics queryResult : queryResults) {
      StatisticsResultDto dto = ActivityStatisticsResultDto.fromActivityStatistics(queryResult);
      results.add(dto);
    }

    return results;
  }

  @Override
  public ProcessDefinitionDiagramDto getProcessDefinitionBpmn20Xml() {
    InputStream processModelIn = null;
    try {
      processModelIn = engine.getRepositoryService().getProcessModel(processDefinitionId);
      byte[] processModel = IoUtil.readInputStream(processModelIn, "processModelBpmn20Xml");
      return ProcessDefinitionDiagramDto.create(processDefinitionId, new String(processModel, UTF_8));
    } catch (AuthorizationException e) {
      throw e;
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, "No matching definition with id " + processDefinitionId);
    } finally {
      IoUtil.closeSilently(processModelIn);
    }
  }

  @Override
  public Response getProcessDefinitionDiagram() {
    ProcessDefinition definition = engine.getRepositoryService().getProcessDefinition(processDefinitionId);
    InputStream processDiagram = engine.getRepositoryService().getProcessDiagram(processDefinitionId);
    if (processDiagram == null) {
      return Response.noContent().build();
    } else {
      String fileName = definition.getDiagramResourceName();
      return Response.ok(processDiagram)
          .header("Content-Disposition", URLEncodingUtil.buildAttachmentValue(fileName))
          .type(getMediaTypeForFileSuffix(fileName)).build();
    }
  }

  /**
   * Determines an IANA media type based on the file suffix.
   * Hint: as of Java 7 the method Files.probeContentType() provides an implementation based on file type detection.
   *
   * @param fileName
   * @return content type, defaults to octet-stream
   */
  public static String getMediaTypeForFileSuffix(String fileName) {
    String mediaType = "application/octet-stream"; // default
    if (fileName != null) {
      fileName = fileName.toLowerCase();
      if (fileName.endsWith(".png")) {
        mediaType = "image/png";
      } else if (fileName.endsWith(".svg")) {
        mediaType = "image/svg+xml";
      } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
        mediaType = "image/jpeg";
      } else if (fileName.endsWith(".gif")) {
        mediaType = "image/gif";
      } else if (fileName.endsWith(".bmp")) {
        mediaType = "image/bmp";
      }
    }
    return mediaType;
  }

  @Override
  public FormDto getStartForm() {
    final FormService formService = engine.getFormService();

    final StartFormData formData;
    try {
      formData = formService.getStartFormData(processDefinitionId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Cannot get start form data for process definition " + processDefinitionId);
    }
    FormDto dto = FormDto.fromFormData(formData);
    if((dto.getKey() == null || dto.getKey().isEmpty()) && dto.getOperatonFormRef() == null
        && formData != null && formData.getFormFields() != null && !formData.getFormFields().isEmpty()) {
        dto.setKey("embedded:engine://engine/:engine/process-definition/"+processDefinitionId+"/rendered-form");
      }

    dto.setContextPath(ApplicationContextPathUtil.getApplicationPathByProcessDefinitionId(engine, processDefinitionId));

    return dto;
  }

  @Override
  public Response getRenderedForm() {
    FormService formService = engine.getFormService();

    Object startForm = formService.getRenderedStartForm(processDefinitionId);
    if (startForm != null) {
      String content = startForm.toString();
      InputStream stream = new ByteArrayInputStream(content.getBytes(EncodingUtil.DEFAULT_ENCODING));
      return Response
          .ok(stream)
          .type(MediaType.APPLICATION_XHTML_XML)
          .build();
    }

    throw new InvalidRequestException(Status.NOT_FOUND, "No matching rendered start form for process definition with the id " + processDefinitionId + " found.");
  }

  @Override
  public void updateSuspensionState(ProcessDefinitionSuspensionStateDto dto) {
    try {
      dto.setProcessDefinitionId(processDefinitionId);
      dto.updateSuspensionState(engine);

    } catch (IllegalArgumentException e) {
      String message = String.format("The suspension state of Process Definition with id %s could not be updated due to: %s", processDefinitionId, e.getMessage());
      throw new InvalidRequestException(Status.BAD_REQUEST, e, message);
    }
  }

  @Override
  public void updateHistoryTimeToLive(HistoryTimeToLiveDto historyTimeToLiveDto) {
    engine.getRepositoryService().updateProcessDefinitionHistoryTimeToLive(processDefinitionId, historyTimeToLiveDto.getHistoryTimeToLive());
  }

  @Override
  public Map<String, VariableValueDto> getFormVariables(String variableNames, boolean deserializeValues) {

    final FormService formService = engine.getFormService();
    List<String> formVariables = null;

    if(variableNames != null) {
      StringListConverter stringListConverter = new StringListConverter();
      formVariables = stringListConverter.convertQueryParameterToType(variableNames);
    }

    VariableMap startFormVariables = formService.getStartFormVariables(processDefinitionId, formVariables, deserializeValues);

    return VariableValueDto.fromMap(startFormVariables);
  }

  @Override
  public List<CalledProcessDefinitionDto> getStaticCalledProcessDefinitions() {
    try {
      return engine.getRepositoryService().getStaticCalledProcessDefinitions(processDefinitionId).stream()
        .map(CalledProcessDefinitionDto::from)
        .toList();
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void restartProcessInstance(RestartProcessInstanceDto restartProcessInstanceDto) {
    try {
      createRestartProcessInstanceBuilder(restartProcessInstanceDto).execute();
    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public BatchDto restartProcessInstanceAsync(RestartProcessInstanceDto restartProcessInstanceDto) {
    try {
      Batch batch = createRestartProcessInstanceBuilder(restartProcessInstanceDto).executeAsync();
      return BatchDto.fromBatch(batch);
    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  private RestartProcessInstanceBuilder createRestartProcessInstanceBuilder(RestartProcessInstanceDto restartProcessInstanceDto) {
    RuntimeService runtimeService = engine.getRuntimeService();
    RestartProcessInstanceBuilder builder = runtimeService
        .restartProcessInstances(processDefinitionId);

    if (restartProcessInstanceDto.getProcessInstanceIds() != null) {
      builder.processInstanceIds(restartProcessInstanceDto.getProcessInstanceIds());
    }

    if (restartProcessInstanceDto.getHistoricProcessInstanceQuery() != null) {
      builder.historicProcessInstanceQuery(restartProcessInstanceDto.getHistoricProcessInstanceQuery().toQuery(engine));
    }

    if (restartProcessInstanceDto.isInitialVariables()) {
      builder.initialSetOfVariables();
    }

    if (restartProcessInstanceDto.isWithoutBusinessKey()) {
      builder.withoutBusinessKey();
    }

    if (restartProcessInstanceDto.isSkipCustomListeners()) {
      builder.skipCustomListeners();
    }

    if (restartProcessInstanceDto.isSkipIoMappings()) {
      builder.skipIoMappings();
    }
    restartProcessInstanceDto.applyTo(builder, engine, objectMapper);
    return builder;
  }

  @Override
  public Response getDeployedStartForm() {
    try {
      InputStream deployedStartForm = engine.getFormService().getDeployedStartForm(processDefinitionId);
      return Response.ok(deployedStartForm, getStartFormMediaType(processDefinitionId)).build();
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    } catch (NullValueException | BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    } catch (AuthorizationException e) {
      throw new InvalidRequestException(Status.FORBIDDEN, e.getMessage());
    }
  }

  protected String getStartFormMediaType(String processDefinitionId) {
    String formKey = engine.getFormService().getStartFormKey(processDefinitionId);
    OperatonFormRef operatonFormRef = engine.getFormService().getStartFormData(processDefinitionId).getOperatonFormRef();
    if(formKey != null) {
      return ContentTypeUtil.getFormContentType(formKey);
    } else if(operatonFormRef != null) {
      return ContentTypeUtil.getFormContentType(operatonFormRef);
    }
    return MediaType.APPLICATION_XHTML_XML;
  }
}
