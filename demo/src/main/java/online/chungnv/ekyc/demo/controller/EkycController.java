package online.chungnv.ekyc.demo.controller;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import online.chungnv.ekyc.demo.bean.CompreFaceResponse;
import online.chungnv.ekyc.demo.bean.EkycRequest;

import online.chungnv.ekyc.demo.bean.EkycResponse;
import online.chungnv.ekyc.demo.bean.PersonInfomation;
import online.chungnv.ekyc.demo.service.CompreFaceService;
import online.chungnv.ekyc.demo.service.GoogleVisionService;
import online.chungnv.ekyc.demo.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
public class EkycController {
    private static final Logger logger = LoggerFactory.getLogger(EkycController.class);
    public static final DateFormat showDf = new SimpleDateFormat("dd/MM/yyyy");
    @Autowired
    GoogleVisionService googleVisionService;
    @Autowired
    CompreFaceService compreFaceService;

    @PostMapping("/kyc")
    public ResponseEntity<EkycResponse> kyc(@RequestBody EkycRequest request) {
        logger.info("start ekyc");
        EkycResponse ekycResponse = new EkycResponse();
        String transactionId = String.valueOf(Calendar.getInstance().getTime().getTime());
        String portraitFileName = transactionId + "_portrait.jpg";
        String frontFileName = transactionId + "_front.jpg";
        String backFileName = transactionId + "_back.jpg";
        if (request.getPortrait() != null && request.getPortrait().startsWith("data:image/jpeg;base64,")) {
            Utils.convertBase64ToFile(request.getPortrait(), portraitFileName);
        }
        if (request.getFront() != null && request.getFront().startsWith("data:image/jpeg;base64,")) {
            Utils.convertBase64ToFile(request.getFront(), frontFileName);
        }
        if (request.getBack() != null && request.getBack().startsWith("data:image/jpeg;base64,")) {
            Utils.convertBase64ToFile(request.getBack(), backFileName);
        }

        //Read MRZ from front to detect passport
        //TODO
        //If not passport, call google vision
        AnnotateImageResponse frontResponse = googleVisionService.processImageByVision(frontFileName);
        logger.info("text:" + frontResponse.getFullTextAnnotation().getText());

        List<String> ids = Utils.extractCCCD(frontResponse.getFullTextAnnotation().getText());
        List<String> names = Utils.extractUppercaseStrings(frontResponse.getFullTextAnnotation().getText());
        List<String> dateList = Utils.extractDates(frontResponse.getFullTextAnnotation().getText());
        PersonInfomation personInfomation = new PersonInfomation();
        if (ids.size() > 0){
            personInfomation.setId(ids.get(0));
        }

        if (names.size() > 4){
            personInfomation.setName(names.get(3));
        }
        if (dateList.size() > 0){
            if (dateList.size() == 2){
                //detect both expiry and dob
                try {
                    Date date1 = showDf.parse(dateList.get(0));
                    Date date2 = showDf.parse(dateList.get(1));
                    if (date1.getTime() > date2.getTime()){
                        personInfomation.setExpiredDate(dateList.get(0));
                        personInfomation.setDob(dateList.get(1));
                    }else{
                        personInfomation.setExpiredDate(dateList.get(1));
                        personInfomation.setDob(dateList.get(0));
                    }
                }catch (Exception e){

                }

            }
        }
        logger.info("ids:" + ids);
        logger.info("names:" + names);
        logger.info("dateList:" + dateList);

        //Check QR code existence
        for (LocalizedObjectAnnotation annotation : frontResponse.getLocalizedObjectAnnotationsList()) {
            logger.info("Object: " + annotation.getName());
            logger.info("Bounding Poly: " + annotation.getBoundingPoly());
            if (annotation.getName().equals("2D barcode")) {
                personInfomation.setDocumentType("CCCD");
                //CCCD detected, extract QRCode
                BufferedImage qrCodeImage = Utils.cropImage(frontFileName, annotation.getBoundingPoly());
                try {
                    File outputfile = new File("qrcode.jpg");
                    ImageIO.write(qrCodeImage, "jpg", outputfile);
                }catch (Exception e){

                }

                String qrCode = Utils.readQRCode(qrCodeImage);
                if (qrCode != null){
                    personInfomation = Utils.parseIdQRCode(qrCode);
                    logger.info("personInfomation:" + personInfomation);

                    //extract expiry date from text

                    for (String date : dateList){
                        if (!personInfomation.getDob().equals(date)){
                            personInfomation.setExpiredDate(date);
                        }

                    }
                }

            }

        }

        ekycResponse.setErrorCode("00");
        ekycResponse.setErrorDesc("Success");
        ekycResponse.setPersonInfomation(personInfomation);
        CompreFaceResponse compreFaceResponse = compreFaceService.verify(frontFileName, portraitFileName);
        ekycResponse.setSimilarity(compreFaceResponse.getResult().get(0).getFace_matches().get(0).getSimilarity());
        return ResponseEntity.ok(ekycResponse);
    }


}
