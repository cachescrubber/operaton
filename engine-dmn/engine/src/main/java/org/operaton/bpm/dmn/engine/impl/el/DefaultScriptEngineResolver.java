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
package org.operaton.bpm.dmn.engine.impl.el;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;

public class DefaultScriptEngineResolver implements DmnScriptEngineResolver {

  protected Map<String, ScriptEngine> scriptEngineCache = new HashMap<>();

  protected ScriptEngineManager scriptEngineManager;

  public DefaultScriptEngineResolver(ScriptEngineManager scriptEngineManager) {
    this.scriptEngineManager = scriptEngineManager;
  }

  public DefaultScriptEngineResolver() {
    this(new ScriptEngineManager());
  }

  @Override
  public ScriptEngine getScriptEngineForLanguage(String language) {
    ensureNotNull("language", language);

    ScriptEngine scriptEngine = scriptEngineCache.get(language);

    if (scriptEngine == null) {
      synchronized (this) {
        scriptEngine = scriptEngineCache.computeIfAbsent(language, scriptEngineManager::getEngineByName);
      }
    }

    return scriptEngine;
  }

}
