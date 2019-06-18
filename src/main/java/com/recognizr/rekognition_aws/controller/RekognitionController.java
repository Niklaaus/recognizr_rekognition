package com.recognizr.rekognition_aws.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.recognizr.rekognition_aws.service.RekognitionService;

@RestController
public class RekognitionController {

	@Autowired
	RekognitionService rekogService;

	@GetMapping(value = "/test", produces = "application/json")
	public ResponseEntity testingRekognition() {
		String filepath = "/media/chilgoza/Photos & Videos/Personal Photos n Videos/VaultOfPhotos101/IMG-20150530-WA0003.jpg";
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(Paths.get(filepath));
		} catch (IOException e) {
			System.err.println("Failed to load image: " + e.getMessage());
			return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
		}
		try {
			return new ResponseEntity(rekogService.detectLabels(bytes), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/create-collection", produces = "application/json")
	public ResponseEntity createIfNotExistsCollection(@RequestBody String collectionName) {

		try {
			if (!StringUtils.isEmpty(collectionName)) {
				rekogService.createIfNotExistsCollection(collectionName);
			}
		} catch (Exception e) {
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity(HttpStatus.OK);
	}

	@PostMapping(value = "/add-face", produces = "application/json")
	public ResponseEntity addFacesToCollection(@RequestBody String collectionName) { // will add another parameter to
																						// handle base64 image coming
																						// from front end

		if (!rekogService.collectionExists(collectionName)) {
			System.out.println("collection of this name does not exist");
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}

		// temp code till front end is not created for training
		String folderPath = "/home/chilgoza/Downloads/" + collectionName;
		File foldername = new File(folderPath);
		for (File f : foldername.listFiles()) {
			Path path = Paths.get(f.getAbsolutePath());
			try {
				byte[] bytes = Files.readAllBytes(path);
				rekogService.addFaceToCollection(collectionName, bytes);
			} catch (Exception e) {
				return new ResponseEntity(HttpStatus.BAD_REQUEST);
			}

		}
		// temp code till front end is not created for training ends

		return new ResponseEntity(HttpStatus.OK);
	}
	
	@PostMapping(value = "/recognize", produces = "application/json")
	public ResponseEntity searchFace(@RequestBody String base64InputImage) {
		
		return null;
	}
	
}
