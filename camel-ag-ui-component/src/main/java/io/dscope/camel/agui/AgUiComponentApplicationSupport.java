package io.dscope.camel.agui;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.camel.Processor;
import org.apache.camel.main.Main;

import io.dscope.camel.agui.bridge.AgUiTaskEventBridge;
import io.dscope.camel.agui.bridge.AgUiToolEventBridge;
import io.dscope.camel.agui.bridge.NoopAgUiTaskEventBridge;
import io.dscope.camel.agui.bridge.NoopAgUiToolEventBridge;
import io.dscope.camel.agui.config.AgUiProtocolMethods;
import io.dscope.camel.agui.model.AgUiMethodCatalog;
import io.dscope.camel.agui.processor.AgUiAgentEnvelopeProcessor;
import io.dscope.camel.agui.processor.AgUiAgentRequestProcessor;
import io.dscope.camel.agui.processor.AgUiDiagnosticsProcessor;
import io.dscope.camel.agui.processor.AgUiErrorProcessor;
import io.dscope.camel.agui.processor.AgUiHealthMethodProcessor;
import io.dscope.camel.agui.processor.AgUiHealthProcessor;
import io.dscope.camel.agui.processor.AgUiInterruptProcessor;
import io.dscope.camel.agui.processor.AgUiJsonRpcEnvelopeProcessor;
import io.dscope.camel.agui.processor.AgUiMethodDispatchProcessor;
import io.dscope.camel.agui.processor.AgUiPostSseBridgeProcessor;
import io.dscope.camel.agui.processor.AgUiResumeProcessor;
import io.dscope.camel.agui.processor.AgUiRunFinishProcessor;
import io.dscope.camel.agui.processor.AgUiRunStartProcessor;
import io.dscope.camel.agui.processor.AgUiRunTextProcessor;
import io.dscope.camel.agui.processor.AgUiSseProcessor;
import io.dscope.camel.agui.processor.AgUiStateUpdateProcessor;
import io.dscope.camel.agui.processor.AgUiToolCallProcessor;
import io.dscope.camel.agui.service.AgUiEventCodec;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import io.dscope.camel.agui.service.AgUiStateDeltaCalculator;
import io.dscope.camel.agui.service.AgUiStateStore;
import io.dscope.camel.agui.service.InMemoryAgUiSessionRegistry;
import io.dscope.camel.agui.service.InMemoryAgUiStateStore;
import io.dscope.camel.agui.service.JacksonAgUiEventCodec;
import io.dscope.camel.agui.service.PersistentAgUiSessionRegistry;
import io.dscope.camel.agui.service.PersistentAgUiStateStore;
import io.dscope.camel.persistence.core.FlowStateStore;
import io.dscope.camel.persistence.core.FlowStateStoreFactory;
import io.dscope.camel.persistence.core.PersistenceConfiguration;

public class AgUiComponentApplicationSupport {

    public static final String BEAN_ENVELOPE_PROCESSOR = "agUiJsonRpcEnvelopeProcessor";
    public static final String BEAN_ERROR_PROCESSOR = "agUiErrorProcessor";
    public static final String BEAN_METHOD_DISPATCH_PROCESSOR = "agUiMethodDispatchProcessor";
    public static final String BEAN_SSE_PROCESSOR = "agUiSseProcessor";
    public static final String BEAN_POST_SSE_BRIDGE_PROCESSOR = "agUiPostSseBridgeProcessor";
    public static final String BEAN_HEALTH_PROCESSOR = "agUiHealthProcessor";
    public static final String BEAN_DIAGNOSTICS_PROCESSOR = "agUiDiagnosticsProcessor";
    public static final String BEAN_SESSION_REGISTRY = "agUiSessionRegistry";
    public static final String BEAN_STATE_STORE = "agUiStateStore";
    public static final String BEAN_METHOD_CATALOG = "agUiMethodCatalog";
    public static final String BEAN_TOOL_EVENT_BRIDGE = "agUiToolEventBridge";
    public static final String BEAN_TASK_EVENT_BRIDGE = "agUiTaskEventBridge";
    public static final String BEAN_AGENT_ENVELOPE_PROCESSOR = "agUiAgentEnvelopeProcessor";
    public static final String BEAN_AGENT_REQUEST_PROCESSOR = "agUiAgentRequestProcessor";
    public static final String BEAN_AGENT_PRE_RUN_TEXT_PROCESSOR = "agUiAgentPreRunTextProcessor";

    @FunctionalInterface
    public interface BeanBinder {
        void bind(String name, Object bean);
    }

    public Main createMain(String routeIncludePattern) {
        return createMain(routeIncludePattern, null);
    }

    public Main createMain(String routeIncludePattern, Consumer<Main> customizer) {
        if (routeIncludePattern == null || routeIncludePattern.isBlank()) {
            throw new IllegalArgumentException("Route include pattern must not be blank");
        }

        Main main = new Main();
        bindDefaultBeans(main::bind);
        if (customizer != null) {
            customizer.accept(main);
        }
        main.configure().withRoutesIncludePattern(routeIncludePattern);
        return main;
    }

    public void bindDefaultBeans(BeanBinder binder) {
        Objects.requireNonNull(binder, "binder must not be null");

        PersistenceConfiguration persistenceConfig = PersistenceConfiguration.fromProperties(systemProperties());

        AgUiEventCodec eventCodec = new JacksonAgUiEventCodec();
        AgUiSessionRegistry sessionRegistry;
        AgUiStateStore stateStore;
        if (persistenceConfig.enabled()) {
            FlowStateStore flowStateStore = FlowStateStoreFactory.create(persistenceConfig);
            sessionRegistry = new PersistentAgUiSessionRegistry(eventCodec, flowStateStore, persistenceConfig.rehydrationPolicy());
            stateStore = new PersistentAgUiStateStore(flowStateStore);
        } else {
            sessionRegistry = new InMemoryAgUiSessionRegistry(eventCodec);
            stateStore = new InMemoryAgUiStateStore();
        }

        AgUiStateDeltaCalculator stateDeltaCalculator = new AgUiStateDeltaCalculator();
        AgUiMethodCatalog methodCatalog = new AgUiMethodCatalog();
        AgUiToolEventBridge toolEventBridge = new NoopAgUiToolEventBridge();
        AgUiTaskEventBridge taskEventBridge = new NoopAgUiTaskEventBridge();

        Map<String, Processor> methods = Map.of(
            AgUiProtocolMethods.RUN_START, new AgUiRunStartProcessor(sessionRegistry),
            AgUiProtocolMethods.RUN_TEXT, new AgUiRunTextProcessor(sessionRegistry),
            AgUiProtocolMethods.TOOL_CALL, new AgUiToolCallProcessor(sessionRegistry, toolEventBridge),
            AgUiProtocolMethods.STATE_UPDATE, new AgUiStateUpdateProcessor(sessionRegistry, stateStore, stateDeltaCalculator),
            AgUiProtocolMethods.INTERRUPT, new AgUiInterruptProcessor(sessionRegistry),
            AgUiProtocolMethods.RESUME, new AgUiResumeProcessor(sessionRegistry),
            AgUiProtocolMethods.RUN_FINISH, new AgUiRunFinishProcessor(sessionRegistry),
            AgUiProtocolMethods.HEALTH, new AgUiHealthMethodProcessor()
        );

        binder.bind(BEAN_SESSION_REGISTRY, sessionRegistry);
        binder.bind(BEAN_STATE_STORE, stateStore);
        binder.bind(BEAN_METHOD_CATALOG, methodCatalog);
        binder.bind(BEAN_TOOL_EVENT_BRIDGE, toolEventBridge);
        binder.bind(BEAN_TASK_EVENT_BRIDGE, taskEventBridge);
        binder.bind(BEAN_ENVELOPE_PROCESSOR, new AgUiJsonRpcEnvelopeProcessor());
        binder.bind(BEAN_AGENT_ENVELOPE_PROCESSOR, new AgUiAgentEnvelopeProcessor());
        binder.bind(BEAN_ERROR_PROCESSOR, new AgUiErrorProcessor());
        binder.bind(BEAN_METHOD_DISPATCH_PROCESSOR, new AgUiMethodDispatchProcessor(methods));
        binder.bind(BEAN_AGENT_REQUEST_PROCESSOR, new AgUiAgentRequestProcessor());
        binder.bind(BEAN_POST_SSE_BRIDGE_PROCESSOR, new AgUiPostSseBridgeProcessor());
        binder.bind(BEAN_SSE_PROCESSOR, new AgUiSseProcessor(sessionRegistry));
        binder.bind(BEAN_HEALTH_PROCESSOR, new AgUiHealthProcessor());
        binder.bind(BEAN_DIAGNOSTICS_PROCESSOR, new AgUiDiagnosticsProcessor(sessionRegistry, methodCatalog));
    }

    private Properties systemProperties() {
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        return properties;
    }
}
