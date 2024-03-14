package org.zhvtsv.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.zhvtsv.utils.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TreeDetectionDeepforest
{
    private static final String MODEL_PATH = "yolov8TreeDetection.onnx";
    private static final String CLASSES_PATH = "yolov8TreeDetectionClasses.txt";
    private static final int IMG_SIZE=640;

    public byte[] detectObjectOnImage(Mat image, String model){
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".tif", image, matOfByte);
        byte[] img = matOfByte.toArray();
        HttpClient httpClient = HttpClient.newHttpClient();
//        Map<String, byte[]> formData = Map.of("image", image);

        // Create HttpRequest
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8081/deepforest"))
                    .header("Content-Type", "multipart/form-data")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(img))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Send request and handle response
        HttpResponse<byte[]> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Print response code
        System.out.println("Response code: " + response.statusCode());

        // Access the response body as a byte array
        byte[] responseBody = response.body();
            try {
                Files.write(Path.of("/Users/zezko/Documents/bakk/sentinel-processing-api/src/main/resources/zezkobytes.tif"), responseBody);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    return responseBody;
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
