package org.zhvtsv.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class TreeDetectionDeepforest {

    public byte[] detectObjectOnImage(Mat image) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".tif", image, matOfByte);
        byte[] img = matOfByte.toArray();
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/deepforest"))
                    .header("Content-Type", "multipart/form-data")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(img))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpResponse<byte[]> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Response code from deepforest microservice: " + response.statusCode());

        return response.body();
    }

}
