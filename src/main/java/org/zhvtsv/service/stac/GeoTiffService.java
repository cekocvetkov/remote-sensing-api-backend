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
import org.zhvtsv.service.Yolov8EuroSat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
            reader.dispose();

            // Get BufferedImage directly from GridCoverage2D

            // Convert BufferedImage to Mat (assuming BGR channels)
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageByteArray), Imgcodecs.IMREAD_COLOR); // Specify flags as needed


            return image;
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
//            Files.write(Path.of("/Users/zezko/Documents/bakk/tiny-stac/src/main/resources/zeko.tif"), stream.toByteArray());
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
        try {
            int numBands = coverage.getNumSampleDimensions();
            int width = coverage.getRenderedImage().getWidth();
            int height = coverage.getRenderedImage().getHeight();
            System.out.println("Number of bands of geotiff image: "+numBands);
            System.out.println(width +" x "+height);

            //IF  NOT PLANETARY COMPUTAR TAKE CV_8UC3
            Mat mat = new Mat(height, width, CvType.CV_8UC4);
            mat.put(0, 0, data);
            return mat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
