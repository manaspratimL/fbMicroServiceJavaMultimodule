package com.rs.ms.fb.service.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.json.JsonObject;
import com.restfb.types.Comment;
import com.restfb.types.Event;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.Page;
import com.restfb.types.Post;
import com.rs.ms.fb.kafka.KafkaProducerService;
import com.rs.ms.fb.models.FacebookData;
import com.rs.ms.fb.models.FacebookPageModel;
import com.rs.ms.fb.models.FbComment;
import com.rs.ms.fb.models.FbLike;
import com.rs.ms.fb.models.FbPost;
import com.rs.ms.fb.models.PageAccounts;
import com.rs.ms.fb.service.base.FacebookClientService;
import com.rs.ms.fb.service.base.FacebookPageDataService;
import com.rs.ms.fb.utils.DataUrls;

/**
 * 
 * @author ManasC
 *
 */
@Service
public class FacebookPageDataServiceImpl implements FacebookPageDataService {

	@Autowired
	FacebookClientService facebookClientService;

	@Autowired
	KafkaProducerService kafkaProducer;

	private FacebookClient facebookClient;

	public PageAccounts getPageAccounts(String token) {
		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		JsonObject page = facebookClient.fetchObject("/me/accounts", JsonObject.class);

		JsonObject pageAccount = page.getJsonArray("data").getJsonObject(0);

		System.out.println(pageAccount.getString("access_token"));

		PageAccounts pageAcc = createPageAccountObj(pageAccount);

		return pageAcc;

	}

	private PageAccounts createPageAccountObj(JsonObject page) {
		PageAccounts account = new PageAccounts();
		account.setAccess_token(page.getString("access_token"));
		account.setId(page.getString("id"));
		account.setCategory(page.getString("category"));
		account.setName(page.getString("name"));

		return account;
	}

	public Page getPageInfo(String token) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Page page = facebookClient.fetchObject("/me", Page.class, Parameter.with("fields", DataUrls.mePage));

		return page;
	}

	@Async
	public Future<FacebookPageModel> getPageAll(String token) throws Exception {
		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Connection<Post> myFeed = facebookClient.fetchConnection("me/feed", Post.class, Parameter.with("limit",100));

		FacebookPageModel fbUserModel = new FacebookPageModel();

		for (List<Post> myFeedConnectionPage : myFeed) {

			for (Post post : myFeedConnectionPage) {
				// create custom fb object
				FbPost fbPost = createFbPostObject(post);
				// get all likes and Commment for a specific
				populatePostCommentsAndLikes(token,fbPost);
				// add post to fbModel
				fbUserModel.getPost().add(fbPost);
			}
		}

		// writeToFile(fbUserModel);
		FacebookData fbData = new FacebookData();
		fbData.setType("FEED");
		fbData.setFacebookPage(fbUserModel);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(fbData);
		System.out.println(json);

		// produce to kafka
		kafkaProducer.publishUpdates(json, "rsmsfb");

		return new AsyncResult<FacebookPageModel>(fbUserModel);
	}

	@Override
	public List<Post> getPageFeed(String token) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Connection<Post> myFeed = facebookClient.fetchConnection("me/feed", Post.class, Parameter.with("limit", 300));

		List<Post> allFeeds = new ArrayList<Post>();

		for (List<Post> myFeedConnectionPage : myFeed) {
			allFeeds.addAll(myFeedConnectionPage);
		}

		return allFeeds;
	}

	@Override
	public List<Comment> getPostCommentsAndLikes(String token, String postID) {
		if (facebookClient == null) {
			facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);
		}

		List<Comment> allPostComment = new ArrayList<Comment>();

		Connection<Comment> allComments = facebookClient.fetchConnection(postID + "/comments", Comment.class,
				Parameter.with("fields", DataUrls.commentData));
		for (List<Comment> postcomments : allComments) {
			allPostComment.addAll(postcomments);
		}

		return allPostComment;
	}

	public List<Event> getPageEvents(String token) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		List<Event> allPageEvents = new ArrayList<Event>();

		Connection<Event> allEvents = facebookClient.fetchConnection("me/events", Event.class);

		for (List<Event> pageEvent : allEvents) {
			allPageEvents.addAll(pageEvent);
		}

		return allPageEvents;

	}

	public List<Post> getEventFeed(String token, String eventID) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Connection<Post> myFeed = facebookClient.fetchConnection(eventID + "/feed", Post.class,
				Parameter.with("limit", 300));

		List<Post> allFeeds = new ArrayList<Post>();

		for (List<Post> myFeedConnectionPage : myFeed) {
			allFeeds.addAll(myFeedConnectionPage);
		}

		return allFeeds;

	}

	public Event getEventDetails(String token, String eventID) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Event event = facebookClient.fetchObject("/" + eventID, Event.class,
				Parameter.with("fields", DataUrls.meEvent));

		return event;
	}

	/**
	 * 
	 * @param post
	 * @return
	 */
	private FbPost createFbPostObject(Post post) {
		FbPost fbPost = new FbPost();

		fbPost.setCaption(post.getCaption());
		fbPost.setComment_count(post.getCommentsCount());
		fbPost.setCreatedTime(post.getCreatedTime());
		fbPost.setDescription(post.getDescription());
		fbPost.setFrom(post.getFrom() == null ? " " : post.getFrom().getName());
		fbPost.setUserId(post.getFrom() == null ? " " : post.getFrom().getId());
		fbPost.setUserName(post.getFrom() == null ? " " : post.getFrom().getName());
		fbPost.setId(post.getId());
		fbPost.setLikes_count(post.getLikesCount() == null ? 0 : post.getLikesCount());
		fbPost.setLink(post.getLink());
		fbPost.setMessage(post.getMessage());
		fbPost.setMetadata(post.getMetadata() == null ? " " : post.getMetadata().toString());
		fbPost.setReactions(post.getReactions() == null ? " " : post.getReactions().getViewerReaction());
		fbPost.setShare_count(post.getSharesCount());
		fbPost.setStory(post.getStory());

		return fbPost;
	}

	/**
	 * 
	 * @param post
	 */
	private void populatePostCommentsAndLikes(String token,FbPost post) {

		List<Comment> comments = getPostCommentsAndLikes(token, post.getId());

		for (Comment comment : comments) {
			FbComment fbComment = new FbComment();
			fbComment.setComment_count(comment.getCommentCount());
			fbComment.setCreatedTime(comment.getCreatedTime());
			fbComment.setId(comment.getId());
			fbComment.setLikes_count(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
			fbComment.setMessage(comment.getMessage());
			fbComment.setUserId(comment.getFrom() == null ? " " : comment.getFrom().getName());
			fbComment.setUserName(comment.getFrom() == null ? " " : comment.getFrom().getId());
			
			
			if(comment.getLikes() != null){
				createFbLikeObj(fbComment, comment);
			}
			
			if (comment.getComments() != null) {
				createFbCommentObj(fbComment, comment);
			}
			post.getComments().add(fbComment);
		}

		List<NamedFacebookType> likes = getPostLike(token,post);

		for (NamedFacebookType like : likes) {
			FbLike fblike = new FbLike();
			fblike.setUserId(like.getId());
			fblike.setUserName(like.getName());
			post.getLikes().add(fblike);
		}

	}
	
	private List<NamedFacebookType> getPostLike(String token,FbPost post){
		
        List<NamedFacebookType> allPostLike = new ArrayList<NamedFacebookType>();
		
        if(facebookClient==null){
        	facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);
        }
		

		Connection<NamedFacebookType> allLikes = facebookClient.fetchConnection(post.getId()+"/likes", NamedFacebookType.class);

		for (List<NamedFacebookType> postlike : allLikes) {

			allPostLike.addAll(postlike);
		}
		
		return allPostLike;
	}

	private void createFbLikeObj(FbComment parentfbComment,Comment originalComment){
		List<NamedFacebookType> likes=originalComment.getLikes().getData();
		
		for(NamedFacebookType user : likes){
			FbLike like= new FbLike();
			like.setUserName(user.getName());
			like.setUserId(user.getId());
			parentfbComment.getLikes().add(like);
		}
	}
	private void createFbCommentObj(FbComment parentfbComment, Comment originalComment) {

		List<Comment> comments = originalComment.getComments().getData();

		for (Comment comment : comments) {
			FbComment fbComment = new FbComment();
			fbComment.setComment_count(comment.getCommentCount());
			fbComment.setCreatedTime(comment.getCreatedTime());
			fbComment.setId(comment.getId());
			fbComment.setLikes_count(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
			fbComment.setMessage(comment.getMessage());
			fbComment.setUserId(comment.getFrom() == null ? " " : comment.getFrom().getName());
			fbComment.setUserName(comment.getFrom() == null ? " " : comment.getFrom().getId());
			parentfbComment.getComments().add(fbComment);
		}
	}

	private void writeToFile(FacebookPageModel model) {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(model);
		System.out.println(json);

		try {
			gson.toJson(model, new FileWriter("D:\\Activiti\\facebookModel.txt"));
		} catch (JsonIOException | IOException e) {
			e.printStackTrace();
		}
	}

}
