/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the New BSD License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.gondola.container;

import com.yahoo.gondola.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Gondola admin client.
 */
public class GondolaAdminClient {

    public static final String API_SET_LEADER = "/api/gondola/v1/local/setLeader";
    public static final String API_GONDOLA_STATUS = "/api/gondola/v1/local/gondolaStatus";
    public static final String API_INSPECT_REQUEST_URI = "/api/gondola/v1/local/inspectRequestUri";
    public static final String API_ENABLE = "/api/gondola/v1/local/enable";
    Config config;
    Client client = ClientBuilder.newClient();
    Logger logger = LoggerFactory.getLogger(GondolaAdminClient.class);

    public GondolaAdminClient(Config config) {
        this.config = config;
    }

    public Map setLeader(String hostId, String shardId) {
        String appUri = Utils.getAppUri(config, hostId);
        WebTarget target = client.target(appUri)
            .path(API_SET_LEADER)
            .queryParam("shardId", shardId);
        return target.request().post(null, Map.class);
    }

    public Map enable(String hostId, String shardId, boolean enable) {
        String appUri = Utils.getAppUri(config, hostId);
        WebTarget target = client.target(appUri)
            .path(API_ENABLE)
            .queryParam("shardId", shardId)
            .queryParam("enable", enable);
        return target.request().post(null, Map.class);
    }

    public Map getHostStatus(String hostId) {
        try {
            return getHostStatusAsync(hostId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Future<Map> getHostStatusAsync(String hostId) {
        String appUri = Utils.getAppUri(config, hostId);
        WebTarget target = client.target(appUri).path(API_GONDOLA_STATUS);
        return target.request(MediaType.APPLICATION_JSON_TYPE).async().get(Map.class);
    }


    public Map<String, Object> getServiceStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        config.getHostIds().stream()
            .collect(Collectors.toMap(
                hostId -> hostId,
                this::getHostStatusAsync))
            .forEach((hostId, mapFuture) -> {
                try {
                    map.put(hostId, mapFuture.get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.warn("Cannot get serviceStatus for hostId={}", hostId);
                    map.put(hostId, null);
                }
            });
        return map;
    }

    public Map inspectRequestUri(String uri, String hostId) {
        String appUri = Utils.getAppUri(config, hostId);
        return client.target(appUri).path(API_INSPECT_REQUEST_URI)
            .queryParam("requestUri", uri).request().get(Map.class);
    }
}
