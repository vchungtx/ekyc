package online.chungnv.ekyc.demo.utils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import online.chungnv.ekyc.demo.bean.PersonInfomation;
import online.chungnv.ekyc.demo.controller.EkycController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final DateFormat documentDf = new SimpleDateFormat("ddMMyyyy");
    public static final DateFormat showDf = new SimpleDateFormat("dd/MM/yyyy");
    public static final int CROP_MARGIN = 5;
    public static String readQRCode(BufferedImage image) {
        try {
            logger.info("Start read qr");
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//            Result result = new MultiFormatReader().decode(bitmap);
            MultiFormatReader reader = new MultiFormatReader();
            GenericMultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
            Result[] zxingResults = multiReader.decodeMultiple(bitmap);
            logger.info("ZXing result count: " + zxingResults.length);
            if (zxingResults != null) {
                for (Result zxingResult : zxingResults) {
                    logger.info("Format: " + zxingResult.getBarcodeFormat());
                    logger.info("Text: " + zxingResult.getText());
                    return  zxingResult.getText();
                }
            }

        } catch (NotFoundException e) {
            e.printStackTrace();

        }
        return null;
    }

    public static void convertBase64ToFile(String base64Image, String filePath) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Image.replaceFirst("data:image/jpeg;base64,", ""));

            // Write the decoded bytes to a file
            Path file = Paths.get(filePath);
            Files.write(file, decodedBytes, StandardOpenOption.CREATE);
            logger.info("File created successfully at: " + file.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static BufferedImage cropImage(String filePath,  BoundingPoly boundingPoly){
        try {
            logger.info("Start crop");
            Path path = Paths.get(filePath);
            BufferedImage image = ImageIO.read(Files.newInputStream(path));

            // Retrieve the normalized vertices of the bounding polygon
            List<NormalizedVertex> normalizedVertices = boundingPoly.getNormalizedVerticesList();

            // Calculate the pixel coordinates of the bounding polygon
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            logger.info("imageWidth: " + imageWidth);
            logger.info("imageHeight: " + imageHeight);
            int xMin = Integer.MAX_VALUE;
            int yMin = Integer.MAX_VALUE;
            int xMax = Integer.MIN_VALUE;
            int yMax = Integer.MIN_VALUE;

            for (NormalizedVertex vertex : normalizedVertices) {
                int x = (int) (vertex.getX() * imageWidth);
                int y = (int) (vertex.getY() * imageHeight);

                xMin = Math.min(xMin, x) - CROP_MARGIN;
                yMin = Math.min(yMin, y) - CROP_MARGIN;
                xMax = Math.max(xMax, x) + CROP_MARGIN;
                yMax = Math.max(yMax, y) + CROP_MARGIN;
            }

            // Crop the image
            BufferedImage croppedImage = image.getSubimage(xMin, yMin, xMax - xMin, yMax - yMin);
            return croppedImage;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args) throws Exception{
//        BufferedImage in = ImageIO.read(new File("qrcode.jpg"));
//        logger.info(readQRCode(in));
        String name = "NGÔ THỊ LỆ THUỶ";
String mrz = "IDVNM1910086005034191008600<<8\n" +
        "9106204F3106202VNM<<<<<<<<<<2\n" +
        "NGO<THI<LE<THUY<<<<<<<<<<<<<<<";

        List<String> mrzDate = extractMRZDateCCCD(mrz);
        System.out.println(mrzDate);
        System.out.println(extractMRZIdCCCD(mrz));
        List<String> mrzName = extractMRZName(mrz);
        System.out.println(mrzName);
        if (mrzDate.size() > 0){
            String mrzStr = mrzDate.get(0);
            String mrzDob = mrzStr.substring(0,6);
            String mrzGender = mrzStr.substring(7,8);
            String mrzExpired = mrzStr.substring(8,14);
            System.out.println(mrzDob);
            System.out.println(mrzGender);
            System.out.println(mrzExpired);
        }

        if (mrzName.size() > 0){
            String mrzStr = mrzName.get(0);
            String nameASCII = mrzStr.replaceAll("<<", " ").replaceAll("<"," ").trim();
            if (convertToAscii(name).trim().equals(nameASCII)){
                System.out.printf(name);
            }
        }
    }

    public static PersonInfomation parseIdQRCode(String qrCode){
        PersonInfomation personInfomation = new PersonInfomation();
        personInfomation.setDocumentType("CCCD");
        String[] splited = qrCode.split("\\|");
        if (splited.length > 6){
            personInfomation.setId(splited[0]);
            personInfomation.setAdditional(splited[1]);
            personInfomation.setName(splited[2]);
            try {
                logger.info(splited[3]);
                personInfomation.setDob(showDf.format(documentDf.parse(splited[3])));
                personInfomation.setIssuedDate(showDf.format(documentDf.parse(splited[6])));
            }catch (Exception e){
                e.printStackTrace();
            }
            personInfomation.setGender(splited[4]);
            personInfomation.setAddress(splited[5]);
        }
        return personInfomation;
    }

    public static List<String> extractDates(String input) {
        List<String> dates = new ArrayList<>();

        String regex = "\\b\\d{2}/\\d{2}/\\d{4}\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String date = matcher.group();
            dates.add(date);
        }

        return dates;
    }

    public static List<String> extractCCCD(String input) {
        List<String> dates = new ArrayList<>();

        String regex = "\\b\\d{12}\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String date = matcher.group();
            dates.add(date);
        }

        return dates;
    }

    public static List<String> extractUppercaseStrings(String input) {
        List<String> uppercaseStrings = new ArrayList<>();

        String regex = "\\b[\\p{Lu}][\\p{Lu} ]*[\\p{Lu}]\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String uppercaseString = matcher.group();
            if (countWords(uppercaseString) >= 2) {
                uppercaseStrings.add(uppercaseString);
            }
        }

        return uppercaseStrings;
    }

    public static List<String> extractMRZDateCCCD(String input) {
        List<String> response = new ArrayList<>();

        String regex = "\\d{7}(M|F)\\d{7}VNM";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {

            String str = matcher.group();
            response.add(str);
        }

        return response;
    }
public static List<String> extractMRZIdCCCD(String input) {
        List<String> response = new ArrayList<>();

        String regex = "IDVNM\\d{22}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {

            String str = matcher.group();
            response.add(str);
        }

        return response;
    }


    public static List<String> extractMRZName(String input) {
        List<String> response = new ArrayList<>();
        String regex = "\\b[A-Z]+<{1,2}[A-Z<]+<<";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String str = matcher.group();
            response.add(str);
        }

        return response;
    }


    public static int countWords(String input) {
        String[] words = input.split("\\s+");
        return words.length;
    }

    public static String convertToAscii(String vietnameseText) {
        String normalizedText = Normalizer.normalize(vietnameseText, Normalizer.Form.NFD);
        String asciiText = normalizedText.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D");

        return asciiText;
    }

    public static double calculateStringSimilarity(String string1, String string2) {
        int distance = calculateLevenshteinDistance(string1, string2);
        int maxLength = Math.max(string1.length(), string2.length());

        double similarity = 1 - (double) distance / maxLength;

        return similarity;
    }

    public static int calculateLevenshteinDistance(String string1, String string2) {
        int[][] dp = new int[string1.length() + 1][string2.length() + 1];

        for (int i = 0; i <= string1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= string2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= string1.length(); i++) {
            for (int j = 1; j <= string2.length(); j++) {
                if (string1.charAt(i - 1) == string2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    int min = Math.min(dp[i - 1][j], dp[i][j - 1]);
                    dp[i][j] = 1 + Math.min(min, dp[i - 1][j - 1]);
                }
            }
        }

        return dp[string1.length()][string2.length()];
    }
}
