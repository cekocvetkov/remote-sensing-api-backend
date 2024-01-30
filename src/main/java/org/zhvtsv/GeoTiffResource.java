package org.zhvtsv;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.zhvtsv.service.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

@Path("/api/v1/sentinel")
public class GeoTiffResource {
    private static final Logger LOG = Logger.getLogger(GeoTiffResource.class);
    @Inject
    SentinelProcessApiClient sentinelProcessApiClient;
    @Inject
    Yolov8EuroSat yolov8EuroSat;
    @Inject
    ObjectDetection yolovObjectDetection;
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getGeoTiffSentinel(SentinelRequest sentinelRequest) {
        String boundingBox = getBoundingBoxString(sentinelRequest.getExtent());
        LOG.info("Request for GeoTiffs " + sentinelRequest);

        InputStream inputStream = sentinelProcessApiClient.getGeoTiff(sentinelRequest.getExtent(), sentinelRequest.getDateFrom() + "T00:00:00Z", sentinelRequest.getDateTo() + "T00:00:00Z", sentinelRequest.getCloudCoverage());

        return Response.ok(inputStream).header("Content-Type", "image/tiff").build();
    }
    @POST
    @Path("/classify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response classifyGeoTiff(SentinelRequest sentinelRequest) {
        String boundingBox = getBoundingBoxString(sentinelRequest.getExtent());
        LOG.info("Classify the geotiff from the request " + sentinelRequest);


        InputStream inputStream = sentinelProcessApiClient.getGeoTiff(sentinelRequest.getExtent(), sentinelRequest.getDateFrom() + "T00:00:00Z", sentinelRequest.getDateTo() + "T00:00:00Z", sentinelRequest.getCloudCoverage());

        return Response.ok(yolov8EuroSat.predictClass(readImageFromInputStream(inputStream))).build();
    }
    @POST
    @Path("/od")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({"image/png", "image/jpg"})

    public Response objectDetection(SentinelRequest sentinelRequest) {
        String boundingBox = getBoundingBoxString(sentinelRequest.getExtent());
        LOG.info("Object Detection for geotiff from the request " + sentinelRequest);

        InputStream inputStream = sentinelProcessApiClient.getGeoTiff(sentinelRequest.getExtent(), sentinelRequest.getDateFrom() + "T00:00:00Z", sentinelRequest.getDateTo() + "T00:00:00Z", sentinelRequest.getCloudCoverage());
        Mat res = yolovObjectDetection.detectObjectOnImage(readImageFromInputStream(inputStream));
        BufferedImage image = mat2BufferedImage(res);

        return Response.ok(image).build();
    }
    private Mat readImageFromInputStream(InputStream inputStream) {
        byte[] imageBytes = new byte[0];
        try {
            imageBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MatOfByte matOfByte = new MatOfByte(imageBytes);
        return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
    }
    private String getBoundingBoxString(double[] extent) {
        String array = Arrays.toString(extent);
        return array.substring(1, array.length() - 2).replaceAll("\\s+", "");
    }

    public BufferedImage mat2BufferedImage(Mat mat) {
        //Encoding the image
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, matOfByte);
        //Storing the encoded Mat in a byte array
        byte[] byteArray = matOfByte.toArray();
        //Preparing the Buffered Image
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage bufImage = null;
        try {
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bufImage;
    }
}
