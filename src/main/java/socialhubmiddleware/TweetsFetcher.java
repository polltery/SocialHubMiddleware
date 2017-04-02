package socialhubmiddleware;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class TweetsFetcher implements Callable{

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		
		String username = (String) eventContext.getMessage().getInvocationProperty("username");
		MongoManager mm = new MongoManager();

		BasicDBObject output = new BasicDBObject();
		
		if (true) {//do token checks in this if. (don't need username checks)
			TwitterRequestManager twitterRequestManager = new TwitterRequestManager();
			JSONArray jsonArray = twitterRequestManager.getUserTimeline(username, "3");
			if (jsonArray.size()==0){
				boolean success = false;
				String message = "No tweets made by this account";
				output.put("success", success);
				output.put("message", message);
			} else {
				
				//200 response
				ArrayList<TweetPost> tweetsList = parseTweets(jsonArray);			
				boolean success = true;
				output.put("success", success);
				//BasicDBObject data = new BasicDBObject("data", tweetsList);
				BasicDBList data = new BasicDBList();
				data.addAll(tweetsList);
//				ArrayList tags = new ArrayList();
//				tags.add("tag1");
//				tags.add("tag2");
//				BasicDBObject data = new BasicDBObject("data", tags);
				
				output.append("data", data);
			}
		} else {
			boolean success = false;
			String message = "Invalid token | user doens't exists";
			output.put("success", success);
			output.put("message", message);
		}
		
		
		
		mm.closeMongoConnection();
		///////////////////////////////////testing
/*		output = new BasicDBObject();
		boolean success = false;
		String message = "Invalid token | user doens't exists";
		output.put("success", success);
		output.put("message", message);
	*/	//////////testing
		return output/*.toString()*/;
	}
	
	public ArrayList<TweetPost> parseTweets(JSONArray jsonArray) {
		ArrayList<TweetPost> tweetsList = new ArrayList<TweetPost>();

		for (int i = 0; i < jsonArray.size(); i++) {
			TweetPost tweetPost = new TweetPost();
			JSONObject jTweet = (JSONObject) jsonArray.get(i);
			JSONObject jUserInfo = (JSONObject) jTweet.get("user");
			tweetPost.user = (String) jUserInfo.get("name");
			tweetPost.tweet = (String) jTweet.get("text");
			tweetPost.timestamp = (String) jTweet.get("created_at");
			try {
				final String TWITTER="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
				SimpleDateFormat sf = new SimpleDateFormat(TWITTER);
				sf.setLenient(true);
				Date date = sf.parse(tweetPost.timestamp);
				tweetPost.timestamp = new SimpleDateFormat("MMM dd HH:mm yyyy").format(date);
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			} 
			// for tweetPost.type
			JSONObject jEntities = (JSONObject) jTweet.get("entities");
			if (jEntities != null && jEntities.containsKey("media")) {
				JSONArray jMedia = (JSONArray) jEntities.get("media");
				tweetPost.type = (String) ((JSONObject) jMedia.get(0)).get("type");
			}
			JSONObject jExtendedEntities = (JSONObject) jTweet.get("extended_entities");
			if (jExtendedEntities != null && jExtendedEntities.containsKey("media")) {
				JSONArray jMedia = (JSONArray) jExtendedEntities.get("media");
				tweetPost.sourceType = (String) ((JSONObject) jMedia.get(0)).get("type");
				tweetPost.sourceUrl = (String) ((JSONObject) jMedia.get(0)).get("media_url");
			}
			if (tweetPost.type == null) {
				tweetPost.type = "text";
			}
			
			//for tweetPost.likes
			tweetPost.likes = (int) Math.toIntExact((long)jTweet.get("favorite_count"));
			JSONObject jRetweet= (JSONObject) jTweet.get("retweeted_status");
			if (jRetweet != null && jRetweet.containsKey("favorite_count")) {
				//update likes
				tweetPost.likes = (int) Math.toIntExact((long)jRetweet.get("favorite_count"));
				//update retweets
				tweetPost.retweets = (int) Math.toIntExact((long)jRetweet.get("retweet_count"));
			}
			
			tweetsList.add(tweetPost);
		}
		return tweetsList;
	}

}
