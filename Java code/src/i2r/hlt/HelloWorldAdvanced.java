package i2r.hlt;

/** A little more advanced introduction to Java.
 * 
 * @author Parth Gupta
 *
 */
public class HelloWorldAdvanced {
	public static void main(String[] args) {
		HelloWorldAdvanced hwa = new HelloWorldAdvanced();
		
		// to declare int, double, float, String variables.
		int a = 10;
		double b = 1.0;
		float c = 1;
		String s = "terrier"+"-tut"; // String concatenation
		
		// to print something
		System.out.println("Hello World!");
		
		// to call a user defined function
		hwa.myFunction("Hello World!!");
		hwa.myFunction(s);
		
		double res = hwa.add(a, b);
		System.out.println("The sum of "+a+" and " + b + " is = " + res);
		
		
	}
	
	public void myFunction(String s) {
		System.out.println("In Function: " + s);
	}
	
	public double add(int a, double b) {
		double result = 0.0;
		result = a + b;
		return result;
	}
}