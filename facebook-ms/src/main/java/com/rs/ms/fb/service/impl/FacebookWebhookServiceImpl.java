package com.rs.ms.fb.service.impl;

import com.restfb.types.Post;
import com.restfb.types.webhook.WebhookEntry;
import com.restfb.types.webhook.WebhookObject;
import com.rs.ms.fb.service.base.FacebookUserDataService;
import com.rs.ms.fb.service.base.FacebookWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 
 * @author ManasC
 *
 */
@Service
public class FacebookWebhookServiceImpl implements FacebookWebhookService {
	
	@Autowired
    FacebookUserDataService fbUserDataService;
	/**
	 * 
	 */
	public void processFieldChanges(WebhookObject webhookObject){
		
		for(WebhookEntry entry:webhookObject.getEntryList()){
			processChange(entry);
		}
	}
	/**
	 * 
	 * @param entry
	 */
	private void processChange(WebhookEntry entry){
		
		System.out.println("ID : "+entry.getId());
		System.out.println("changed fields : "+entry.getChangedFields());
		System.out.println("Changes : "+entry.getChanges());
		
		for(String changeField : entry.getChangedFields()){
			getChanges(changeField,entry.getId());
		}
		
	}
	
	/**
	 * 
	 * @param change
	 * @param fbUserID
	 */
	private void getChanges(String change,String fbUserID){
		
		System.out.println("Fetching Changes");
		
		switch (change) {
		case "feed":
			String token=FacebookUserDataService.Usertokens.get(fbUserID);
			List<Post> posts=fbUserDataService.getUserFeed(token,1,false);
			for(Post post : posts){
				System.out.println(post.getMessage());
				System.out.println(post.getStory());
			}
			break;

		default:
			break;
		}
	}
}
