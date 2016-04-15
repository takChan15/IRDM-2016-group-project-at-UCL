package uk.ac.ucl;

import java.util.ArrayList;

/**
 * Represents a product
 * @author taklumbo
 *
 */
public class Product {
	String Id;
	String Title;
	String Description;
	ArrayList<Attribute> Attributes;
	
	public Product(String id, String title, String description, ArrayList<Attribute> attributes){
		Id = id;
		Title = title;
		Description = description;
		Attributes = attributes;
	}
	
	public Product(){}
}
