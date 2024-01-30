package org.zhvtsv.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.zhvtsv.utils.PathUtils;
import java.util.Random;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ObjectDetectionOld {
    private static final String MODEL_PATH = "yolov8ObjectDetectionDior.onnx";
    private static final String CLASSES_PATH = "yolov8ObjectDetectionDiorClasses.txt";
    public void detectObjectOnImage(Mat geoTiffImage) {

    var diorClasses = new ArrayList<String>();

    try {
        diorClasses = getClasses();
        System.out.println(diorClasses);
    } catch (IOException ex) {
        System.out.printf("Could not read %s file:%n", CLASSES_PATH );
        ex.printStackTrace();
        //TODO: TO THROW REST EXCEPTION
    }

//  load our YOLO object detector trained on COCO dataset
    // read and process the input image
    Mat inputBlob = preProcessImageForYolov8ObjectDetectionDior(geoTiffImage);
    // Load the ONNX model
    Net dnnNet = Dnn.readNetFromONNX(PathUtils.getPathForImageInResources( MODEL_PATH ));
    System.out.println("DNN from ONNX was successfully loaded!");


    // generate radnom color in order to draw bounding boxes
    Random random = new Random();
    ArrayList<Scalar> colors = new ArrayList<Scalar>();
        for (int i= 0; i < diorClasses.size(); i++) {
        colors.add(new Scalar( new double[] {random.nextInt(255), random.nextInt(255), random.nextInt(255)}));
    }

    List<String> layerNames = dnnNet.getLayerNames();
    List<String> outputLayers = new ArrayList<String>();
        for (Integer i : dnnNet.getUnconnectedOutLayers().toList()) {
        outputLayers.add(layerNames.get(i - 1));
    }
    HashMap<String, List> result = forwardImageOverNetwork(inputBlob, dnnNet, outputLayers);

    ArrayList<Rect2d> boxes = (ArrayList<Rect2d>)result.get("boxes");
    ArrayList<Float> confidences = (ArrayList<Float>) result.get("confidences");
    ArrayList<Integer> class_ids = (ArrayList<Integer>)result.get("class_ids");

    // -- Now , do so-called “non-maxima suppression”
    //Non-maximum suppression is performed on the boxes whose confidence is equal to or greater than the threshold.
    // This will reduce the number of overlapping boxes:
    MatOfInt indices =  getBBoxIndicesFromNonMaximumSuppression(boxes,
            confidences);
    //-- Finally, go over indices in order to draw bounding boxes on the image:
    geoTiffImage =  drawBoxesOnTheImage(geoTiffImage,
                               indices,
                               boxes,
                               diorClasses,
                               class_ids,
                               colors);
        HighGui.imshow("Test", geoTiffImage );
        HighGui.waitKey(10000);
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
    private HashMap<String, List> forwardImageOverNetwork(Mat img, Net dnnNet, List<String> outputLayers) {
        // --We need to prepare some data structure  in order to store the data returned by the network  (ie, after Net.forward() call))
        // So, Initialize our lists of detected bounding boxes, confidences, and  class IDs, respectively
        // This is what this method will return:
        HashMap<String, List> result = new HashMap<String, List>();
        result.put("boxes", new ArrayList<Rect2d>());
        result.put("confidences", new ArrayList<Float>());
        result.put("class_ids", new ArrayList<Integer>());


        dnnNet.setInput(img);

        // -- the output from network's forward() method will contain a List of OpenCV Mat object, so lets prepare one
        List<Mat> outputs = new ArrayList<Mat>();

        // -- Finally, let pass forward throught network. The main work is done here:
        dnnNet.forward(outputs, outputLayers);

        // --Each output of the network outs (ie, each row of the Mat from 'outputs') is represented by a vector of the number
        // of classes + 5 elements.  The first 4 elements represent center_x, center_y, width and height.
        // The fifth element represents the confidence that the bounding box encloses the object.
        // The remaining elements are the confidence levels (ie object types) associated with each class.
        // The box is assigned to the category corresponding to the highest score of the box:

        for(Mat output : outputs) {
            //  loop over each of the detections. Each row is a candidate detection,
            System.out.println("Output.rows(): " + output.rows() + ", Output.cols(): " + output.cols());
            for (int i = 0; i < output.rows(); i++) {
                Mat row = output.row(i);
                List<Float> detect = new MatOfFloat(row).toList();
                List<Float> score = detect.subList(5, output.cols());
                int class_id = argmax(score); // index maximalnog elementa liste
                float conf = score.get(class_id);
                if (conf >= 0.5) {
                    int center_x = (int) (detect.get(0) * img.cols());
                    int center_y = (int) (detect.get(1) * img.rows());
                    int width = (int) (detect.get(2) * img.cols());
                    int height = (int) (detect.get(3) * img.rows());
                    int x = (center_x - width / 2);
                    int y = (center_y - height / 2);
                    Rect2d box = new Rect2d(x, y, width, height);
                    result.get("boxes").add(box);
                    result.get("confidences").add(conf);
                    result.get("class_ids").add(class_id);
                }
            }
        }
        return result;
    }
    /**
     Returns index of maximum element in the list
     */
    private  int argmax(List<Float> array){
        float max = array.get(0);
        int re = 0;
            for (int i = 1; i < array.size(); i++) {
            if (array.get(i) > max) {
                max = array.get(i);
                re = i;
            }
        }
            return re;
    }

    private MatOfInt getBBoxIndicesFromNonMaximumSuppression(ArrayList<Rect2d> boxes,
                                                     ArrayList<Float> confidences ) {
        MatOfRect2d mOfRect = new MatOfRect2d();
        mOfRect.fromList(boxes);
        MatOfFloat mfConfs = new MatOfFloat(Converters.vector_float_to_Mat(confidences));
        MatOfInt result = new MatOfInt();
        Dnn.NMSBoxes(mOfRect, mfConfs, (float)(0.6), (float)(0.5), result);
        return result;
    }
    private Mat drawBoxesOnTheImage(Mat img,
                            MatOfInt indices,
                                    ArrayList<Rect2d> boxes,
                                    List<String> cocoLabels,
                                    ArrayList<Integer> class_ids,
                                    ArrayList<Scalar> colors) {
        //Scalar color = new Scalar( new double[]{255, 255, 0});
        List indices_list = indices.toList();
        for (int i = 0; i < boxes.size(); i++) {
            if (indices_list.contains(i)) {
                Rect2d box = boxes.get(i);
                Point x_y = new Point(box.x, box.y);
                Point w_h = new Point(box.x + box.width, box.y + box.height);
                Point text_point = new Point(box.x, box.y - 5);
                Imgproc.rectangle(img, w_h, x_y, colors.get(class_ids.get(i)), 1);
                String label = cocoLabels.get(class_ids.get(i));
                Imgproc.putText(img, label, text_point, Imgproc.FONT_HERSHEY_SIMPLEX, 1, colors.get(class_ids.get(i)), 2);
            }
        }
        return img;
    }
}
