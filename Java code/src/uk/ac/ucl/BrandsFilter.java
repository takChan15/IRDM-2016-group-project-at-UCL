package uk.ac.ucl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;

public class BrandsFilter {

	public static void main(String[] args) throws IOException {
		String brandFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/Brands.txt";
		String brandsToFilter = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/brandsToFilter.txt";
		String cleanBrandFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/NewBrand.txt";
		
		LinkedHashSet<String> brandFileSet = readFile(brandFile);
		LinkedHashSet<String> filterFileSet = readFile(brandsToFilter);
		
		for(String brandToFilter:filterFileSet){
			brandFileSet.remove(brandToFilter);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(cleanBrandFile));
		for(String brand:brandFileSet){
			writer.write(brand.toLowerCase().replaceAll(",", ""));
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	
	public static LinkedHashSet<String> readFile(String file) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		while((line = reader.readLine()) != null){
			if(line.trim().equals(""))
				continue;
			set.add(line);
		}
		reader.close();
		return set;
	}

}
