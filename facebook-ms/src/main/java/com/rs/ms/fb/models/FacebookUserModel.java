package com.rs.ms.fb.models;

import java.util.ArrayList;
import java.util.List;


public class FacebookUserModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String userId;
	private String leadId;
	
	//private User userInfo;
	
	private List<FbPost> post;

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the leadId
	 */
	public String getLeadId() {
		return leadId;
	}

	/**
	 * @param leadId the leadId to set
	 */
	public void setLeadId(String leadId) {
		this.leadId = leadId;
	}

	/**
	 * @return the post
	 */
	public List<FbPost> getPost() {
		
		if(post==null){
			post= new ArrayList<>();
		}
		
		return post;
	}

	/**
	 * @param post the post to set
	 */
	public void setPost(List<FbPost> post) {
		this.post = post;
	}

	/**
	 * @return the userInfo
	 */
//	public User getUserInfo() {
//		return userInfo;
//	}

	/**
	 * @param userInfo the userInfo to set
	 */
//	public void setUserInfo(User userInfo) {
//		this.userInfo = userInfo;
//	}
}
