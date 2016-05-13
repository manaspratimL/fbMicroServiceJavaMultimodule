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
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.Comment;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.Page;
import com.restfb.types.Post;
import com.restfb.types.User;
import com.rs.ms.fb.kafka.KafkaProducerService;
import com.rs.ms.fb.models.FacebookConfigModel;
import com.rs.ms.fb.models.FacebookData;
import com.rs.ms.fb.models.FacebookPageModel;
import com.rs.ms.fb.models.FacebookUserModel;
import com.rs.ms.fb.models.FbComment;
import com.rs.ms.fb.models.FbLike;
import com.rs.ms.fb.models.FbPost;
import com.rs.ms.fb.models.PageAccounts;
import com.rs.ms.fb.service.base.FacebookClientService;
import com.rs.ms.fb.service.base.FacebookPageDataService;
import com.rs.ms.fb.service.base.FacebookUserDataService;
import com.rs.ms.fb.utils.DataUrls;

/**
 * 
 * @author ManasC
 *
 */
@Service
public class FacebookUserDataServiceImpl implements FacebookUserDataService {
	

	@Autowired
    FacebookClientService facebookClientService;

	@Autowired
    KafkaProducerService kafkaProducer;
	
	@Autowired
	FacebookPageDataService fbPageDataService;
	
	private FacebookClient facebookClient;
	
	@Override
	@Async
	public void processUser(FacebookConfigModel config) throws Exception {
		//AccessToken token=getLongLivedAccessToken(config.getToken());		
		
		/*User user=getUserInfo(token.getAccessToken());
		
		Future<FacebookUserModel> future=getUserAll(token.getAccessToken(),config.getFacebookId());
		
		PageAccounts pageAccount=fbPageDataService.getPageAccounts(token.getAccessToken());
		
		Page page=fbPageDataService.getPageInfo(pageAccount.getAccess_token());
		
		fbPageDataService.subscribePage(pageAccount.getAccess_token(), page.getId());
		
		Future<FacebookPageModel> page1=fbPageDataService.getPageAll(pageAccount.getAccess_token());*/
		
        User user=getUserInfo(config.getToken());
		
		Future<FacebookUserModel> future=getUserAll(config.getToken(),config.getFacebookId());
		
		PageAccounts pageAccount=fbPageDataService.getPageAccounts(config.getToken());
		
		Page page=fbPageDataService.getPageInfo(pageAccount.getAccess_token());
		
		//fbPageDataService.subscribePage(pageAccount.getAccess_token(), page.getId());
		
		Future<FacebookPageModel> page1=fbPageDataService.getPageAll(pageAccount.getAccess_token());
	}
	
	private AccessToken getLongLivedAccessToken(String token){
		
		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		AccessToken accessToken =
				facebookClient.obtainExtendedAccessToken("230136383995486",
				    "f74ac6f816ff3e2d9da35ef18447d9c8", token);

				System.out.println("My extended access token: " + accessToken);
				
		return accessToken;
	}

	@Override
	public User getUserInfo(String token) {

		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		User user = facebookClient.fetchObject("/me", User.class, Parameter.with("fields", DataUrls.me));

		return user;
	}
	
	
	@Async
	public Future<FacebookUserModel> getUserAll(String token,String fbUserID) throws Exception {
		
		int limit=1;
		
		String userId=Usertokens.get(fbUserID);
		
		if(userId==null){
			Usertokens.put(fbUserID,token);
		}
		
		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Connection<Post> myFeed = facebookClient.fetchConnection("me/feed", Post.class, Parameter.with("limit",10));


		FacebookUserModel fbUserModel = new FacebookUserModel();
		int i=0;
		for (List<Post> myFeedConnectionPage : myFeed) {

			for (Post post : myFeedConnectionPage) {
				//create custom fb object
				FbPost fbPost = createFbPostObject(post);
				//get all likes and Commment for a specific 
				populatePostCommentsAndLikes(token,fbPost);
				//add post to fbModel
				fbUserModel.getPost().add(fbPost);
			}
			
			i++;
			if(i>=limit){
				break;
			}
		}
		
		//writeToFile(fbUserModel);
		//return fbUserModel;
		FacebookData fbData= new FacebookData();
		fbData.setType("FEED");
		fbData.setFacebookUser(fbUserModel);
		
		 Gson gson = new GsonBuilder().setPrettyPrinting().create();
		 
		 String json = gson.toJson(fbData);
	     System.out.println(json);
		
		//produce to kafka
		kafkaProducer.publishUpdates(json, "rsmsfb");
		
		return new AsyncResult<FacebookUserModel>(fbUserModel);
	}

	@Override
	public List<Post> getUserFeed(String token,int limitValue,boolean pagination) {


		facebookClient = facebookClientService.getFacebookClient(token, Version.VERSION_2_6);

		Connection<Post> myFeed = facebookClient.fetchConnection("me/feed", Post.class, Parameter.with("limit",limitValue));
		
		List<Post> allFeeds = new ArrayList<Post>();
	
		if(pagination){
		 for (List<Post> myFeedConnectionPage : myFeed) {
			 allFeeds.addAll(myFeedConnectionPage);
		}
		}else{
			allFeeds.addAll(myFeed.getData());
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
		fbPost.setFrom(post.getFrom()==null?" ":post.getFrom().getName());
		fbPost.setUserId(post.getFrom()==null?" ":post.getFrom().getId());
		fbPost.setUserName(post.getFrom()==null?" ":post.getFrom().getName());
		fbPost.setId(post.getId());
		fbPost.setLikes_count(post.getLikesCount()==null?0:post.getLikesCount());
		fbPost.setLink(post.getLink());
		fbPost.setMessage(post.getMessage());
		fbPost.setMetadata(post.getMetadata()==null?" ":post.getMetadata().toString());
		fbPost.setReactions(post.getReactions()==null?" ":post.getReactions().getViewerReaction());
		fbPost.setShare_count(post.getSharesCount());
		fbPost.setStory(post.getStory());

		return fbPost;
	}

	/**
	 * 
	 * @param post
	 */
	private void populatePostCommentsAndLikes(String token,FbPost post) {
		
		List<Comment> comments=getPostCommentsAndLikes(token,post.getId());
		
		for(Comment comment:comments){
			FbComment fbComment= new FbComment();
			fbComment.setComment_count(comment.getCommentCount());
			fbComment.setCreatedTime(comment.getCreatedTime());
			fbComment.setId(comment.getId());
			fbComment.setLikes_count(comment.getLikeCount()==null?0:comment.getLikeCount());
			fbComment.setMessage(comment.getMessage());
			fbComment.setUserId(comment.getFrom()==null?" ":comment.getFrom().getId());
			fbComment.setUserName(comment.getFrom()==null?" ":comment.getFrom().getName());
						
			
			if(comment.getLikes() != null){
				createFbLikeObj(fbComment, comment);
			}
			
			
			if(comment.getComments() != null){
				createFbCommentObj(fbComment, comment);
			}
			post.getComments().add(fbComment);
		}
		
		List<NamedFacebookType> likes=getPostLike(token,post);
		
		for(NamedFacebookType like : likes){
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
	
	private void createFbCommentObj(FbComment parentfbComment,Comment originalComment){
		
		List<Comment> comments=originalComment.getComments().getData();
		
		for(Comment comment:comments){
			FbComment fbComment= new FbComment();
			fbComment.setComment_count(comment.getCommentCount());
			fbComment.setCreatedTime(comment.getCreatedTime());
			fbComment.setId(comment.getId());
			fbComment.setLikes_count(comment.getLikeCount()==null?0:comment.getLikeCount());
			fbComment.setMessage(comment.getMessage());
			fbComment.setUserId(comment.getFrom()==null?" ":comment.getFrom().getId());
			fbComment.setUserName(comment.getFrom()==null?" ":comment.getFrom().getName());
			
			if(comment.getLikes() != null){
				createFbLikeObj(fbComment, comment);
			}
			
			parentfbComment.getComments().add(fbComment);
		}
	}

 private void writeToFile(FacebookUserModel model){
	 
	 Gson gson = new GsonBuilder().setPrettyPrinting().create();
	   
	 try {
		gson.toJson(model, new FileWriter("D:\\Activiti\\facebookUserModel.json"));
	} catch (JsonIOException | IOException e) {
		e.printStackTrace();
	}

 }
}
