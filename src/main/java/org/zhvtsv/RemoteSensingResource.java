package org.zhvtsv;


import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.zhvtsv.models.ExtentRequest;
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
    @POST
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItems(ExtentRequest extentRequest) {
        LOG.info("Get items request for extent "+ Arrays.toString(extentRequest.getExtent()));
        List<STACItemPreview> stacItemPreviewList = this.stacClient.getStacItems(getBoundingBoxString(extentRequest.getExtent()));
        LOG.info("Found "+stacItemPreviewList.size()+" items.");
        return Response.ok(stacItemPreviewList).build();
    }

    @POST
    @Path("/object-detection")
    @Produces({"image/png", "image/jpg", "image/tiff"})
    public Response objectDetection(ExtentRequest extentRequest) {
        LOG.info("Load GeoTiff for item with id "+ extentRequest.getId() + " and extent "+Arrays.toString(extentRequest.getExtent()));
        STACItemPreview stacItemPreview = this.stacClient.getStacItemById(extentRequest.getId());
        Mat image = this.geoTiffService.downloadStacItemGeoTiffRGB(stacItemPreview.getDownloadUrl(), extentRequest.getExtent());
        Mat res = yolovObjectDetection.detectObjectOnImage(image, extentRequest.getModel());
        BufferedImage response = mat2BufferedImage(res);
        return Response.ok(response).build();
    }

    @POST
    @Path("/tree-detection")
    @Produces({"image/png", "image/jpg", "image/tiff"})
    public Response treeDetectionYolo(ExtentRequest extentRequest) {
        LOG.info("Load GeoTiff for item with id "+ extentRequest.getId() + " and extent "+Arrays.toString(extentRequest.getExtent()));
        STACItemPreview stacItemPreview = this.stacClient.getStacItemById(extentRequest.getId());
        Mat image = this.geoTiffService.downloadStacItemGeoTiffRGB(stacItemPreview.getDownloadUrl(), extentRequest.getExtent());
        Mat res = treeDetection.detectObjectOnImage(image, extentRequest.getModel());
        BufferedImage response = mat2BufferedImage(res);
        return Response.ok(response).build();
    }
//    @POST
//    @Path("/tree-detection/deepforest")
//    @Produces({"image/png", "image/jpg", "image/tiff"})
//    public Response treeDetectionDeepforest(ExtentRequest extentRequest) {
//        LOG.info("Load GeoTiff for item with id "+ extentRequest.getId() + " and extent "+Arrays.toString(extentRequest.getExtent()));
//        STACItemPreview stacItemPreview = this.stacClient.getStacItemById(extentRequest.getId());
//        byte[] image = this.geoTiffService.downloadStacItemGeoTiffRGBBytes(stacItemPreview.getDownloadUrl(), extentRequest.getExtent());
//
//        return Response.ok(treeDetectionDeepforest.detectObjectOnImage(image)).build();
//    }
    private static String getBoundingBoxString(double[] extent) {
        String array = Arrays.toString(extent);
        return array.substring(1, array.length() - 2).replaceAll("\\s+", "");
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
