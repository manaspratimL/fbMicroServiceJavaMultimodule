package com.rs.ms.fb.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.restfb.DefaultJsonMapper;
import com.restfb.JsonMapper;
import com.restfb.types.webhook.WebhookEntry;
import com.restfb.types.webhook.WebhookObject;
import com.rs.ms.fb.kafka.KafkaProducerService;
import com.rs.ms.fb.models.FacebookData;
import com.rs.ms.fb.service.base.FacebookWebhookService;

/**
 * 
 * @author ManasC
 *
 */
@RestController
public class FacebookWebhookController {

	@Autowired
    FacebookWebhookService fbWebhookService;
	
	@Autowired
    KafkaProducerService kafkaProducer;
	
	@RequestMapping(value ="/facebook", method = RequestMethod.GET)
	public String getfacebookChallenge(@RequestParam(value = "hub.mode") String mode,
			@RequestParam(value = "hub.verify_token") String verify_token, @RequestParam(value = "hub.challenge") String challenge) {
		
		if(mode.equalsIgnoreCase("subscribe") && verify_token.equalsIgnoreCase("token")){
			return challenge;
		}
		
		return "Challenge Failed";
	}

	@RequestMapping(value = "/facebook", method = RequestMethod.POST)
	public void facebookDataStream(HttpServletRequest request,String data) throws IOException {
		
		String pushedJsonAsString = IOUtils.toString(request.getInputStream(),"utf-8");
		System.out.println("JSON String : "+pushedJsonAsString);
		
		JsonMapper mapper = new DefaultJsonMapper();
		WebhookObject webhookObject = 
		         mapper.toJavaObject(pushedJsonAsString, WebhookObject.class);
		
		System.out.println("webhook object"+webhookObject.getEntryList().toString());
		
		fbWebhookService.processFieldChanges(webhookObject);
	
		
	}
	
	@RequestMapping(value ="/facebookPage", method = RequestMethod.GET)
	public String getfacebookChallengePage(@RequestParam(value = "hub.mode") String mode,
			@RequestParam(value = "hub.verify_token") String verify_token, @RequestParam(value = "hub.challenge") String challenge) {
		
		if(mode.equalsIgnoreCase("subscribe") && verify_token.equalsIgnoreCase("token")){
			return challenge;
		}
		
		return "Challenge Failed";
	}

	@RequestMapping(value = "/facebookPage", method = RequestMethod.POST)
	public void facebookDataStreamPage(HttpServletRequest request,String data) throws Exception {
				
		
		String pushedJsonAsString = IOUtils.toString(request.getInputStream(),"utf-8");
		System.out.println("JSON String : "+pushedJsonAsString);
		
		JsonMapper mapper = new DefaultJsonMapper();
		WebhookObject webhookObject = 
		         mapper.toJavaObject(pushedJsonAsString, WebhookObject.class);
		
		System.out.println("webhook object"+webhookObject.getEntryList().toString());
		
		for(WebhookEntry entry:webhookObject.getEntryList()){
			System.out.println("ID : "+entry.getId());
			System.out.println("changed fields : "+entry.getChangedFields());
			System.out.println("Changes : "+entry.getChanges());
		}
		
		FacebookData fbData= new FacebookData();
		fbData.setType("STREAM");
		fbData.setWebhookObject(webhookObject);
		
		 Gson gson = new GsonBuilder().setPrettyPrinting().create();
		 
		 String json = gson.toJson(fbData);
	     System.out.println(json);
		
		kafkaProducer.publishUpdates(json, "rsmsfb");
		
	}
	
	@RequestMapping(value ="/publish", method = RequestMethod.POST)
	public String publishonkafka(HttpServletRequest request) throws Exception {
		
		String pushedJsonAsString = IOUtils.toString(request.getInputStream(),"utf-8");
		System.out.println("JSON String : "+pushedJsonAsString);
		
		kafkaProducer.publishUpdates(pushedJsonAsString, "rsmsfb");
		
		return "kafka Publishing";
	}

}
