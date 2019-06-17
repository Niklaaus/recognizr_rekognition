package com.recognizr.rekognition_aws.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recognizr.rekognition_aws.service.RekognitionService;

@RestController
public class RekognitionController {

@Autowired
RekognitionService rekogService;

@GetMapping(value="/test", produces="application/json")
public ResponseEntity testingRekognition() {
	String filepath="/media/chilgoza/Photos & Videos/Personal Photos n Videos/VaultOfPhotos101/IMG-20150530-WA0003.jpg";
	byte[] bytes;
    try {
        bytes = Files.readAllBytes(Paths.get(filepath));
    } catch (IOException e) {
        System.err.println("Failed to load image: " + e.getMessage());
        return new ResponseEntity(null,HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
    }
   
	return new ResponseEntity(rekogService.detectLabels(bytes),HttpStatus.OK);
	
}
	
}
