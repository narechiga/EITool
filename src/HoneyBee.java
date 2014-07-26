import java.io.*;
import java.util.*;

import manticore.dl.*;
import honeybee.mathematicakit.*;
import honeybee.drealkit.*;


class HoneyBee {

	public static void main ( String [] args ) {

		System.out.println("Hello world!");

		if ( args.length == 1 ) {
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
				
			    MathematicaInterface.writeParametricRefinementVerificationQuery(
											    hbParser.statevariables,
											    hbParser.eiparameters,
											    hbParser.envelope,
											    hbParser.invariant,
											    hbParser.robustparameters,
											    hbParser.control
											    );
			    dRealInterface dreal = new dRealInterface( 0.00001 );
			    dreal.parametricVerify(  
						hbParser.statevariables,
						hbParser.eiparameters,
						hbParser.envelope,
						hbParser.invariant,
						hbParser.robustparameters,
						hbParser.control,
						0.1);
			    				
			    //dreal.writeSingleRefinementVerificationQuery(
			    //    						  hbParser.statevariables,
			    //    						  hbParser.eiparameters,
			    //    						  hbParser.envelope,
			    //    						  hbParser.invariant,
			    //    						  hbParser.robustparameters,
			    //    						  hbParser.control
			    //    						  );
			    //  
			} else {
			    System.out.println("The control envelope is: " + hbParser.envelope.toKeYmaeraString() );
			    System.out.println("The invariant is: " + hbParser.invariant.toKeYmaeraString() );
			    System.out.println("The robust parameters are: " + hbParser.robustparameters.toKeYmaeraString() );
			    System.out.println("The control template is: " + hbParser.control.toKeYmaeraString() );
				
			    MathematicaInterface.writeParametricRefinementSynthesisQuery(
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
		} else {
		    commandLine();
		}

	}
    
    public static void commandLine() {
	
	Scanner commandScanner = new Scanner( System.in );
	while (true) {
	    try {
		System.out.print("\nfindinstance:> ");
		String input = commandScanner.nextLine();
		input = input.trim();
		//		Scanner in = new Scanner( input );
		
                StringReader inreader = new StringReader( input );
                Lexer myLexer = new Lexer( inreader );
                YYParser myParser = new YYParser( myLexer );
                myParser.parse();

                System.out.println("buzz buzz");
                if ( (myParser.parsedStructure instanceof dLFormula)  ) {
		    System.out.println( "PARSED: " + myParser.parsedStructure.toKeYmaeraString() );
		    
		    if ( myParser.parsedStructure instanceof dLFormula) {
			dLFormula myformula = (dLFormula)myParser.parsedStructure;
			if ( !myformula.isStatic() ) {
			    throw new Exception("Formula is not static!");
			} if (!myformula.isQuantifierFree() ) {
			    throw new Exception("Formula is not quantifier free!");
			}
			
			dRealInterface dreal = new dRealInterface( 0.00001 );
			Valuation myresult = dreal.findInstance( myformula );
			if ( myresult != null ) {
				System.out.println("Result is: " + myresult.toString() );
			} else {
				System.out.println("No instance found.");
			}
			
		    } else {
			throw new Exception("Input is not a logical formula");
		    }

                }

    
		// if ( in.hasNext("parse") ){
		//     in.skip("parse");
		//     runParser( in.nextLine() + "\n" );
		// } else if ( in.hasNext("evaluate") ) { 
		//     in.skip("evaluate");
		//     runEvaluate( in.nextLine() + "\n");
		// } else if ( in.hasNext("simulate") ) { 
		//     in.skip("simulate");
		//     runSimulate( in.nextLine() + "\n");
		//     //} else if ( in.hasNext("execute") ) {
		//     //      in.skip("execute");
		//     //      runExecute( in.nextLine() + "\n");
		// } else if ( in.hasNext("version") ) { 
		//     System.out.println("Manticore version 0");
		//     in.nextLine();
		// } else {
		//     runParser( in.nextLine() + "\n" );
		// }

	    } catch ( Exception e ) { 
		System.out.println("Error running parser test loop.");
		System.err.println( e );
		e.printStackTrace();
	    }
	}
    }


}
