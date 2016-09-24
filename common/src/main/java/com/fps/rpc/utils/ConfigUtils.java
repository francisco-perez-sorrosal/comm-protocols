package com.fps.rpc.utils;

import com.beust.jcommander.*;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ConfigUtils {

    public static void parseCommandArgs(String args[], Object settings) {

        JCommander commander = new JCommander(settings);
        try {
            commander.parse(args);
        } catch (ParameterException ex) {
            commander.usage();
            throw new IllegalArgumentException(ex.getMessage());
        }

    }

    public static String parseCommandArgs(String args[], Map<String, Object> commands) {

        JCommander commander = new JCommander();

        commands.forEach((key, value) -> {
            commander.addCommand(key, value);
        });

        try {
            commander.parse(args);
        } catch (ParameterException ex) {
            commander.usage();
            throw new IllegalArgumentException(ex.getMessage());
        }

        return commander.getParsedCommand();

    }


    public static class HostAndPortConverter implements IStringConverter<HostAndPort> {

        @Override
        public HostAndPort convert(String value) {
            return HostAndPort.fromString(value);
        }

    }
    public static class RequestTypeConverter implements IStringConverter<RequestType> {

        @Override
        public RequestType convert(String value) {
            return RequestType.fromString(value);
        }

    }

    public enum RequestType {

        SYNC, ASYNC;

        private static final Logger LOG = LoggerFactory.getLogger(RequestType.class);

        public static RequestType fromString(String code) {

            for(RequestType output : RequestType.values()) {
                if(output.toString().equalsIgnoreCase(code)) {
                    return output;
                }
            }

            LOG.warn("Request type {} not valid. Allowed values [SYNC (Default) | ASYNC]. Using default...");
            return SYNC;
        }
    }

}