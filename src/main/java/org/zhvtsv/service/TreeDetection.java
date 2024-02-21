package org.zhvtsv.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.zhvtsv.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TreeDetection
{
    private static final String MODEL_PATH = "yolov8TreeDetection.onnx";
    private static final String CLASSES_PATH = "yolov8TreeDetectionClasses.txt";
    private static final int IMG_SIZE=640;

    public Mat detectObjectOnImage(Mat image){
        System.out.println("Image has " + image.channels());
        // Convert to 3 channels (RGB) if it has an alpha channel
        if (image.channels() == 4) {
            List<Mat> channels = new ArrayList<>();
            Core.split(image, channels);

            // Keep only the first 3 channels (BGR)
            Mat bgrImage = new Mat();
            Core.merge(channels.subList(0, 3), bgrImage);

            image = bgrImage;
        }
//        Mat inputBlob = preProcessImageForYolov8ObjectDetectionDior(geoTiffImage);
        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(IMG_SIZE, IMG_SIZE), Imgproc.INTER_AREA);
        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "resized.tif" ), resizedImage);

        // Calculate scale factor
//        double scale = length / IMG_SIZE;
        Mat inputBlob = Dnn.blobFromImage(resizedImage, 1.0/255.0, new Size(IMG_SIZE, IMG_SIZE), // Here we supply the spatial size that the Convolutional Neural Network expects.
                new Scalar(new double[]{0.0, 0.0, 0.0}), true, false);
//        Mat inputBlob = preProcessImageForYolov8EuroSat(imageLocation);

        // read generated ONNX model into org.opencv.dnn.Net object
        Net dnnNet = Dnn.readNetFromONNX(PathUtils.getPathForImageInResources( MODEL_PATH ));
        System.out.println("DNN from ONNX was successfully loaded!");

        // Set input for the neural network model
        dnnNet.setInput(inputBlob);


        // Perform inference
        Mat outputs = dnnNet.forward();

        Mat mat2D = outputs.reshape(1, (int)outputs.size().width ); // The second parameter is the number of rows

        Core.transpose( mat2D, mat2D );
        System.out.println(mat2D);
        System.out.println("Output: "+mat2D.rows()+" x "+mat2D.cols());

        HashMap<String, List> result = new HashMap<String, List>();
        result.put("boxes", new ArrayList<Rect2d>());
        result.put("scores", new ArrayList<Double>());
        result.put("class_ids", new ArrayList<Integer>());


        System.out.println("-----Start-----");
        for(int i=0;i<mat2D.rows();i++){
//        for(int i=0;i<3;i++){
            List<Double> scores = new ArrayList<>();
            for(int j=4;j<mat2D.cols();j++){
                scores.add( mat2D.get( i, j )[0] );
            }
            double maxScore = Collections.max(scores);
            double x = mat2D.get( i,0 )[0] - (0.5 * mat2D.get( i,2)[0]);
            double y = mat2D.get( i,1 )[0]- (0.5 * mat2D.get( i,3)[0]);
            //            double x = mat2D.get( i,0 )[0] - (0.5 * mat2D.get( i,2)[0]);
//            double y = mat2D.get( i,1 )[0]- (0.5 * mat2D.get( i,3)[0]);
            double width = mat2D.get( i,2 )[0];
            double height = mat2D.get( i,3 )[0];
//            System.out.println("L: "+scores.size() + ": "+scores);
//            System.out.println("Max score: "+ maxScore);
//            System.out.println("("+x+", "+y+", "+width+", "+height+")");
            if(maxScore >= 0.25) {
                int classId = argmax( scores );
                Rect2d box = new Rect2d(x, y, width, height);
                result.get("boxes").add(box);
                result.get("scores").add(maxScore);
                result.get("class_ids").add(classId);
            }
        }
        System.out.println("Found classes: "+(ArrayList<Integer>)result.get("class_ids"));
        System.out.println("______End________");
        System.out.println();
        System.out.println("Boxes preprocessing...");
        ArrayList<Rect2d> boxes = (ArrayList<Rect2d>)result.get("boxes");
        ArrayList<Double> scores = (ArrayList<Double>) result.get("scores");
        ArrayList<Integer> classIds = (ArrayList<Integer>)result.get("class_ids");
        if(classIds.isEmpty()){
            System.out.println("NO OBJECTS FOUND!");
            throw new NotFoundException("No objects found");
        }
        // -- Now , do so-called “non-maxima suppression”
        //Non-maximum suppression is performed on the boxes whose confidence is equal to or greater than the threshold.
        // This will reduce the number of overlapping boxes:
        MatOfRect2d mOfRect = new MatOfRect2d();
        mOfRect.fromList( boxes );
        List<Float> floatScores = scores.stream().map(Double::floatValue).collect(Collectors.toList());

        MatOfFloat mScores = new MatOfFloat(Converters.vector_float_to_Mat( floatScores));
        MatOfInt boxesResult = new MatOfInt();
        Dnn.NMSBoxes( mOfRect, mScores, 0.25f, 0.45f, boxesResult);
        System.out.println(classIds);
        drawBoxesOnTheImage( resizedImage, boxesResult , boxes, getClasses(), classIds, getRandomColorsPerClass());
//        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "1.tif" ), resizedImage);
//        HighGui.imshow("Test", img );
//        HighGui.waitKey(10000);
        //        result_boxes = cv2.dnn.NMSBoxes(boxes, scores, 0.25, 0.45, 0.5)

        System.out.println("Done.");
        return resizedImage;
    }

    public ArrayList<Scalar> getRandomColorsPerClass() {
        Random random = new Random();
        ArrayList<Scalar> colors = new ArrayList<Scalar>();
        for (int i= 0; i < getClasses().size(); i++) {
            colors.add(new Scalar( new double[] {random.nextInt(255), random.nextInt(255), random.nextInt(255)}));
        }
        return colors;
    }

    private Mat drawBoxesOnTheImage(Mat img, MatOfInt boxesResult, ArrayList<Rect2d> boxes, List<String> cocoLabels, ArrayList<Integer> classIds, ArrayList<Scalar> colors) {
        //Scalar color = new Scalar( new double[]{255, 255, 0});
        List indices_list = boxesResult.toList();
        for (int i = 0; i < boxes.size(); i++) {
            if (indices_list.contains(i)) {
                Rect2d box = boxes.get(i);
                Point x_y = new Point(box.x, box.y);
                Point w_h = new Point(box.x + box.width, box.y + box.height);
                Point text_point = new Point(box.x, box.y - 5);
                Imgproc.rectangle(img, w_h, x_y, new Scalar(0, 165, 255), 1);
                String label = cocoLabels.get(classIds.get(i));
                Imgproc.putText(img, label, text_point, Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 165, 255), 2);
            }
        }
        return img;
    }
    private ArrayList<String> getClasses()
    {
        ArrayList<String> imgLabels;
        try (Stream<String> lines = Files.lines( Path.of( PathUtils.getPathForImageInResources( CLASSES_PATH ) ) )) {
            imgLabels = lines.collect( Collectors.toCollection(ArrayList::new));
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        return imgLabels;
    }
    private Mat preProcessImageForYolov8ObjectDetectionDior(Mat imageToPreprocess) {
        // Get the dimensions of the original image
//        int height = imageToPreprocess.rows();
//        int width = imageToPreprocess.cols();
//        System.out.println(height);
//
//        // Prepare a square image for inference
//        int length = Math.max(height, width);
//        Mat image = new Mat(new Size(length, length), CvType.CV_8UC3, new Scalar(0, 0, 0));
//        imageToPreprocess.copyTo(image.submat(0, height, 0, width));
        Mat resizedImage = new Mat();
        Imgproc.resize(imageToPreprocess, resizedImage, new Size(IMG_SIZE, IMG_SIZE), Imgproc.INTER_AREA);
        Imgcodecs.imwrite(PathUtils.getPathForImageInResources( "resized.tif" ), resizedImage);

        // Calculate scale factor
//        double scale = length / IMG_SIZE;
        Mat blob = Dnn.blobFromImage(resizedImage, 1.0/255.0, new Size(IMG_SIZE, IMG_SIZE), // Here we supply the spatial size that the Convolutional Neural Network expects.
                new Scalar(new double[]{0.0, 0.0, 0.0}), true, false);

        return blob;
    }


    /**
     Returns index of maximum element in the list
     */
    private  int argmax(List<Double> array){
        double max = array.get(0);
        int re = 0;
        for (int i = 1; i < array.size(); i++) {
            if (array.get(i) > max) {
                max = array.get(i);
                re = i;
            }
        }
        return re;
    }

}
