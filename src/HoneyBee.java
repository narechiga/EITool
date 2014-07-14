import java.io.*;
import java.util.*;

import manticore.dl.*;
import honeybee.mathematicakit.*;


class HoneyBee {

	public static void main ( String [] args ) {

		System.out.println("Hello world!");
		String filename = args[0];

		System.out.println("My argument is: " + filename);

		try {
			Lexer hbLexer = new Lexer( new FileReader( args[0] ) );
			YYParser hbParser = new YYParser( hbLexer );
			hbParser.parse();

			if ( !hbParser.synthesis ) {
				System.out.println("The control envelope is: " + hbParser.envelope.toKeYmaeraString() );
				System.out.println("The invariant is: " + hbParser.invariant.toKeYmaeraString() );
				System.out.println("The robust parameters are: " + hbParser.robustparameters.toKeYmaeraString() );
				System.out.println("The control law is: " + hbParser.control.toKeYmaeraString() );
				
				MathematicaInterface.writeSingleRefinementVerificationQuery(
											    hbParser.statevariables,
											    hbParser.eiparameters,
											    hbParser.envelope,
											    hbParser.invariant,
											    hbParser.robustparameters,
											    hbParser.control
											    );
			} else {
				System.out.println("The control envelope is: " + hbParser.envelope.toKeYmaeraString() );
				System.out.println("The invariant is: " + hbParser.invariant.toKeYmaeraString() );
				System.out.println("The robust parameters are: " + hbParser.robustparameters.toKeYmaeraString() );
				System.out.println("The control template is: " + hbParser.control.toKeYmaeraString() );
				
				MathematicaInterface.writeSingleRefinementSynthesisQuery(
											    hbParser.statevariables,
											    hbParser.eiparameters,
											    hbParser.envelope,
											    hbParser.invariant,
											    hbParser.robustparameters,
											    hbParser.control
											    );
			}


		} catch ( Exception e ) {
			e.printStackTrace();
		}


	}


}
