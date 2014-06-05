import java.io.*;
import java.util.*;



class EITool {

	public static void main ( String [] args ) {

		System.out.println("Hello world!");
		String filename = args[0];

		System.out.println("My argument is: " + filename);

		System.out.println("The control envelope is: " + Parser.parseEnvelope( filename ) );
		System.out.println("The invariant is: " + Parser.parseInvariant( filename ));
		System.out.println("The robust parameters are: " + Parser.parseRobustParameters( filename ));
		System.out.println("The control law is: " + Parser.parseControlLaw( filename ));


	}


}
