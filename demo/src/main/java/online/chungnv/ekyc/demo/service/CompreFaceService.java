package online.chungnv.ekyc.demo.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import online.chungnv.ekyc.demo.bean.CompreFaceResponse;
import online.chungnv.ekyc.demo.controller.EkycController;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.apache.http.HttpHeaders;;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class CompreFaceService {
    private static final Logger logger = LoggerFactory.getLogger(CompreFaceService.class);
    private String url;
    private String apiKey;

    @Autowired
    public CompreFaceService(Environment env) {
        this.url = env.getProperty("compreface.url");
        this.apiKey = env.getProperty("compreface.apiKey");
    }

    public CompreFaceService() {
    }

    public CompreFaceResponse verify(String source, String target) {
        try {
            logger.info("call face verify");
            // Create the HTTP POST request
            HttpPost request = new HttpPost(url);
//            request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType());
            request.setHeader("x-api-key", apiKey);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            File sourceFile = new File(source);
            File targetFile = new File(target);
            builder.addBinaryBody("source_image", sourceFile, ContentType.DEFAULT_BINARY, sourceFile.getName());
            builder.addBinaryBody("target_image", targetFile, ContentType.DEFAULT_BINARY, targetFile.getName());
            HttpEntity multipart = builder.build();
            request.setEntity(multipart);
            // Create a HttpClient
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                // Successful response
                HttpEntity responseEntity = response.getEntity();
                String responseBody = EntityUtils.toString(responseEntity);
                logger.info("compre responseBody:" + responseBody);
                return new Gson().fromJson(responseBody, CompreFaceResponse.class);
            } else {
                // Handle the error response
                throw new RuntimeException("Face verification request failed with status code: " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        CompreFaceService service  = new CompreFaceService();
        service.url = "http://localhost:8000/api/v1/verification/verify";
        service.apiKey = "00000000-0000-0000-0000-000000000004";
        System.out.println(service.verify("1688094389728_front.jpg", "1688109020304_portrait.jpg"));
    }
}
