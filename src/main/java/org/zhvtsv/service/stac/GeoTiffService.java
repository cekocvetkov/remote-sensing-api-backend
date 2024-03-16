package org.zhvtsv.service.stac;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.jboss.logging.Logger;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.zhvtsv.utils.PathUtils;

import java.awt.image.DataBufferByte;
import java.io.IOException;

@ApplicationScoped
public class GeoTiffService {
    private static final Logger LOG = Logger.getLogger(GeoTiffService.class);

    private final CropImageService cropImageService;

    public GeoTiffService(CropImageService cropImageService) {
        this.cropImageService = cropImageService;
    }

    public Mat downloadStacItemGeoTiffRGB(String href, double[] extent) {

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

            Mat image = convertToMat(cropped);
            Imgcodecs.imwrite(PathUtils.getPathForImageInResources("new.tif"), image);

            reader.dispose();

            return image;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Mat convertToMat(GridCoverage2D coverage) {
        int numBands = coverage.getNumSampleDimensions();
        byte[] imageBytes = ((DataBufferByte) coverage.getRenderedImage().getData().getDataBuffer()).getData();

        // Convert byte array to Mat object
        Mat mat = new Mat(coverage.getRenderedImage().getHeight(), coverage.getRenderedImage().getWidth(), numBands == 4 ? CvType.CV_8UC4 : CvType.CV_8UC3);
        mat.put(0, 0, imageBytes);
        if (numBands == 3 || numBands == 4) {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            mat = rgbMat;
        }
        return mat;
    }
}
