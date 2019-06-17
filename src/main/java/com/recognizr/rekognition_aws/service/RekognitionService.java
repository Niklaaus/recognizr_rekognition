package com.recognizr.rekognition_aws.service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.recognizr.rekognition_aws.factory.ClientFactory;

@Service
public class RekognitionService {

	public Map<String, Float> detectLabels(byte[] bytes) {
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

}
