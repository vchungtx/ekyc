package online.chungnv.ekyc.demo.service;

import com.google.api.client.util.Value;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class GoogleVisionService {

    private String googleVisionJsonPath;

    @Autowired
    public GoogleVisionService(Environment env) {
        this.googleVisionJsonPath = env.getProperty("google-vision.path");
    }

    public AnnotateImageResponse processImageByVision(String imagePath){
        try{
            System.out.println("google-vision:" + googleVisionJsonPath);
            // Read the image file into a ByteString
            Path path = Paths.get(imagePath);
            byte[] imageBytes = Files.readAllBytes(path);
            ByteString imgBytes = ByteString.copyFrom(imageBytes);

            // Create the image request
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Define the features for object detection and face detection
            Feature objectDetectionFeature = Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).build();
            Feature faceDetectionFeature = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
            Feature documentDetectionFeature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            // Create the annotate image request with both features
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(objectDetectionFeature)
                    .addFeatures(faceDetectionFeature)
                    .addFeatures(documentDetectionFeature)
                    .setImage(img)
                    .build();

            // Authenticate using the service account JSON file
            GoogleCredentials credentials = GoogleCredentials.fromStream(Files.newInputStream(Paths.get(googleVisionJsonPath)));
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            // Perform the request
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(ImmutableList.of(request));
                AnnotateImageResponse imageResponse = response.getResponses(0);
                return imageResponse;
                // Process the response to extract object detection and face detection results
//                if (imageResponse.hasError()) {
//                    System.err.println("Error: " + imageResponse.getError().getMessage());
//                } else {
//                    for (LocalizedObjectAnnotation annotation : imageResponse.getLocalizedObjectAnnotationsList()) {
//                        System.out.println("Object: " + annotation.getName());
//                        System.out.println("Bounding Poly: " + annotation.getBoundingPoly());
//                        String qrFile = "qrtemp.jpg";
//                        cropImage(path, annotation.getBoundingPoly(), qrFile);
//                        System.out.println(readQRCode(qrFile));
//                    }
//
//                    for (FaceAnnotation annotation : imageResponse.getFaceAnnotationsList()) {
//                        System.out.println("Joy likelihood: " + annotation.getJoyLikelihood());
//                        System.out.println("Bounding Poly: " + annotation.getBoundingPoly());
//                    }
//                    System.out.println(imageResponse.getFullTextAnnotation().getText());
//                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

}
