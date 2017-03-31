/*
 * Author(s) : Azeem, Balraj
 * Date : 21/03/2017
 */

package socialhubmiddleware;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateUtils;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class MongoManager {
	MongoClient mongoClient = null;
	DBCollection usersCollection = null;
	
	public MongoManager(){
		try {
			mongoClient = new MongoClient( "localhost" , 27017 );
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		DB db = mongoClient.getDB("socialHub");
		usersCollection= db.getCollection("users");
	}
	
	public boolean checkIfEmailExists(String email){
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		DBCursor cursor = usersCollection.find(searchQuery);
		boolean exists =false;
		while (cursor.hasNext()) {
			exists = true;
			break;
		}
		return exists;
	}
	
	public boolean checkIfUsernameExists(String username){
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("username", username);
		DBCursor cursor = usersCollection.find(searchQuery);
		boolean exists =false;
		while (cursor.hasNext()) {
			exists = true;
			break;
		}
		return exists;
	}
	
	public String getPassword(String username){
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("username", username);
		DBCursor cursor = usersCollection.find(searchQuery);
		if (cursor.hasNext()) {
			return cursor.next().get("password").toString();
		}
		return null;
	}
	
	public boolean checkIfUsernameExists(String username, String password){
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("username", username);
		searchQuery.put("password", Hasher.hashPass(password));
		DBCursor cursor = usersCollection.find(searchQuery);
		boolean exists =false;
		if (cursor.length()>0) {
			exists = true;
		}
		return exists;
	}
	
	public String addUser(BasicDBObject document){
		
		// hash the password
		document.replace("password", Hasher.hashPass((String) document.get("password")));
		
		// Create a new DB object for userDetails
		BasicDBObject detailsDocument = new BasicDBObject();
		
		// Add properties to detailsDocument
		detailsDocument.append("description","You have not added any description yet.");
		detailsDocument.append("profilePictureUrl", "https://d30y9cdsu7xlg0.cloudfront.net/png/138926-200.png");
		detailsDocument.append("theme","default");
		detailsDocument.append("hasTwitterAccess", false);
		detailsDocument.append("hasInstagramAccess", false);
		
		// Add properties to document
		document.append("details",detailsDocument);
		document.append("token", "");
		document.append("tokenExpiry", "");
		
		// Debug
		System.out.println(document.toString());
		System.out.println();
		
		// Insert into mongoDB
		Object obj = usersCollection.insert(document);
		
		// Debug
		System.out.println(document.toString());
		
		// Return added document
		return document.toString();
		
	}
	
	public void closeMongoConnection(){
		mongoClient.close();
	}

	// This method will store a token for a given user along with an expire date
	public void storeToken(String username, String token) {
		
		// Create document for update
		BasicDBObject updateDocument = new BasicDBObject();
		updateDocument.append("token",token);
		updateDocument.append("tokenExpiry", getExpiryDateForToken());
		
		// Create document for set operation append
		BasicDBObject setOperation = new BasicDBObject();
		setOperation.append("$set", updateDocument);
		
		// Create a search query
		BasicDBObject searchQuery = new BasicDBObject().append("username", username);
		
		// Update collection
		usersCollection.update(searchQuery, setOperation);
	}
	
	// The purpose of this method is to create and return an expiry date for the token
	private String getExpiryDateForToken(){
		
		// Get timezone
		TimeZone tz = TimeZone.getTimeZone("UTC");
		
		// Set date format
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		
		// Set timezone
		df.setTimeZone(tz);
		
		// Set expiry date
		Date expiry = DateUtils.addHours(new Date(), 24);
		
		// Get ISO string
		String expiryString = df.format(expiry);
		
		return expiryString;
		
	}
	
	public BasicDBObject getUserDetails(String username){
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("username", username);
		
		// Create project object
		BasicDBObject queryProjection = new BasicDBObject();
		
		// Set projection properties
		queryProjection.append("username",1);
		queryProjection.append("details",1);
		queryProjection.append("_id",0);
		
		DBCursor cursor = usersCollection.find(searchQuery,queryProjection);
		BasicDBObject dbo = (BasicDBObject) cursor.next();
		return dbo;
	}
	
public void updateUserDetails(String username, BasicDBObject userDetails){
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("$set", userDetails);	
		BasicDBObject sQuery = new BasicDBObject().append("username", username);
		usersCollection.update(sQuery, newDocument);
	}
	
}
