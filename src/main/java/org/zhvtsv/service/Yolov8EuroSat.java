package org.zhvtsv.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.zhvtsv.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Uses a pre-trained model used the EuroSat dataset - https://github.com/phelber/EuroSAT
 */
@ApplicationScoped
public class Yolov8EuroSat
{
    private static final String MODEL_PATH = "yolov8EuroSat.onnx";
    private static final String CLASSES_PATH = "yolov8EuroSatClasses.txt";

    public String predictClass(Mat geoTiffImage){

        // read and process the input image
        Mat inputBlob = preProcessImageForYolov8EuroSat(geoTiffImage);

        // read generated ONNX model into org.opencv.dnn.Net object
        Net dnnNet = Dnn.readNetFromONNX(PathUtils.getPathForImageInResources( MODEL_PATH ));
        System.out.println("DNN from ONNX was successfully loaded!");
    
        // set OpenCV model input
        dnnNet.setInput(inputBlob);
        
        // provide inference
        Mat classification = dnnNet.forward();
    
        var euroSatImageClasses = new ArrayList<String>();
    
        try {
            euroSatImageClasses = getClasses();
            System.out.println(euroSatImageClasses);
        } catch (IOException ex) {
            System.out.printf("Could not read %s file:%n", CLASSES_PATH );
            ex.printStackTrace();
            //TODO: TO THROW REST EXCEPTION
        }
        
        // obtain max prediction result
        Core.MinMaxLocResult mm = Core.minMaxLoc(classification);
        double maxValIndex = mm.maxLoc.x;
        System.out.println(maxValIndex);
        var predictedClass = euroSatImageClasses.get((int) maxValIndex);
        System.out.println("Predicted Class: " + predictedClass);
        return predictedClass;
    }
    private Mat preProcessImageForYolov8EuroSat(Mat imageToPreprocess) {
        // resize input image (model was trained on images with size 64x64px )
//        Mat rgbMat = new Mat(imageToPreprocess.rows(), imageToPreprocess.cols(), imageToPreprocess.type());
//        Imgproc.cvtColor(imageToPreprocess, rgbMat, Imgproc.COLOR_BGR2RGB);
//        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "new.tif" ), rgbMat);

        Mat resizedImage = new Mat();
        Imgproc.resize(imageToPreprocess, resizedImage, new Size(64, 64), Imgproc.INTER_AREA);
        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "resized.tif" ), resizedImage);
        
        // prepare DNN input
        Mat blob = Dnn.blobFromImage(resizedImage, 1.0/255.0, new Size(64, 64), // Here we supply the spatial size that the Convolutional Neural Network expects.
                new Scalar(new double[]{0.0, 0.0, 0.0}), true, false);

        return blob;
    }
    
    private ArrayList<String> getClasses() throws IOException
    {
        ArrayList<String> imgLabels;
        try (Stream<String> lines = Files.lines( Path.of( PathUtils.getPathForImageInResources( CLASSES_PATH ) ) )) {
            imgLabels = lines.collect( Collectors.toCollection(ArrayList::new));
        }
        return imgLabels;
    }




}
