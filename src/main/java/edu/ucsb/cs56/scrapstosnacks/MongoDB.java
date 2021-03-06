package edu.ucsb.cs56.scrapstosnacks;

/*
 * Some portions:
 * Copyright (c) 2017 ObjectLabs Corporation
 * Distributed under the MIT license - http://opensource.org/licenses/MIT
 *
 * Written with mongo-3.4.2.jar
 * Documentation: http://api.mongodb.org/java/
 * A Java class connecting to a MongoDB database given a MongoDB Connection URI.
 */

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import static spark.Spark.port;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;
import java.util.Arrays;
import com.mongodb.Block;

import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;


public class MongoDB {

	/**
	  return a HashMap with values of all the environment variables
	  listed; print error message for each missing one, and exit if any
	  of them is not defined.
	  */
	public static ArrayList<String> ingredients = new ArrayList<String>();
	public static HashMap<String,String> getNeededEnvVars(String [] neededEnvVars) {

		ProcessBuilder processBuilder = new ProcessBuilder();

		HashMap<String,String> envVars = new HashMap<String,String>();

		boolean error=false;		
		for (String k:neededEnvVars) {
			String v = processBuilder.environment().get(k);
			if ( v!= null) {
				envVars.put(k,v);
			} else {
				error = true;
				System.err.println("Error: Must define env variable " + k);
			}
		}

		if (error) { System.exit(1); }

		System.out.println("envVars=" + envVars);
		return envVars;	 
	}

	public static String mongoDBUri(HashMap<String,String> envVars) {

		System.out.println("envVars=" + envVars);
		//string will look like:
		// mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
		String uriString = "mongodb://" +
			envVars.get("MONGODB_USER") + ":" +
			envVars.get("MONGODB_PASS") + "@" +
			envVars.get("MONGODB_HOST") + ":" +
			envVars.get("MONGODB_PORT") + "/" +
			envVars.get("MONGODB_NAME");
		System.out.println("uriString=" + uriString);
		return uriString;
	}

	static void initDatabase(String uriString) {
		try {
			MongoDB.initCollection(uriString);
		} catch (UnknownHostException e) {
			System.out.println("Unknown Host thrown");
		}
	}

	static String makeString(ArrayList<String> text) {
		String resultString = "";
		for (String s: text) {
			resultString += "<b> " + s + "</b><br/>";
		}
		return resultString;
	}

	public static void getForm(String s) {
		ingredients.add(s);
	}


	public static void initCollection (String uriString) throws UnknownHostException {
		MongoDatabase db = connect_database(uriString); //get database access
		MongoCollection<Document> songs = db.getCollection("recipes");

		disconnect_database(uriString);

	}
	public static MongoDatabase connect_database(String uriString)
	{
		MongoClientURI uri  = new MongoClientURI(uriString); 
		MongoClient client = new MongoClient(uri);
		MongoDatabase db = client.getDatabase(uri.getDatabase());
		return db;
	}
	public static void disconnect_database(String uriString)
	{
		MongoClientURI uri  = new MongoClientURI(uriString); 
		MongoClient client = new MongoClient(uri);
		client.close();
	}
	public static void add_document(String uriString,Document a)
	{
		MongoDatabase db = MongoDB.connect_database(uriString);
		MongoCollection<Document> songs = db.getCollection("recipes");
		songs.insertOne(a);
	}
	public static Document createOne(ArrayList<String> t)
	{
		ArrayList temp = new ArrayList();
		for(int i=1;i<t.size();i++)
		{
			temp.add(t.get(i));
		}
		return (new Document(
					"recipename",t.get(0))
				.append("ingredient",temp)
				.append("ready","y")
		       );
	}


	public static ArrayList<String> display_all (String uriString)
	{
		MongoDatabase db = MongoDB.connect_database(uriString);
		MongoCollection<Document> songs = db.getCollection("recipes");
		Document findQuery = new Document("ready","y");
		MongoCursor<Document> cursor = songs.find(findQuery).iterator();
		ArrayList<String> result = new ArrayList<String>();
		try 
		{
			while (cursor.hasNext()) 
			{
				Document doc = cursor.next();
				String recipe = "Recipe name: "+ doc.get("recipename"); 
				recipe +=  " Ingredients: ";
				for(String s:(ArrayList<String>)(doc.get("ingredient")))
				{
					recipe += s + ", ";
				}
				result.add(recipe);
			}
		}
		finally
		{
			cursor.close();
		}
		return result;
	}
	public static ArrayList<String> searchByName(String uriString,String recipe_name)
	{
		MongoDatabase db = MongoDB.connect_database(uriString);
		MongoCollection<Document> songs = db.getCollection("recipes");
		ArrayList<String> result = new ArrayList<String>();
		Document findQuery = new Document("recipename",recipe_name);
		MongoCursor<Document> cursor = songs.find(findQuery).iterator();
		try 
		{
			while (cursor.hasNext()) 
			{
				Document doc = cursor.next();
				String recipe = "Recipe name: "+ doc.get("recipename");
				recipe += " Ingredients: ";
				for(String s:(ArrayList<String>)(doc.get("ingredient")))
				{
					recipe +=  s + ", ";
				}
				result.add(recipe);
			}
		}
		finally
		{
			cursor.close();
		}
		return result;
	}

	public static ArrayList<String> searchByIngredients(String uriString, String ingredients)
	{

		MongoDatabase db = MongoDB.connect_database(uriString);
		MongoCollection<Document> songs = db.getCollection("recipes");
		ArrayList<String> result = new ArrayList<String>();
		List<String> i = Arrays.asList(ingredients.split(","));
		Document findQuery = new Document("ready", "y");
		MongoCursor<Document> cursor = songs.find(findQuery).iterator();
		try
		{
			while (cursor.hasNext())
			{
				Document doc = cursor.next();
				String recipe = "Recipe name: "+ doc.get("recipename");
				boolean match_recipe = false;
				for(int k=0;k<i.size();k++)
				{
					for(String s:(ArrayList<String>)(doc.get("ingredient")))
					{
						
						if((i.get(k).equals(s)))
						{
							match_recipe = true;	
							break;
						}
					}
				}
				if(match_recipe)
				{
					result.add(recipe);
				}
			}	
		}
		finally
		{
			cursor.close();
		}
		return result;


	}

}
