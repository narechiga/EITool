package perseus.drealkit;

import java.util.*;
import java.io.*;
import manticore.dl.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;


public class dRealInterface  {

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

	protected double precision;
	boolean debug = true;

//Constructors
	// Constructor with specified precision
	public dRealInterface( double precision ) {
		this.precision = precision;
	}

	// Constructor with default precision
	public dRealInterface() {
		this.precision = 0.00001;
	}


	//public SMTResult resolve ( dLFormula thisFormula ) throws Exception {
	//    
	//    NotFormula negatedFormula = NotFormula( thisFormula );
	//    File queryFile = writeQueryFile( negatedFormula );

	//    return runQuery( queryFile );

	//}

// Takes a logical formula, uses dReal to find values of the variables that occur in the formula that delta-satisfy 
// the formula. This is delta-satisfy because dReal does delta-satisfaction and not true satisfaction
	public Valuation findInstance ( dLFormula thisFormula ) throws Exception {
		Valuation result;
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );
		File queryFile = writeQueryFile( theseFormulas );

		SMTResult dRealSays = runQuery( queryFile );

		if ( dRealSays.satisfiability.equals("unsat") ) {
			result = null;
		} else {
			result = dRealSays.valuation;
		}

		return result;
	}

// Takes an ArrayList of formulas, and uses dReal to produce a valuation that satisfies all of them
// simultaneously
	public Valuation findInstance ( ArrayList<dLFormula> theseFormulas ) throws Exception {
		Valuation result;

		File queryFile = writeQueryFile( theseFormulas );
		SMTResult dRealSays = runQuery( queryFile );

		if ( dRealSays.satisfiability.equals("unsat") ) {
			result = null;
		} else {
			result = dRealSays.valuation;
		}

		return result;

	}

// Runs dReal on a query file, written by some other function The point of this function is to allow code reuse of 
// the piece that actually invokes dReal
	protected SMTResult runQuery( File queryFile ) throws Exception {
		SMTResult result = new SMTResult();

		String precisionArgument = "--precision=" + precision;
		ProcessBuilder queryPB = new ProcessBuilder("dReal", "--model", 
								precisionArgument, queryFile.getAbsolutePath() );
		queryPB.redirectErrorStream( true );
		Process queryProcess = queryPB.start();
		BufferedReader dRealSays = new BufferedReader( new InputStreamReader(queryProcess.getInputStream()) );

		String line;
		if ( (line = dRealSays.readLine()) != null ) {
			if ( line.equals("unsat")) {
				result.satisfiability = "unsat";
				result.valuation = new Valuation();
			} else if ( line.equals("sat") ) {
				result.satisfiability = "sat";
				result.valuation = extractModel( new File( queryFile.getAbsolutePath() + ".model") );
			} else if ( line.equals("unknown") ) {
				result.satisfiability = "unknown";
				result.valuation = null;
			} else {
				throw new Exception( line );
			}
		} else {
			throw new Exception("dReal returned no output!");
		}


		return result;
	}

// Extracts a counterexample from a *.model file produced after running dReal
	public Valuation extractModel( File modelFile ) throws Exception {
		Valuation model = new Valuation();

		BufferedReader modelReader = new BufferedReader( new FileReader(modelFile) );

		modelReader.readLine();
		String line;
		while( (line = modelReader.readLine()) != null ) {

			line = line.trim();
			String[] tokens = line.split("\\s+");

			RealVariable variable = new RealVariable( tokens[0] );
			String lowerBound = tokens[2].replace("[","").replace(",","").replace("(","").replace(";","");
			String upperBound = tokens[3].replace("]","").replace(")","").replace(";","");

			if ( lowerBound.equals("-inf") && upperBound.equals("inf") ) {
				model.put(variable, new Real(42));

			} else if ( lowerBound.equals("-inf") ) {
				model.put(variable, new Real( upperBound ));

			} else if ( upperBound.equals("inf") ) {
				model.put( variable, new Real( lowerBound ));

			} else {
				model.put( variable, new Real( (Double.parseDouble(upperBound) 
									+ Double.parseDouble(lowerBound))/2 ));

			}
		}

		System.out.println("model : " + model.toString() );
		return model;
	}

// Writes a query file for a logical formula.  Note that it does not negate the formula or anything, it just writes out
// a satisfiability query for the formula that it is given
	protected File writeQueryFile( ArrayList<dLFormula> theseFormulas ) throws Exception {
		String queryString = "(set-logic QF_NRA)\n\n";

		
		// First extract the list of all the variables that occur in any of the formulas
		Iterator<dLFormula> formulaIterator = theseFormulas.iterator();
		Set<RealVariable> variables = new HashSet<RealVariable>();
		while ( formulaIterator.hasNext() ) {
			variables.addAll( formulaIterator.next().getVariables() );
		}

		// Now print the variable declarations
		queryString = queryString + "\n;; Variable declarations\n";
		RealVariable thisVariable;
		Iterator<RealVariable> variableIterator = variables.iterator();
		while ( variableIterator.hasNext() ) {
			queryString = queryString + "(declare-fun " + variableIterator.next() + " () Real )\n";
		}


		// Assert each formula
		formulaIterator = theseFormulas.iterator();
		dLFormula thisFormula;
		while ( formulaIterator.hasNext() ) {
			thisFormula = formulaIterator.next();
			if( debug ) {
				if ( thisFormula == null ) {
					System.out.println("Got a null formula!");
				} else {
					System.out.println("Currently printing out formula: " + thisFormula.toMathematicaString() );
				}
			}

			queryString = queryString + "\n;; Formula is (" + thisFormula.toMathematicaString() +")\n";
			queryString = queryString + "(assert " + thisFormula.todRealString() + " )\n";

		}

		// Print the little thing that needs to go at the end
		queryString = queryString + "\n(check-sat)\n(exit)\n";

		// Now generate the actual file
		File drealworkspacedir = new File("drealworkspace");
		if (!drealworkspacedir.exists()) {
			drealworkspacedir.mkdir();
		}
		double randomID = Math.round(Math.random());
		Date date = new Date();
		String filename = "drealworkspace/query." + date.getTime() + "." + randomID + ".smt2";
		PrintWriter queryFile = new PrintWriter( filename );
		queryFile.println(";; Automatically generated by Perseus on " + date.toString() + "\n");
		queryFile.println( queryString );
		queryFile.close();
		if( debug ) {
			System.out.println("Done writing file, writeQueryFile is returning");
		}
		return new File( filename );

	}

// Creates a formula that represents a ball at the given center with the given radius. 
// Maybe the best approach is to actually create a separate class for balls, open and closed.
// And add them to the parser? That might be a bit annoying
	public ComparisonFormula createBallExclusionFormula( Valuation center, Real radius ) throws Exception {

		ComparisonFormula ballFormula;

		Set<RealVariable> variables = center.keySet();
		Iterator<RealVariable> varIterator = variables.iterator();

		String ballString = "";
		RealVariable thisVar;
		while ( varIterator.hasNext() ) {
			thisVar = varIterator.next();
			System.out.println("\tgenerating ball for variable: " + thisVar.toMathematicaString() );

			if ( varIterator.hasNext() ) {
				ballString = ballString
						+ "( " +thisVar.toMathematicaString() 
						+ " - " + center.get(thisVar).toMathematicaString()
						+  " )^2 + ";
			} else {
				ballString = ballString 
						+ "( " +thisVar.toMathematicaString() 
						+ " - " + center.get(thisVar).toMathematicaString()
						+  " )^2";
			}
		}

		ballString = ballString + " > " + radius.toMathematicaString();

		ballFormula = (ComparisonFormula)(dLStructure.parseStructure( ballString ));

		return ballFormula;
	}

// Check if the invariant robustly covers the domain
	public boolean invariantRobustlyCoversDomain( dLFormula invariant, dLFormula domain ) throws Exception {

		NotFormula negatedInvariant = new NotFormula( invariant );
		ArrayList<dLFormula> formulas = new ArrayList<dLFormula>();
		formulas.add( negatedInvariant );
		formulas.add( domain );

		if ( findInstance( formulas ) == null ) {
			return true;
		} else {
			return false;
		}
	}

// Tries to verify the given control law by refinement.
// 	1. Chooses a parameter point, 
// 	2. Checks if the invariant with this parameter covers the domain
// 	3. then tries refinement. 
// 	4. If it fails, it chooses a new parameter point, that is outside a ball of radius "resolution" 
// 	from the original point
// 	5. Keeps doing this until it succeeds.
	public Valuation parametricVerify (
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula robustparameters,
			dLFormula domain,
			ConcreteAssignmentProgram controllaw,
			double resolution ) throws Exception {

		Valuation witnessParameters = null;
		boolean success = false;
		dLFormula parameterSamplingFormula = robustparameters;
		Valuation thisParameter;
		
		while ( success == false ) {
			// Pick a parameter point
			if ( debug ) {
				System.out.println("Choosing a parameter valuation...");
				System.out.println("Parameter sampling formula is: " + parameterSamplingFormula);
			}
			thisParameter = findInstance( parameterSamplingFormula );
			dLFormula substitutedInvariant = invariant.substituteConcreteValuation( thisParameter );
			
			if ( thisParameter == null ) {
				throw new Exception( ANSI_BOLD + ANSI_RED + "No more parameters at this resolution!" + ANSI_RESET);
			}

			if( debug ) {
				System.out.println("thisParameter is: " + thisParameter.toMathematicaString() );
				System.out.println("substituted invariant is: " + substitutedInvariant.toMathematicaString() );
			}

			if ( !invariantRobustlyCoversDomain( substitutedInvariant, domain ) ) {
				System.out.println( ANSI_BOLD + ANSI_YELLOW + "WARNING:" + ANSI_RESET 
							+ " Invariant parametrization does not cover the domain robustly");
			} else {
				System.out.println( ANSI_BOLD + ANSI_CYAN + "INFO:" + ANSI_RESET 
							+" Invariant parametrization covers domain robustly");
			}

			// Try refinement verification
			File refinementQuery = writeSingleRefinementVerificationQuery(
					statevariables,
					eiparameters,
					envelope,
					invariant,
					thisParameter,
					controllaw );
			SMTResult refinementResult = runQuery( refinementQuery );
			if ( refinementResult.satisfiability.equals("unsat") ) {
				System.out.println("Refinement successful!:" + refinementResult);
				success = true;
				witnessParameters = thisParameter;

			} else { //update parameter formula to try a different point
				System.out.println("Refinement not succesful, choosing a new parameter vector; " 
									+ refinementResult);
				parameterSamplingFormula = new AndFormula( 
								parameterSamplingFormula, 
								createBallExclusionFormula( thisParameter, 
									new Real( resolution ) ) );
			}
		}


		return witnessParameters;

	}

// 
	public HashMap<dLFormula,Valuation> parametricVerifyByParts (
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula robustparameters,
			ConcreteAssignmentProgram controllaw,
			double resolution ) throws Exception {



				return new HashMap<dLFormula,Valuation>();
			}

// Writes a single refinement verification query. The main reason this function is nice w.r.t. the simple 
// query writing function is that it adds neat comments to point out the different portions
	protected File writeSingleRefinementVerificationQuery(
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			Valuation robustparameters,
			ConcreteAssignmentProgram controllaw ) throws Exception {


		String refinementQuery = "(set-logic QF_NRA)\n\n";

		// State variables
		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		refinementQuery = refinementQuery + "\n;; State variable declaration\n";
		RealVariable thisStateVariable;
		while ( stateVariableIterator.hasNext() ) {
			thisStateVariable = stateVariableIterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisStateVariable.todRealString() + " () Real)\n";
		}

		// Control variables
		Set<RealVariable> controlVariables = controllaw.getVariables();
		controlVariables.removeAll( statevariables ); controlVariables.removeAll( eiparameters );
		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		refinementQuery = refinementQuery + "\n;; Control variable declaration\n";
		RealVariable thisControlVariable;
		while ( controlVariableIterator.hasNext() ) {
			thisControlVariable = controlVariableIterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisControlVariable.todRealString() + " () Real)\n";
		}

		// EIParameters
		Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
		refinementQuery = refinementQuery + "\n;; Envelope-invariant parameter declaration\n";
		RealVariable thisEIParameter;
		while ( eiparameteriterator.hasNext() ) {
			thisEIParameter = eiparameteriterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisEIParameter.todRealString() + " () Real)\n";
		}

		// Parameter valuation
		refinementQuery = refinementQuery + "\n;; Assert the parameter valuation\n";
		refinementQuery = refinementQuery + ";; " + robustparameters.toString() + "\n";
		refinementQuery = refinementQuery + robustparameters.todRealString();

		// Invariant
		refinementQuery = refinementQuery + "\n;; Assert the invariant\n";
		refinementQuery = refinementQuery + ";; " + invariant.toMathematicaString() + "\n";
		refinementQuery = refinementQuery + "(assert " + invariant.todRealString() + " )\n";

		// Control law
		refinementQuery = refinementQuery + "\n;; Assert the controllaw\n";
		refinementQuery = refinementQuery + ";; " + controllaw.toMathematicaString() + "\n";
		refinementQuery = refinementQuery + "(assert " + controllaw.todRealString() + " )\n";

		// Envelope
		refinementQuery = refinementQuery + "\n;; Assert the NEGATION of the envelope "
							+ "(remember how dReal works!)\n";
		NotFormula negatedEnvelope = new NotFormula( envelope );
		refinementQuery = refinementQuery + ";; " + negatedEnvelope.toMathematicaString() + "\n";
		refinementQuery = refinementQuery + "(assert " + negatedEnvelope.todRealString() + " )\n";

		refinementQuery = refinementQuery + "\n(check-sat)\n(exit)\n";

		// Write the actual file
		double randomID = Math.round(Math.random());
		Date date = new Date();
		String filename = "drealworkspace/refinementVerificationQuery." 
					+ date.getTime() + "." + randomID + ".smt2";

		File drealworkspacedir = new File("drealworkspace");
		if (!drealworkspacedir.exists()) {
			drealworkspacedir.mkdir();
		}

		PrintWriter queryFile = new PrintWriter(filename);
		queryFile.println(";; Automatically generated by Perseus on " + date.toString() + "\n\n");
		queryFile.println( refinementQuery );
		queryFile.close();

		return new File( filename );

	}


}

