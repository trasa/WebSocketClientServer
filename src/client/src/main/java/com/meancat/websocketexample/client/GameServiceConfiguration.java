package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.jetty.websocket.*;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.SystemPropertyUtils;

@Configuration
@EnableScheduling
@ComponentScan(basePackageClasses = GameServiceConfiguration.class)
public class GameServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceConfiguration.class);

    @Value("${service.url}")
    private String serviceUrl;

    @Bean
    public static PropertyPlaceholderConfigurer propertyConfigurer() {
        logger.debug("reading properties...");
        PropertyPlaceholderConfigurer bean = new PropertyPlaceholderConfigurer();
        bean.setLocation(new DefaultResourceLoader().getResource(SystemPropertyUtils.resolvePlaceholders("file:${app.home}/conf/config.properties")));
        bean.setSearchSystemEnvironment(true);
        bean.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        return bean;
    }

    @Bean
    public ObjectMapper jsonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        objectMapper.disableDefaultTyping();
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        return objectMapper;
    }

    // jetty websocketclient

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WebSocketClientFactory webSocketClientFactory() {
        WebSocketClientFactory bean = new WebSocketClientFactory();
        // set other interesting parameters here
        return bean;
    }

    @Bean
    public WebSocketClient webSocketClient(WebSocketClientFactory webSocketClientFactory) {
        org.eclipse.jetty.websocket.WebSocketClient bean = webSocketClientFactory.newWebSocketClient();
        // other parameters for the websocketclient
        return bean;
    }
}
