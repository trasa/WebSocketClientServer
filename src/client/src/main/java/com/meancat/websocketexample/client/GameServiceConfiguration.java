package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meancat.websocketexample.client.messages.MessageBody;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;

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
    public ObjectMapper jsonObjectMapper(Reflections reflections) {
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        objectMapper.disableDefaultTyping();
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);

        // register the @MessageBody classes
        for(Class<?> clazz : reflections.getTypesAnnotatedWith(MessageBody.class)) {
            logger.debug("Registering MessageBody {}", clazz.getSimpleName());
            objectMapper.registerSubtypes(clazz);
        }

        return objectMapper;
    }

    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
            .filterInputsBy(new FilterBuilder()
                .include(FilterBuilder.prefix("com.meancat.websocketexample.client")))
            .setUrls(ClasspathHelper.forPackage("com.meancat.websocketexample.client"))
            .setScanners(new SubTypesScanner(),
                    new TypeAnnotationsScanner(),
                    new ResourcesScanner())
        );
    }

    // set up some netty code:
    @Bean
    public ChannelFactory socketChannelFactory() {
        return new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }

    @Bean
    WebSocketClientHandshaker clientHandshaker() throws URISyntaxException {
        logger.debug("Creating client handshaker to {}", serviceUrl);
        URI url = new URI(serviceUrl);
        return new WebSocketClientHandshakerFactory().newHandshaker(url, WebSocketVersion.V13, null, false, null);
    }

    @Bean
    public ChannelPipelineFactory socketChannelPipelineFactory(final WebSocketClientSerDe serDe, final WebSocketClientHandler webSocketClientHandler) {
        return new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();

                pipeline.addLast("decoder", new HttpResponseDecoder());
                pipeline.addLast("encoder", new HttpRequestEncoder());
                // this part of the pipeline translates json into objects and back.
                pipeline.addLast("serialierDeserializer", serDe);
                // this part of the pipeline handles the resulting objects to go "do stuff"
                pipeline.addLast("ws-handler", webSocketClientHandler);

                return pipeline;
            }
        };
    }
}
