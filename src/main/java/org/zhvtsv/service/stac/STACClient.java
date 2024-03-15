package org.zhvtsv.service.stac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import org.zhvtsv.service.stac.dto.STACItemPreview;
import org.zhvtsv.service.stac.dto.STACObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class STACClient {
    private static final Logger LOG = Logger.getLogger(STACClient.class);
    private final HttpClient httpClient;
    @ConfigProperty(name = "stac-urls")
    private List<String> urls;

    public STACClient() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    public List<STACItemPreview> getStacItems(String boundingBox) {
        // Make get items request for all the stac sources we have defined in the properties file
        LOG.info("Make stac items request for the following sources: " + urls);
        List<String> responseBodys = new ArrayList<>();
        for (String url : urls) {
            String responseBody = makeGetRequest(url + "?bbox=" + boundingBox + "&timerange=2011-12-01/2020-12-31&eo:cloud_cover=10");
            responseBodys.add(responseBody);
        }
        return STACObjectMapper.getStacItemsPreview(responseBodys);
    }

    public STACItemPreview getStacItemById(String id) {
        for (String url : urls) {
            try {
                String responseBody = makeGetRequest(url + "/" + id);
                return STACObjectMapper.getStacItemPreview(new JSONObject(responseBody));
            } catch (ServerErrorException ex) {
                LOG.info("Id not found for " + url);
            }
        }
        return null;
    }

    private String makeGetRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new ServerErrorException("Gateway http request failed with status code: " + response.statusCode(), Response.Status.BAD_GATEWAY);
            }
        } catch (IOException | InterruptedException e) {
            throw new ServerErrorException("STAC Request failed", Response.Status.BAD_GATEWAY, e);
        }

    }
}
