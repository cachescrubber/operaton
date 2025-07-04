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
package org.operaton.bpm.container.impl.jboss.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessApplicationService;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.impl.JakartaServletProcessApplication;
import org.operaton.bpm.container.ExecutorService;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.container.impl.jboss.util.BindingUtil;
import org.operaton.bpm.container.impl.jboss.util.PlatformServiceReferenceFactory;
import org.operaton.bpm.container.impl.jboss.util.ServiceTracker;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * <p>A {@link RuntimeContainerDelegate} implementation for JBoss AS 7</p>
 *
 * @author Daniel Meyer
 */
public class MscRuntimeContainerDelegate implements Service<MscRuntimeContainerDelegate>, RuntimeContainerDelegate, ProcessEngineService, ProcessApplicationService {

  // used for installing services
  protected ServiceTarget childTarget;
  // used for looking up services
  protected ServiceContainer serviceContainer;

  protected ServiceTracker<ProcessEngine> processEngineServiceTracker;
  protected Set<ProcessEngine> processEngines = new CopyOnWriteArraySet<>();

  protected ServiceTracker<MscManagedProcessApplication> processApplicationServiceTracker;
  protected Set<MscManagedProcessApplication> processApplications = new CopyOnWriteArraySet<>();
  protected Consumer<RuntimeContainerDelegate> provider;

  public MscRuntimeContainerDelegate(Consumer<RuntimeContainerDelegate> provider) {
    this.provider = provider;
  }

  // Lifecycle /////////////////////////////////////////////////

  @Override
  public void start(StartContext context) throws StartException {
    provider.accept(this);
    serviceContainer = context.getController().getServiceContainer();
    childTarget = context.getChildTarget();

    startTrackingServices();
    createJndiBindings();

    // set this implementation as Runtime Container
    RuntimeContainerDelegate.INSTANCE.set(this);
  }

  @Override
  public void stop(StopContext context) {
    provider.accept(null);
    stopTrackingServices();
  }

  @Override
  public MscRuntimeContainerDelegate getValue() throws IllegalStateException, IllegalArgumentException {
    return this;
  }

  // RuntimeContainerDelegate implementation /////////////////////////////

  @Override
  public void registerProcessEngine(ProcessEngine processEngine) {

    if(processEngine == null) {
      throw new ProcessEngineException("Cannot register process engine with Msc Runtime Container: process engine is 'null'");
    }

    ServiceName serviceName = ServiceNames.forManagedProcessEngine(processEngine.getName());

    if(serviceContainer.getService(serviceName) == null) {
      MscManagedProcessEngine processEngineRegistration = new MscManagedProcessEngine(processEngine);

      // install the service asynchronously.
      ServiceBuilder<?> serviceBuilder = childTarget.addService(serviceName);
      serviceBuilder.requires(ServiceNames.forMscRuntimeContainerDelegate());
      serviceBuilder.provides(serviceName);
      serviceBuilder.setInitialMode(Mode.ACTIVE);
      serviceBuilder.setInstance(processEngineRegistration.getRuntimeContainerDelegateSupplier().get());
      serviceBuilder.install();
    }

  }

  @Override
  @SuppressWarnings("unchecked")
  public void unregisterProcessEngine(ProcessEngine processEngine) {

    if(processEngine == null) {
      throw new ProcessEngineException("Cannot unregister process engine with Msc Runtime Container: process engine is 'null'");
    }

    ServiceName serviceName = ServiceNames.forManagedProcessEngine(processEngine.getName());

    // remove the service asynchronously
    ServiceController<ProcessEngine> service = (ServiceController<ProcessEngine>) serviceContainer.getService(serviceName);
    if(service != null && service.getService() instanceof MscManagedProcessEngine) {
      service.setMode(Mode.REMOVE);
    }

  }

  @Override
  public void deployProcessApplication(AbstractProcessApplication processApplication) {
    if(processApplication instanceof JakartaServletProcessApplication jakartaServletProcessApplication) {
      deployServletProcessApplication(jakartaServletProcessApplication);
    }
  }

  @SuppressWarnings("unchecked")
  protected void deployServletProcessApplication(JakartaServletProcessApplication processApplication) {

    ClassLoader contextClassloader = ClassLoaderUtil.getContextClassloader();
    String moduleName = ((ModuleClassLoader)contextClassloader).getModule().getName();

    ServiceName serviceName = ServiceNames.forNoViewProcessApplicationStartService(moduleName);
    ServiceName paModuleService = ServiceNames.forProcessApplicationModuleService(moduleName);

    if(serviceContainer.getService(serviceName) == null) {

      ServiceController<ServiceTarget> requiredService = (ServiceController<ServiceTarget>) serviceContainer.getRequiredService(paModuleService);

      ServiceBuilder<?> serviceBuilder = requiredService.getValue().addService(serviceName);
      Consumer<ProcessApplicationInterface> provider = serviceBuilder.provides(serviceName);
      NoViewProcessApplicationStartService service = new NoViewProcessApplicationStartService(processApplication.getReference(), provider);
      serviceBuilder.setInitialMode(Mode.ACTIVE);
      serviceBuilder.setInstance(service);
      serviceBuilder.install();

    }
  }

  @Override
  public void undeployProcessApplication(AbstractProcessApplication processApplication) {
    // nothing to do
  }

  @Override
  public ProcessEngineService getProcessEngineService() {
    return this;
  }

  @Override
  public ProcessApplicationService getProcessApplicationService() {
    return this;
  }

  @Override
  public ExecutorService getExecutorService() {
    return (ExecutorService) serviceContainer.getRequiredService(ServiceNames.forMscExecutorService()).getValue();
  }

  // ProcessEngineService implementation /////////////////////////////////

  @Override
  public ProcessEngine getDefaultProcessEngine() {
    return getProcessEngineService(ServiceNames.forDefaultProcessEngine());
  }

  @Override
  public List<ProcessEngine> getProcessEngines() {
    return new ArrayList<>(processEngines);
  }

  @Override
  public Set<String> getProcessEngineNames() {
    HashSet<String> result = new HashSet<>();
    for (ProcessEngine engine : processEngines) {
      result.add(engine.getName());
    }
    return result;
  }

  @Override
  public ProcessEngine getProcessEngine(String name) {
    return getProcessEngineService(ServiceNames.forManagedProcessEngine(name));
  }

  // ProcessApplicationService implementation //////////////////////////////

  @Override
  public ProcessApplicationInfo getProcessApplicationInfo(String processApplicationName) {
    MscManagedProcessApplication managedProcessApplication = getManagedProcessApplication(processApplicationName);

    if (managedProcessApplication == null) {
      return null;
    }
    else {
      return managedProcessApplication.getProcessApplicationInfo();
    }
  }

  @Override
  public Set<String> getProcessApplicationNames() {
    HashSet<String> result = new HashSet<>();
    for (MscManagedProcessApplication application : processApplications) {
      result.add(application.getProcessApplicationInfo().getName());
    }
    return result;
  }

  @Override
  public ProcessApplicationReference getDeployedProcessApplication(String name) {
    MscManagedProcessApplication managedPa = getManagedProcessApplication(name);
    if(managedPa == null) {
      return null;
    }
    else {
      return managedPa.getProcessApplicationReference();
    }
  }

  // internal implementation ///////////////////////////////

  protected void createJndiBindings() {

    final PlatformServiceReferenceFactory managedReferenceFactory = new PlatformServiceReferenceFactory(this);

    final ServiceName processEngineServiceBindingServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME
      .append(BpmPlatform.APP_JNDI_NAME)
      .append(BpmPlatform.MODULE_JNDI_NAME)
      .append(BpmPlatform.PROCESS_ENGINE_SERVICE_NAME);

    // bind process engine service
    BindingUtil.createJndiBindings(childTarget, processEngineServiceBindingServiceName, BpmPlatform.PROCESS_ENGINE_SERVICE_JNDI_NAME, managedReferenceFactory);

    final ServiceName processApplicationServiceBindingServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME
        .append(BpmPlatform.APP_JNDI_NAME)
        .append(BpmPlatform.MODULE_JNDI_NAME)
        .append(BpmPlatform.PROCESS_APPLICATION_SERVICE_NAME);

    // bind process application service
    BindingUtil.createJndiBindings(childTarget, processApplicationServiceBindingServiceName, BpmPlatform.PROCESS_APPLICATION_SERVICE_JNDI_NAME, managedReferenceFactory);

  }

  protected ProcessEngine getProcessEngineService(ServiceName processEngineServiceName) {
    try {
      ServiceController<ProcessEngine> serviceController = getProcessEngineServiceController(processEngineServiceName);
      return serviceController.getValue();
    } catch (ServiceNotFoundException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  protected ServiceController<ProcessEngine> getProcessEngineServiceController(ServiceName processEngineServiceName) {
    return (ServiceController<ProcessEngine>) serviceContainer.getRequiredService(processEngineServiceName);
  }

  protected void startTrackingServices() {
    processEngineServiceTracker = new ServiceTracker<>(ServiceNames.forManagedProcessEngines(), processEngines);
    serviceContainer.addListener(processEngineServiceTracker);

    processApplicationServiceTracker = new ServiceTracker<>(ServiceNames.forManagedProcessApplications(), processApplications);
    serviceContainer.addListener(processApplicationServiceTracker);
  }

  protected void stopTrackingServices() {
    serviceContainer.removeListener(processEngineServiceTracker);
    serviceContainer.removeListener(processApplicationServiceTracker);
  }

  /**
   * <p>invoked by the {@link MscManagedProcessEngine} and {@link MscManagedProcessEngineController}
   * when a process engine is started</p>
   */
  public void processEngineStarted(ProcessEngine processEngine) {
    processEngines.add(processEngine);
  }

  /**
   * <p>invoked by the {@link MscManagedProcessEngine} and {@link MscManagedProcessEngineController}
   * when a process engine is stopped</p>
   */
  public void processEngineStopped(ProcessEngine processEngine) {
    processEngines.remove(processEngine);
  }

  @SuppressWarnings("unchecked")
  protected MscManagedProcessApplication getManagedProcessApplication(String name) {
    ServiceController<MscManagedProcessApplication> serviceController = (ServiceController<MscManagedProcessApplication>) serviceContainer
        .getService(ServiceNames.forManagedProcessApplication(name));

    if (serviceController != null) {
      return serviceController.getValue();
    }
    else {
      return null;
    }
  }

}
