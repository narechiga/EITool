package perseus.core;

import java.util.*;
import java.io.*;
import proteus.dl.parser.*;
import proteus.dl.syntax.*;
import proteus.dl.semantics.*;
import proteus.logicsolvers.abstractions.*;

/***/
import perseus.verification.*;

/* later, refine this from proteus InterfaceCore */
public class PerseusInterfaceCore {

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

	LogicSolverInterface solver;
	RefinementVerifier verifier;
	boolean debug = false;

// Constructor
	public PerseusInterfaceCore( LogicSolverInterface solver ) {
		this.solver = solver;
		this.verifier = new RefinementVerifier( solver );
			
	}

// Allow the user to propsoe a refinement by parts
	public boolean proposeParts( VerificationProblem thisProblem ) throws Exception {
		Scanner thisScanner = new Scanner( System.in );

		// Get number of parts
		int numberOfParts = 0;
		System.out.println("How many parts?: ");
		try {
			numberOfParts = thisScanner.nextInt();
			thisScanner.nextLine();
		} catch ( Exception e ) {
			System.out.println("Could not read number of parts, exception occured");
			e.printStackTrace();
		}

		ArrayList<Valuation> theseParameters = new ArrayList<Valuation>();
		dLFormula overallInvariant = null;
		dLFormula overallEnvelope = null;
		String input;
		StringReader inReader;
		dLLexer thisLexer;
		dLParser thisParser;
		for ( int i = 0; i < numberOfParts; i ++ ) {
			System.out.println("Parameter valuation " + i + "? : ");
			input = thisScanner.nextLine().trim();

			inReader = new StringReader( input );
                	thisLexer = new dLLexer( inReader );
                	thisParser = new dLParser( thisLexer );
                	thisParser.parse();

                	if ( thisParser.valuation == null ) {
                		throw new Exception("Could not understand proposed valuation");
			}

			theseParameters.add( thisParser.valuation );

			if ( i == 0 ) { //first time around
				overallInvariant = thisProblem.invariant.substituteConcreteValuation( 
								thisParser.valuation );
				overallEnvelope = new AndFormula( thisProblem.invariant.substituteConcreteValuation(
									thisParser.valuation ),
								thisProblem.envelope.substituteConcreteValuation(
									thisParser.valuation ) );
			} else {
				overallInvariant = new OrFormula( overallInvariant,
							thisProblem.invariant.substituteConcreteValuation(
								thisParser.valuation ) );
				overallEnvelope = new OrFormula( overallEnvelope,
							thisProblem.envelope.substituteConcreteValuation(
								thisParser.valuation ) );
			}
		}

		if ( !verifier.setARobustlyCoversSetB( overallInvariant, thisProblem.domain ) ) {
			System.out.println( ANSI_BOLD + ANSI_YELLOW + "WARNING:" + ANSI_RESET 
						+ " Invariant parametrization does not cover the domain robustly");
		} else {
			System.out.println( ANSI_BOLD + ANSI_CYAN + "INFO:" + ANSI_RESET 
						+" Invariant parametrization covers domain robustly");
		}

		// Then try refinement with the overall envelope and invariant
		LogicSolverResult refinementResult = verifier.singleRefinementVerificationQuery(
				thisProblem.stateVariables,
				overallEnvelope,
				overallInvariant,
				thisProblem.control );
		System.out.println("Refinement result is: " + refinementResult.toString() );
		
		if ( refinementResult.validity.equals("valid") ) {
			System.out.println( ANSI_BOLD + ANSI_GREEN + "Refinement successful!" + ANSI_RESET);
			return true;

		} else {//update parameter formula to try a different point
			System.out.println("delta-counterexample: " + refinementResult.valuation.toMathematicaString() );
			
			System.out.println("Refinement " + ANSI_BOLD + ANSI_RED +"not" 
						+ ANSI_RESET +" succesful with " 
						+ numberOfParts + " parts, incrementing...");
			return false;
		}
	}

// Allow the user to propose a parameterization for refinement check
	public boolean proposeRefine( VerificationProblem thisProblem ) throws Exception {
		System.out.print(ANSI_BOLD + "Propose parameter valuation: " + ANSI_RESET);

		Scanner valuationScanner = new Scanner( System.in );
		String input = valuationScanner.nextLine();
		StringReader inreader = new StringReader( input );
                dLLexer myLexer = new dLLexer( inreader );
                dLParser myParser = new dLParser( myLexer );
                myParser.parse();

		Valuation parameters = myParser.valuation;

		dLFormula substitutedEnvelope = thisProblem.envelope.substituteConcreteValuation( parameters );
		dLFormula substitutedInvariant = thisProblem.invariant.substituteConcreteValuation( parameters );

		if ( !verifier.setARobustlyCoversSetB( substitutedInvariant, thisProblem.domain ) ) {
			System.out.println( ANSI_BOLD + ANSI_YELLOW + "WARNING:" + ANSI_RESET 
						+ " Invariant parametrization does not cover the domain robustly");
		} else {
			System.out.println( ANSI_BOLD + ANSI_CYAN + "INFO:" + ANSI_RESET 
						+" Invariant parametrization covers domain robustly");
		}

		LogicSolverResult refinementResult = verifier.singleRefinementVerificationQuery(
									thisProblem.stateVariables,
									substitutedEnvelope,
									substitutedInvariant,
									thisProblem.control
									);

		System.out.println("Refinement result is: " + refinementResult.toString() );
		if ( refinementResult.validity.equals("valid") ) {
			return true;
		} else {
			System.out.println("delta-counterexample is: " 
				+ refinementResult.valuation.toMathematicaString() );
			return false;
		}
	}

// Automatically search for refinement parameters
	public boolean autoRefine( VerificationProblem thisProblem ) throws Exception {
		if( verifier == null ) {
			System.out.println("I don't have a verifier!");
		}



		Valuation witness = verifier.parametricVerify( thisProblem.stateVariables,
							thisProblem.eiParameters,
							thisProblem.envelope,
							thisProblem.invariant,
							thisProblem.robustParameters,
							thisProblem.domain,
							thisProblem.control,
							1.0 );

		if ( witness.isEmpty() ) {
			return false;
		} else {
			return true;
		}

	}
	
// Automatically try to refine by parts	
	public void autoParts( VerificationProblem thisProblem ) throws Exception {
		verifier.parametricVerifyByParts( thisProblem.stateVariables,
					thisProblem.eiParameters,
					thisProblem.envelope,
					thisProblem.invariant,
					thisProblem.robustParameters,
					thisProblem.domain,
					thisProblem.control,
					0.1 );
	}

// Load a verification problem statement from a file
	public VerificationProblem loadFromFile( String input ) throws Exception {
		System.out.println(ANSI_YELLOW + ANSI_BOLD + "NOTE: " + ANSI_RESET + "only verification queries are fully supported at this time");

		System.out.println("Loading " + input + " (...)");
		dLLexer thisLexer = new dLLexer( new FileReader( input ) );
		dLParser thisParser = new dLParser( thisLexer );
		thisParser.parse();
		
		if ( thisParser.synthesis ) {
			throw new Exception(ANSI_BOLD + ANSI_RED + "Sorry, synthesis queries are not currently supported" + ANSI_RESET );
		}

		VerificationProblem thisProblem = new VerificationProblem( 
									thisParser.statevariables,
									thisParser.eiparameters,
									thisParser.envelope,
									thisParser.invariant,
									thisParser.robustparameters,
									thisParser.domain,
									thisParser.control
									);
		return thisProblem;
	}

// Just parse a file
	public void runParser( String input ) throws Exception {
		dLLexer thisLexer = new dLLexer( new StringReader( input ) );
		dLParser thisParser = new dLParser( thisLexer );
		thisParser.parse();
		System.out.println("PARSED: " + thisParser.parsedStructure.toMathematicaString() );
	}

// Search for a satisfying instance
	public void findInstance( String input ) throws Exception {
                StringReader inreader = new StringReader( input );
                dLLexer myLexer = new dLLexer( inreader );
                dLParser myParser = new dLParser( myLexer );
                myParser.parse();

                if ( (myParser.parsedStructure instanceof dLFormula)  ) {
			dLFormula myformula = (dLFormula)myParser.parsedStructure;
			if ( !myformula.isStatic() ) {
				throw new Exception("Formula is not static!");
			} if (!myformula.isQuantifierFree() ) {
				throw new Exception("Formula is not quantifier free!");
			}
		
			Valuation myresult = solver.findInstance( myformula ).valuation;

			if ( !myresult.isEmpty() ) {
				System.out.println("Instance is: " + myresult.toString() );
			} else {
				System.out.println("No instance found.");
			}
		    
		} else {
			throw new Exception("Input is not a logical formula");
		}
	}

// give help
	public void giveHelp() {
		System.out.println("load <filename>:          loads a problem file");
		System.out.println("print                     prints loaded problem instance, if any");
		System.out.println("auto-refine:              attempts automatic refinement on the loaded file");
		System.out.println("auto-parts:               attempts automatic refinement by parts on the loaded file");
		System.out.println("propose-refine:           attempts refinement with user-specified parameter valuation");
		System.out.println("propose-parts:            attempts refinement by parts with user-specified parameter valuations");
		System.out.println("find-instance:            finds a valuation of variables that (delta) satisfies the given formuls");
		System.out.println("clear:                    forgets loaded file");
		System.out.println("version:                  prints version information");
		System.out.println("help:                     prints this help message");
		System.out.println("exit:                     exits the program");

	}

// print version
	public void printVersion() {
		System.out.println(":: Perseus version 0 ::");
		System.out.println("Copyright 2014 Nikos Arechiga");
 
		System.out.println("Licensed under the Apache License, Version 2.0 (the \"License\");");
		System.out.println("you may not use this program except in compliance with the License.");
		System.out.println("You may obtain a copy of the License at");

		System.out.println("http://www.apache.org/licenses/LICENSE-2.0");

		System.out.println("Unless required by applicable law or agreed to in writing, software");
		System.out.println("distributed under the License is distributed on an \"AS IS\" BASIS,");
		System.out.println("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
		System.out.println("See the License for the specific language governing permissions and");
		System.out.println("limitations under the License.");
	}

}
