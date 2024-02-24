package org.zhvtsv.service.stac;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.jboss.logging.Logger;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.zhvtsv.service.Yolov8EuroSat;
import org.zhvtsv.utils.PathUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class GeoTiffService {
    private static final Logger LOG = Logger.getLogger(GeoTiffService.class);

    private final STACConfig stacConfig;
    private final CropImageService cropImageService;
    private final Yolov8EuroSat yolov8EuroSatService;

    public GeoTiffService(STACConfig stacConfig, CropImageService cropImageService, Yolov8EuroSat yolov8EuroSat){
        this.stacConfig = stacConfig;
        this.cropImageService = cropImageService;
        this.yolov8EuroSatService = yolov8EuroSat;
    }
    public Mat downloadStacItemGeoTiffRGB(String href, double [] extent) {

        try {
            BasicAuthURI cogUri = new BasicAuthURI(href, false);
            HttpRangeReader rangeReader =
                    new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);
            CogSourceSPIProvider input =
                    new CogSourceSPIProvider(
                            cogUri,
                            new CogImageReaderSpi(),
                            new CogImageInputStreamSpi(),
                            rangeReader.getClass().getName());

            GeoTiffReader reader = new GeoTiffReader(input);

            GridCoverage2D coverage = reader.read(null);
            LOG.info("Reading Geotiff file successful.");
            GridCoverage2D cropped = cropImageService.cropGeoTiff(coverage, extent);
            LOG.info("GeoTiff cropped");

            byte [] imageByteArray = coverageToBinary(cropped);
            Mat image = convertToMat(cropped, imageByteArray);
            Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "new.tif" ), image);

            reader.dispose();

            // Get BufferedImage directly from GridCoverage2D

            // Convert BufferedImage to Mat (assuming BGR channels)

            return image;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] downloadStacItemGeoTiffRGBBytes(String href, double [] extent) {

        try {
            BasicAuthURI cogUri = new BasicAuthURI(href, false);
            HttpRangeReader rangeReader =
                    new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);
            CogSourceSPIProvider input =
                    new CogSourceSPIProvider(
                            cogUri,
                            new CogImageReaderSpi(),
                            new CogImageInputStreamSpi(),
                            rangeReader.getClass().getName());

            GeoTiffReader reader = new GeoTiffReader(input);

            GridCoverage2D coverage = reader.read(null);
            LOG.info("Reading Geotiff file successful.");
            GridCoverage2D cropped = cropImageService.cropGeoTiff(coverage, extent);
            LOG.info("GeoTiff cropped");

            byte [] imageByteArray = coverageToBinary(cropped);
//            Mat image = convertToMat(cropped, imageByteArray);
//            Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "new.tif" ), image);

            reader.dispose();

            // Get BufferedImage directly from GridCoverage2D

            // Convert BufferedImage to Mat (assuming BGR channels)

            return imageByteArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    public byte[] downloadStacItemGeoTiffRGB(String href, double [] extent){
//        GeoTiffReader reader = null;
//        try {
//            reader = new GeoTiffReader(href);
//            GridCoverage2D coverage = reader.read(null);
//
//            LOG.info("Reading Geotiff file successful.");
//            GridCoverage2D cropped = cropImageService.cropGeoTiff(coverage, extent);
//            LOG.info("GeoTiff cropped");
//
//            byte [] imageByteArray = coverageToBinary(cropped);
//            yolov8EuroSatService.predictClass(convertToMat(cropped,imageByteArray));
//            return imageByteArray;
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
    private byte[] coverageToBinary(GridCoverage2D coverage2D)  {
        long startTime = System.currentTimeMillis();
        LOG.info("Start converting to binary");

        var stream = new ByteArrayOutputStream();
        GeoTiffWriter writer = null;
        try {
            writer = new GeoTiffWriter(stream);
            writer.write(coverage2D, null);
        } catch (IOException e) {
            throw new RuntimeException("Could not export geotiff to blob", e);
        } finally {
            if (writer != null){
                writer.dispose();
            }
        }
//        try {
//            Files.write(Path.of("/Users/zezko/Documents/bakk/sentinel-processing-api/src/main/resources/zezko.tif"), stream.toByteArray());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        LOG.info("Converted");
        // Record end time
        long endTime = System.currentTimeMillis();

        // Calculate and print the elapsed time
        long elapsedTime = endTime - startTime;
        LOG.info("Elapsed Time: " + elapsedTime/1000.0 + " seconds");

        return stream.toByteArray();
    }

    private Mat convertToMat(GridCoverage2D coverage, byte[] data) {

        int numBands = coverage.getNumSampleDimensions();
        byte[] imageBytes = ((DataBufferByte) coverage.getRenderedImage().getData().getDataBuffer()).getData();

        // Convert byte array to Mat object
        Mat mat = new Mat(coverage.getRenderedImage().getHeight(), coverage.getRenderedImage().getWidth(), numBands==4 ? CvType.CV_8UC4 : CvType.CV_8UC3);
        mat.put(0, 0, imageBytes);
        if (numBands == 3 || numBands ==4) {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat,rgbMat, Imgproc.COLOR_BGR2RGB);
            mat = rgbMat;
        }
        return mat;
    }
}
