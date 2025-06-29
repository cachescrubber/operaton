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
package org.operaton.commons.logging;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * Base class for implementing a logger class. A logger class is a class with
 * dedicated methods for each log message:
 *
 * <pre>
 * public class MyLogger extends BaseLogger {
 *
 *   public static MyLogger LOG = createLogger(MyLogger.class, "MYPROJ", "org.example", "01");
 *
 *   public void engineStarted(long currentTime) {
 *     logInfo("100", "My super engine has started at '{}'", currentTime);
 *   }
 *
 * }
 * </pre>
 *
 * The logger can then be used in the following way:
 *
 * <pre>
 * LOG.engineStarted(System.currentTimeMilliseconds());
 * </pre>
 *
 * This will print the following message:
 * <pre>
 * INFO  org.example - MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * <h2>Slf4j</h2>
 * This class uses slf4j as logging API. The class ensures that log messages and exception
 * messages are always formatted using the same template.
 *
 * <h2>Log message format</h2>
 * The log message format produced by this class is as follows:
 * <pre>
 * [PROJECT_CODE]-[COMPONENT_ID][MESSAGE_ID] message
 * </pre>
 * Example:
 * <pre>
 * MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 *
 */
public abstract class BaseLogger {

  /** the slf4j logger we delegate to */
  protected Logger delegateLogger;

  /** the project code of the logger */
  protected String projectCode;

  /** the component id of the logger. */
  protected String componentId;

  protected BaseLogger() {
  }

  /**
   * Creates a new instance of the {@link BaseLogger Logger}.
   *
   * @param loggerClass the type of the logger
   * @param projectCode the unique code for a complete project.
   * @param name the name of the slf4j logger to use.
   * @param componentId the unique id of the component.
   */
  public static <T extends BaseLogger> T createLogger(Class<T> loggerClass, String projectCode, String name, String componentId) {
    try {
      T logger = loggerClass.getDeclaredConstructor().newInstance();
      logger.projectCode = projectCode;
      logger.componentId = componentId;
      logger.delegateLogger = LoggerFactory.getLogger(name);

      return logger;

    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException("Unable to instantiate logger '" + loggerClass.getName() + "'", e);

    }
  }

  /**
   * Logs a message with the specified log level. If the log level cannot be matched, it defaults to DEBUG.
   *
   * @param level           the log level
   * @param id              the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters      a list of optional parameters
   */
  protected void log(String level, String id, String messageTemplate, Object... parameters) {
    log(level, Level.DEBUG, id, messageTemplate, parameters);
  }

  /**
   * Logs a message with the specified log level.
   *
   * @param level           the log level
   * @param defaultLevel    the default log level to use when log level cannot be matched
   * @param id              the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters      a list of optional parameters
   */
  protected void log(String level, Level defaultLevel, String id, String messageTemplate, Object... parameters) {
    switch (Level.parse(level, defaultLevel)) {
    case ERROR:
      logError(id, messageTemplate, parameters);
      break;
    case WARN:
      logWarn(id, messageTemplate, parameters);
      break;
    case INFO:
      logInfo(id, messageTemplate, parameters);
      break;
    case DEBUG:
      logDebug(id, messageTemplate, parameters);
      break;
    case TRACE:
      logTrace(id, messageTemplate, parameters);
      break;
    }
  }

  /**
   * Logs a 'TRACE' message
   *
   * @param id              the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters      a list of optional parameters
   */
  protected void logTrace(String id, String messageTemplate, Object... parameters) {
    if (isTraceEnabled()) {
      String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.trace(msg, sanitizeParameters(parameters));
    }
  }

  /**
   * Logs a 'DEBUG' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logDebug(String id, String messageTemplate, Object... parameters) {
    if(isDebugEnabled()) {
      String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.debug(msg, sanitizeParameters(parameters));
    }
  }

  /**
   * Logs an 'INFO' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logInfo(String id, String messageTemplate, Object... parameters) {
    if (isInfoEnabled()) {
      String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.info(msg, sanitizeParameters(parameters));
    }
  }

  /**
   * Logs an 'WARN' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logWarn(String id, String messageTemplate, Object... parameters) {
    if(isWarnEnabled()) {
      String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.warn(msg, sanitizeParameters(parameters));
    }
  }

  /**
   * Logs an 'ERROR' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logError(String id, String messageTemplate, Object... parameters) {
    if(isErrorEnabled()) {
      String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.error(msg, sanitizeParameters(parameters));
    }
  }

  /**
   * @return true if the logger will log 'TRACE' messages
   */
  public boolean isTraceEnabled() {
    return delegateLogger.isTraceEnabled();
  }

  /**
   * @return true if the logger will log 'DEBUG' messages
   */
  public boolean isDebugEnabled() {
    return delegateLogger.isDebugEnabled();
  }

  /**
   * @return true if the logger will log 'INFO' messages
   */
  public boolean isInfoEnabled() {
    return delegateLogger.isInfoEnabled();
  }

  /**
   * @return true if the logger will log 'WARN' messages
   */
  public boolean isWarnEnabled() {
    return delegateLogger.isWarnEnabled();
  }

  /**
   * @return true if the logger will log 'ERROR' messages
   */
  public boolean isErrorEnabled() {
    return delegateLogger.isErrorEnabled();
  }

  /**
   * Formats a message template
   *
   * @param id the id of the message
   * @param messageTemplate the message template to use
   *
   * @return the formatted template
   */
  protected String formatMessageTemplate(String id, String messageTemplate) {
    return projectCode + "-" + componentId + id + " " + messageTemplate;
  }

  /**
   * Prepares an exception message
   *
   * @param id the id of the message
   * @param messageTemplate the message template to use
   * @param parameters the parameters for the message (optional)
   *
   * @return the prepared exception message
   */
  protected String exceptionMessage(String id, String messageTemplate, Object... parameters) {
    String formattedTemplate = formatMessageTemplate(id, messageTemplate);
    if(parameters == null || parameters.length == 0) {
      return formattedTemplate;

    } else {
      return MessageFormatter.arrayFormat(formattedTemplate, parameters).getMessage();

    }
  }

  /**
   *  Sanitize parameters to avoid injection attacks
   */
  @SuppressWarnings("java:S1168")
  Object[] sanitizeParameters(Object... parameters) {
    if (parameters == null) {
      return null;
    }
    Object[] sanitized = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      sanitized[i] = sanitize(parameters[i]);
    }
    return sanitized;
  }

  private Object sanitize(Object parameter) {
    if (parameter instanceof String param) {
      return param.replaceAll("[\n\r\t]", "_");
    }
    return parameter;
  }

}
