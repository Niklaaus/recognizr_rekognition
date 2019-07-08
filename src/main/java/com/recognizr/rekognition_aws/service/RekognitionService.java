package com.recognizr.rekognition_aws.service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.Celebrity;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest;
import com.amazonaws.services.rekognition.model.DeleteCollectionResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesRequest;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesResult;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
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
			CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(collectionName);

			CreateCollectionResult resultCollection = rekognition.createCollection(request);

			Integer statusCode = resultCollection.getStatusCode();
			String collectionArn = resultCollection.getCollectionArn();
			String faceModelVersion = resultCollection.getFaceModelVersion();

			if (statusCode != 200 || StringUtils.isEmpty(collectionArn) || StringUtils.isEmpty(faceModelVersion)) {
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

	public void addFaceToCollection(String collectionName, byte[] bytes, String personName) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

		AmazonRekognition rekognition = ClientFactory.createClient();
		IndexFacesRequest request = new IndexFacesRequest().withCollectionId(collectionName)
				.withDetectionAttributes("ALL").withImage(new Image().withBytes(byteBuffer))
				.withExternalImageId(personName);
		IndexFacesResult result = rekognition.indexFaces(request);
		// debug code

		List<FaceRecord> faceRecords = result.getFaceRecords();
		for (FaceRecord rec : faceRecords) {
			FaceDetail faceDetail = rec.getFaceDetail();

			Face face = rec.getFace();
			System.out.println("Face-ID: " + face.getFaceId() + "\nImage ID: " + face.getImageId()
					+ "\nExternal Image ID: " + face.getExternalImageId() + "\nConfidence: " + face.getConfidence());
		}
		// debug ends
	}

	public Map<String, String> searchIdentifyFace(String collectionName, byte[] bytes) {
		Map<String, String> response = new HashMap<>();
		try {
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

			AmazonRekognition rekognition = ClientFactory.createClient();
			Image image = new Image().withBytes(byteBuffer);

			SearchFacesByImageRequest request = new SearchFacesByImageRequest().withCollectionId(collectionName)
					.withImage(image);
			SearchFacesByImageResult result = rekognition.searchFacesByImage(request);

			List<FaceMatch> faceMatches = result.getFaceMatches();
			
			if(!faceMatches.isEmpty()) {
			FaceMatch match=faceMatches.get(0);//because only one face we are talking abt
			Face face = match.getFace();
			Float similarity = match.getSimilarity();
			response.put("person", face.getExternalImageId());
			response.put("confidence", String.valueOf(similarity));
			
			if (Float.compare(similarity, 87) >= 0) {
				response.put("resp_code", "FR");
			} else{
				response.put("resp_code", "NR");
			}// face recognised
			}
			
			if (StringUtils.isEmpty(response.get("resp_code"))) {
				response.putAll(this.checkForCelebrities(bytes));
				
			}
			
			/*
			 * for (FaceMatch match : faceMatches) { Float similarity =
			 * match.getSimilarity(); Face face = match.getFace();
			 * System.out.println("MATCH:" + "\nSimilarity: " + similarity + "\nFace-ID: " +
			 * face.getFaceId() + "\nImage ID: " + face.getImageId() +
			 * "\nExternal Image ID: " + face.getExternalImageId() + "\nConfidence: " +
			 * face.getConfidence()); response.put("person", face.getExternalImageId());
			 * response.put("confidence", String.valueOf(similarity));
			 * 
			 * if (Float.compare(similarity, 87) >= 0) { response.put("resp_code", "FR"); }
			 * // face recognised else { response.put("resp_code", "NR");
			 * 
			 * response.putAll(this.checkForCelebrities(bytes));
			 * 
			 * if (StringUtils.isEmpty(response.get("resp_code"))) { // no celeb recognised
			 * response.put("resp_code", "NR"); }
			 * 
			 * }
			 * 
			 * }
			 */

		} catch (Exception e) {
			response.put("resp_code", "NF");

		}
		return response;
	}

	public Map<String, String> checkForCelebrities(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		Map<String, String> response = new HashMap<>();
		AmazonRekognition rekognition = ClientFactory.createClient();
		RecognizeCelebritiesRequest request = new RecognizeCelebritiesRequest()
				.withImage(new Image().withBytes(byteBuffer));

		RecognizeCelebritiesResult result = rekognition.recognizeCelebrities(request);

		List<Celebrity> celebs = result.getCelebrityFaces();
		System.out.println(celebs.size() + " celebrity(s) were recognized.\n");

		for (Celebrity celebrity : celebs) {
			System.out.println("Celebrity recognized: " + celebrity.getName());
			System.out.println("Celebrity ID: " + celebrity.getId());
			response.put("person", celebrity.getName());
			response.put("confidence", String.valueOf(celebrity.getMatchConfidence()));
			if (Float.compare(celebrity.getMatchConfidence(), 87) >= 0) {
				response.put("resp_code", "CelebFR");
			} // celeb face recognised
			else {
				response.put("resp_code", "CelebNR"); // not really recognised

			}
		}
		System.out.println(result.getUnrecognizedFaces().size() + " face(s) were unrecognized.");
		return response;
	}

	public Integer deleteCollection(String collectionName) {
		DeleteCollectionRequest request = new DeleteCollectionRequest().withCollectionId(collectionName);
		AmazonRekognition rekognition = ClientFactory.createClient();
		DeleteCollectionResult result = rekognition.deleteCollection(request);

		Integer statusCode = result.getStatusCode();
		System.out.println("deleteCollection :: Status code: " + statusCode);
		return statusCode;
	}

}
