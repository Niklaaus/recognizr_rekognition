package com.recognizr.rekognition_aws.service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.recognizr.rekognition_aws.factory.ClientFactory;

@Service
public class RekognitionService {

	public Map<String, Float> detectLabels(byte[] bytes) throws Exception {
		Map<String, Float> responseLabels = new HashMap<String, Float>();
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

		AmazonRekognition rekognition = ClientFactory.createClient();

		DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withBytes(byteBuffer))
				.withMaxLabels(10);
		DetectLabelsResult result = rekognition.detectLabels(request);

		List<Label> labels = result.getLabels();

		for (Label label : labels) {
			responseLabels.put(label.getName(), label.getConfidence());
		}
		return responseLabels;
	}

	public void createIfNotExistsCollection(String collectionName) throws Exception {
		
		if (!collectionExists(collectionName)) {
		AmazonRekognition rekognition = ClientFactory.createClient();
		 CreateCollectionRequest request = new CreateCollectionRequest()
	                .withCollectionId(collectionName);
	 
	        CreateCollectionResult resultCollection = rekognition.createCollection(request);
	 
	        Integer statusCode = resultCollection.getStatusCode();
	        String collectionArn = resultCollection.getCollectionArn();
	        String faceModelVersion = resultCollection.getFaceModelVersion();
	        
	        if(statusCode!=200 || StringUtils.isEmpty(collectionArn) || StringUtils.isEmpty(faceModelVersion)) {
	        	throw new Exception("some error creating collection" + collectionName);
	        }
	}
	}
	
	public boolean collectionExists(String collectionName) {
		ListCollectionsRequest listRequest = new ListCollectionsRequest().withMaxResults(100);

		AmazonRekognition rekognition = ClientFactory.createClient();
		ListCollectionsResult resultList = rekognition.listCollections(listRequest);

		List<String> collectionIds = resultList.getCollectionIds();
		while (collectionIds != null) {
			if (collectionIds.contains(collectionName)) {
				System.out.println("collection with this name already exists");
				return true;
			}

			String token = resultList.getNextToken();
			if (token != null) {
				resultList = rekognition.listCollections(listRequest.withNextToken(token));
			} else {
				collectionIds = null;
			}
		}
		
		return false;
	}

	public void addFaceToCollection(String collectionName, byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

		AmazonRekognition rekognition = ClientFactory.createClient();
		  IndexFacesRequest request = new IndexFacesRequest()
                  .withCollectionId(collectionName)
                  .withDetectionAttributes("ALL")
                  .withImage(new Image().withBytes(byteBuffer));
          IndexFacesResult result = rekognition.indexFaces(request);
		//debug code

          List<FaceRecord> faceRecords = result.getFaceRecords();
          for (FaceRecord rec : faceRecords) {
              FaceDetail faceDetail = rec.getFaceDetail();
             
              Face face = rec.getFace();
              System.out.println("Face-ID: " + face.getFaceId() +
                      "\nImage ID: " + face.getImageId() +
                      "\nExternal Image ID: " + face.getExternalImageId() +
                      "\nConfidence: " + face.getConfidence());
          }
          //debug ends
	}

}
