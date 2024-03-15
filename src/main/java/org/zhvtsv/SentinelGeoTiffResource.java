package org.zhvtsv;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.zhvtsv.service.ObjectDetection;
import org.zhvtsv.service.TreeDetection;
import org.zhvtsv.service.TreeDetectionDeepforest;
import org.zhvtsv.service.sentinel.SentinelProcessApiClient;
import org.zhvtsv.service.sentinel.SentinelRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Path("/api/v1/sentinel")
public class SentinelGeoTiffResource {
    private static final Logger LOG = Logger.getLogger(SentinelGeoTiffResource.class);
    @Inject
    SentinelProcessApiClient sentinelProcessApiClient;
    @Inject
    ObjectDetection yolovObjectDetection;
    @Inject
    TreeDetection treeDetection;
    @Inject
    TreeDetectionDeepforest treeDetectionDeepforest;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({"image/png", "image/jpg", "image/tiff"})
    public Response getGeoTiffSentinel(SentinelRequest sentinelRequest) {
        String boundingBox = getBoundingBoxString(sentinelRequest.getExtent());
        LOG.info("Request for GeoTiffs " + sentinelRequest);

        InputStream inputStream = sentinelProcessApiClient.getGeoTiff(sentinelRequest.getExtent(), sentinelRequest.getDateFrom() + "T00:00:00Z", sentinelRequest.getDateTo() + "T00:00:00Z", sentinelRequest.getCloudCoverage());
        Mat res = yolovObjectDetection.detectObjectOnImage(readImageFromInputStream(inputStream), sentinelRequest.getModel());
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

    private BufferedImage mat2BufferedImage(Mat mat) {
        //Encoding the image
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
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
