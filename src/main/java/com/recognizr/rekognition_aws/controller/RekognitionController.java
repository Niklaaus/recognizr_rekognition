package com.recognizr.rekognition_aws.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.recognizr.rekognition_aws.service.RekognitionService;

@RestController
public class RekognitionController {

	@Autowired
	RekognitionService rekogService;

	final String collectionName = "engineer_sahab_faces_collection";
	
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

	@PostMapping(value = "/delete-collection", produces = "application/json")
	public ResponseEntity deleteCollection(@RequestBody String collectionName) {
		try {
			if (!StringUtils.isEmpty(collectionName)) {
				rekogService.deleteCollection(collectionName);
				return new ResponseEntity(HttpStatus.OK);
			} else {
				throw new Exception("Controller.deleteCollection ::: collectionName is empty");
			}
		} catch (Exception e) {
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/add-face", produces = "application/json")
	public ResponseEntity addFacesToCollection(@RequestBody Map<String, String> params) { 
		String personName = params.get("personName");
		String base64InputImage = params.get("base64InputImage");

		if (StringUtils.isEmpty(personName) || StringUtils.isEmpty(base64InputImage)) {
			System.out.println("addFacesToCollection controller :::: personName=" + personName + " base64InputImage::"
					+ StringUtils.isEmpty(base64InputImage));
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}

		if (!rekogService.collectionExists(collectionName)) {
			System.out.println("collection of this name does not exist");
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}
		int startOfBase64Data = base64InputImage.indexOf(",") + 1;
		base64InputImage = base64InputImage.substring(startOfBase64Data, base64InputImage.length());
		try {

			rekogService.addFaceToCollection(collectionName, Base64.getDecoder().decode(base64InputImage.getBytes()),
					personName);
		} catch (Exception e) {
			return new ResponseEntity(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity(HttpStatus.OK);
	}

	@PostMapping(value = "/recognize", produces = "application/json")
    @CrossOrigin(origins = "https://recognizr.xyz")
	public ResponseEntity searchFace(@RequestBody String base64InputImage) {
		if (!StringUtils.isEmpty(base64InputImage)) {
			int startOfBase64Data = base64InputImage.indexOf(",") + 1;
			base64InputImage = base64InputImage.substring(startOfBase64Data, base64InputImage.length());
			Map<String, String> response = rekogService
					.searchIdentifyFace(collectionName,Base64.getDecoder().decode(base64InputImage.getBytes()));
			return new ResponseEntity(response,HttpStatus.OK);
		}
		return new ResponseEntity(HttpStatus.BAD_REQUEST);
	}

}
