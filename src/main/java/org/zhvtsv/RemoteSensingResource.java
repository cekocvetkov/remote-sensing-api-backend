package org.zhvtsv;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.zhvtsv.models.ExtentRequest;
import org.zhvtsv.models.MultipartBody;
import org.zhvtsv.service.ObjectDetection;
import org.zhvtsv.service.TreeDetection;
import org.zhvtsv.service.TreeDetectionDeepforest;
import org.zhvtsv.service.stac.GeoTiffService;
import org.zhvtsv.service.stac.STACClient;
import org.zhvtsv.service.stac.dto.STACItemPreview;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.zhvtsv.utils.ImageUtils.readImageFromInputStream;

@Path("/api/v1/remote-sensing/stac")
public class RemoteSensingResource {
    private static final Logger LOG = Logger.getLogger(RemoteSensingResource.class);
    @Inject
    STACClient stacClient;
    @Inject
    GeoTiffService geoTiffService;
    @Inject
    ObjectDetection yolovObjectDetection;
    @Inject
    TreeDetection treeDetection;
    @Inject
    TreeDetectionDeepforest treeDetectionDeepforest;

    private static String getBoundingBoxString(double[] extent) {
        String array = Arrays.toString(extent);
        return array.substring(1, array.length() - 2).replaceAll("\\s+", "");
    }

    @POST
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItems(ExtentRequest extentRequest) {
        LOG.info("Get items request for extent " + Arrays.toString(extentRequest.getExtent()));
        List<STACItemPreview> stacItemPreviewList = this.stacClient.getStacItems(getBoundingBoxString(extentRequest.getExtent()));
        LOG.info("Found " + stacItemPreviewList.size() + " items.");
        return Response.ok(stacItemPreviewList).build();
    }

    @POST
    @Path("/detection")
    @Produces({"image/png", "image/jpg", "image/tiff"})
    public Response objectDetection(ExtentRequest extentRequest) {
        String id = extentRequest.getId();
        String model = extentRequest.getModel();
        double[] extent = extentRequest.getExtent();

        LOG.info("Load GeoTiff for item with id " + id + " and extent " + Arrays.toString(extent));
        STACItemPreview stacItemPreview = this.stacClient.getStacItemById(id);
        Mat image = this.geoTiffService.downloadStacItemGeoTiffRGB(stacItemPreview.getDownloadUrl(), extent);
        Mat res = new Mat();

        if (model.endsWith("object-detection")) {
            res = yolovObjectDetection.detectObjectOnImage(image, model);
        } else if (model.endsWith("tree-detection")) {
            res = treeDetection.detectObjectOnImage(image, extentRequest.getModel());
        } else {
            return Response.ok(treeDetectionDeepforest.detectObjectOnImage(image, extentRequest.getModel())).build();
        }
        BufferedImage response = mat2BufferedImage(res);
        return Response.ok(response).build();
    }

    @POST
    @Path("/bing/detection")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({"image/png", "image/jpg"})
    public Response objectDetection(@MultipartForm MultipartBody data) {
        LOG.info("Bing detection with model " + data.model);
        String model = data.model;
        Mat res = new Mat();
        if (model.endsWith("tree-detection")) {
            res = treeDetection.detectObjectOnImage(readImageFromInputStream(data.file), data.model);
        } else if (model.endsWith("object-detection")) {
            res = yolovObjectDetection.detectObjectOnImage(readImageFromInputStream(data.file), data.model);
        } else {
            return Response.ok(treeDetectionDeepforest.detectObjectOnImage(res, model)).build();

        }
        BufferedImage image = mat2BufferedImage(res);

        return Response.ok(image).build();
    }

    public BufferedImage mat2BufferedImage(Mat mat) {
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
