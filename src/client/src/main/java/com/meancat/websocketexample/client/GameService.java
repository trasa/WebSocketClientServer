package com.meancat.websocketexample.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.meancat.websocketexample.client.messages.Message;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;

public class GameService {


    private static class ApplicationArguments {
        @Option(required = true, name = "--home", aliases = {"-h"}, usage = "Home directory of application.")
        public String home;
    }

    public static void main(String[] args) throws InterruptedException, WebSocketException {
        ApplicationArguments appArgs = new ApplicationArguments();
        CmdLineParser parser = new CmdLineParser(appArgs);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println();
            parser.printUsage(System.err);
            return;
        }
        System.setProperty("app.home", appArgs.home);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(new File(SystemPropertyUtils.resolvePlaceholders("${app.home}/conf/logback.xml")));
        } catch (JoranException je) {
            // ignore since StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);

        System.out.println("Starting game service");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(GameServiceConfiguration.class);
        System.out.println("Started game service");

        // ok, lets get our connection to the services running:
        System.out.println("Attempting connection to service");
        WebSocketClient client = applicationContext.getBean(WebSocketClient.class);
        client.connect();
        System.out.println("Connected to " + client.getConnectedUrl());

        // websocket ping
        client.sendPingWebSocketFrame();
        // sending a message which will be echo'd back to us:
        client.send(new Message("Hello, World!"));

        while (applicationContext.isActive()) {
            Thread.sleep(5000);
        }
        System.out.println("Stopped game service");
    }
}
