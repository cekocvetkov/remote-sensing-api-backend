package org.zhvtsv.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.opencv.core.Mat;
import org.opencv.core.Point;
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

@ApplicationScoped
public class Yolov8ObjectDetection {
    private static final String MODEL_PATH = "yolov8ObjectDetectionDior.onnx";
    private static final String CLASSES_PATH = "yolov8ObjectDetectionDiorClasses.txt";
    public void detect(Mat geoTiffImage) {
        // read and process the input image
        Mat inputBlob = preProcessImageForYolov8ObjectDetectionDior(geoTiffImage);
        // Load the ONNX model
        Net dnnNet = Dnn.readNetFromONNX(PathUtils.getPathForImageInResources( MODEL_PATH ));
        System.out.println("DNN from ONNX was successfully loaded!");

        // Continue with image processing and object detection...
//        Mat blob = Dnn.blobFromImage(image, 1.0, new Size(224, 224), new Scalar(104, 117, 123), false, false);
        dnnNet.setInput(inputBlob);

        Mat detections = dnnNet.forward();


        var diorClasses = new ArrayList<String>();

        try {
            diorClasses = getClasses();
            System.out.println(diorClasses);
        } catch (IOException ex) {
            System.out.printf("Could not read %s file:%n", CLASSES_PATH );
            ex.printStackTrace();
            //TODO: TO THROW REST EXCEPTION
        }

        processDetections(detections, inputBlob, diorClasses);


    }
    private Mat preProcessImageForYolov8ObjectDetectionDior(Mat imageToPreprocess) {
        Mat resizedImage = new Mat();
        Imgproc.resize(imageToPreprocess, resizedImage, new Size(800, 800), Imgproc.INTER_AREA);
        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "resizedod.tif" ), resizedImage);

        // prepare DNN input
        Mat blob = Dnn.blobFromImage(resizedImage, 1.0/255.0, new Size(800, 800), // Here we supply the spatial size that the Convolutional Neural Network expects.
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
    private void processDetections(Mat detections, Mat inputImage, ArrayList<String> diorClasses) {

    }
}