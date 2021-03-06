import java.io.*;
import java.util.*;

import proteus.dl.parser.*;
import proteus.dl.syntax.*;
import proteus.dl.semantics.*;

import perseus.core.*;
import proteus.logicsolvers.mathematicakit.*;
import proteus.logicsolvers.drealkit.*;


class Perseus {
	//
	// COLORS! OMG COLORS!
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_BOLD = "\u001B[1m";

	public static void main ( String [] args ) {

		if ( args.length == 1 ) {
		    String filename = args[0];

		    try {
			dLLexer hbLexer = new dLLexer( new FileReader( args[0] ) );
			dLParser hbParser = new dLParser( hbLexer );
			hbParser.parse();

			if ( !hbParser.synthesis ) {
			    System.out.println("The control envelope is: " + hbParser.envelope.toKeYmaeraString() );
			    System.out.println("The invariant is: " + hbParser.invariant.toKeYmaeraString() );
			    System.out.println("The robust parameters are: " 
			    			+ hbParser.eiparameterset.toKeYmaeraString() );
			    System.out.println("The control law is: " + hbParser.control.toKeYmaeraString() );
				
			    dRealInterface dR = new dRealInterface( 0.00001 );
			//    Valuation witnessParameters = dreal.parametricVerify(  
			//							hbParser.statevariables,
			//							hbParser.eiparameters,
			//							hbParser.envelope,
			//							hbParser.invariant,
			//							hbParser.eiParameterSetet,
			//        						hbParser.domain,
			//							hbParser.control,
			//							1);
			    //System.out.println("Witness parameters: " + witnessParameters.toString() );
			//    dR.parametricVerifyByParts(  
			//				hbParser.statevariables,
			//				hbParser.eiparameters,
			//				hbParser.envelope,
			//				hbParser.invariant,
			//				hbParser.eiParameterSetet,
			//        			hbParser.domain,
			//				hbParser.control,
			//				0.1);
			    				
			    //dreal.writeSingleRefinementVerificationQuery(
			    //    						  hbParser.statevariables,
			    //    						  hbParser.eiparameters,
			    //    						  hbParser.envelope,
			    //    						  hbParser.invariant,
			    //    						  hbParser.eiParameterSetet,
			    //    						  hbParser.control
			    //    						  );
			    //  
			} else {
			    System.out.println("The control envelope is: " + hbParser.envelope.toKeYmaeraString() );
			    System.out.println("The invariant is: " + hbParser.invariant.toKeYmaeraString() );
			    System.out.println("The robust parameters are: " + hbParser.eiparameterset.toKeYmaeraString() );
			    System.out.println("The control template is: " + hbParser.control.toKeYmaeraString() );
				
			}


		    } catch ( Exception e ) {
			e.printStackTrace();
		    }
		} else {
		    commandLine();
		}

	}
    
	public static void commandLine() {
		PerseusCommandLineInterface thisCommandline = 
				new PerseusCommandLineInterface();
					//new PerseusInterfaceCore( new dRealInterface(1)) );
					//new PerseusInterfaceCore( new MathematicaInterface()) );
		thisCommandline.run();
	}



}
