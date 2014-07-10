import java.io.*;
import java.util.*;

import manticore.dl.*;



class EITool {

	public static void main ( String [] args ) {

		System.out.println("Hello world!");
		String filename = args[0];

		System.out.println("My argument is: " + filename);

		try {
			Lexer eiLexer = new Lexer( new FileReader( args[0] ) );
			YYParser eiParser = new YYParser( eiLexer );
			eiParser.parse();

			System.out.println("The control envelope is: " + eiParser.envelope.toKeYmaeraString() );
			System.out.println("The invariant is: " + eiParser.invariant.toKeYmaeraString() );
			System.out.println("The robust parameters are: " + eiParser.robustparameters.toKeYmaeraString() );
			System.out.println("The control law is: " + eiParser.controllaw.toKeYmaeraString() );
		} catch ( Exception e ) {
			e.printStackTrace();
		}


	}


}
