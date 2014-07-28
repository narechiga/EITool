package honeybee.drealkit;

import java.util.*;
import java.io.*;
import manticore.dl.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;


public class dRealInterface  {

	protected double precision;
	boolean debug = true;

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

		File queryFile = writeQueryFile( thisFormula );
		SMTResult dRealSays = runQuery( queryFile );

		if ( dRealSays.satisfiability.equals("unsat") ) {
			result = null;
		} else {
			result = dRealSays.valuation;
		}

		if( debug ) {
			System.out.println("findInstance valuation: " +  result.toString() );
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
			if( debug ) {
				System.out.println("(extractModel) found real variable: " 
							+ variable.toMathematicaString() );
			}
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
	protected File writeQueryFile( dLFormula thisFormula ) throws Exception {
		String queryString = "(set-logic QF_NRA)\n\n";

		Iterator<RealVariable> variableIterator = thisFormula.getVariables().iterator();
		queryString = queryString + "\n;; Variable declarations\n";
		RealVariable thisVariable;
		while ( variableIterator.hasNext() ) {
			queryString = queryString + "(declare-fun " + variableIterator.next() + " () Real )\n";
		}

		queryString = queryString + "\n;; Formula\n";
		queryString = queryString + "(assert " + thisFormula.todRealString() + " )\n";

		queryString = queryString + "\n(check-sat)\n(exit)\n";


		File drealworkspacedir = new File("drealworkspace");
		if (!drealworkspacedir.exists()) {
			drealworkspacedir.mkdir();
		}

		double randomID = Math.round(Math.random());
		Date date = new Date();
		String filename = "drealworkspace/query." + date.getTime() + "." + randomID + ".smt2";
		PrintWriter queryFile = new PrintWriter( filename );
		queryFile.println(";; Automatically generated by HoneyBee on " + date.toString() + "\n");
		queryFile.println(";; Assertion is " + thisFormula.toMathematicaString() + "\n\n");
		queryFile.println( queryString );
		queryFile.close();

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
				ballString = ballString + thisVar.toMathematicaString() +  "^2 + ";
			} else {
				ballString = ballString + thisVar.toMathematicaString();
			}
		}

		ballString = ballString + " > " + radius.toMathematicaString();

		ballFormula = (ComparisonFormula)(dLFormula.parseFormula( ballString ));

		return ballFormula;

		// TODO: finishme!


	}

// Tries to verify the given control law by refinement.
// 	1. Chooses a parameter point, then tries refinement. 
// 	2. If it fails, it chooses a new parameter point, that is outside a ball of radius "resolution" 
// 	from the original point
// 	3. Keeps doing this until it succeeds.
	public Valuation parametricVerify (
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula robustparameters,
			ConcreteAssignmentProgram controllaw,
			double resolution ) throws Exception {

		Valuation witnessParameters = null;
		boolean success = false;
		dLFormula parameterSamplingFormula = robustparameters;
		Valuation thisParameter;
		// Pick  a parameter point, try refinement verification
		// If it succeeds, return
		// If it fails, try a different point
		// continue until dreal returns no more points
		while ( success == false ) {
			// Pick a parameter point
			
			System.out.println("Choosing a parameter valuation...");
			System.out.println("Parameter sampling formula is: " + parameterSamplingFormula);
			thisParameter = findInstance( parameterSamplingFormula );
			
			if ( thisParameter == null ) {
				throw new Exception("No more parameters at this resolution!");
			}
			
			System.out.println("Writing refinement query...");
			// Try refinement verification
			File refinementQuery = writeSingleRefinementVerificationQuery(
					statevariables,
					eiparameters,
					envelope,
					invariant,
					thisParameter,
					controllaw );
			System.out.println("Running refinement query...");
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

	public HashMap<dLFormula,Valuation> parametricVerifyByParts (
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula robustparameters,
			ConcreteAssignmentProgram controllaw,
			double resolution ) throws Exception {
			}

	// Writes a single refinement verification query. The main reason this
	// function is nice w.r.t. the simple query writing function is that it adds
	// neat comments to point out the different portions
	protected File writeSingleRefinementVerificationQuery(
			ArrayList<RealVariable> statevariables,
			ArrayList<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			Valuation robustparameters,
			ConcreteAssignmentProgram controllaw ) throws Exception {


		String refinementQuery = "(set-logic QF_NRA)\n\n";

		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		refinementQuery = refinementQuery + "\n;; State variable declaration\n";
		RealVariable thisStateVariable;
		while ( stateVariableIterator.hasNext() ) {
			thisStateVariable = stateVariableIterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisStateVariable.todRealString() + " () Real)\n";
		}

		Iterator<RealVariable> controlVariableIterator = controllaw.getVariables().iterator();
		refinementQuery = refinementQuery + "\n;; Control variable declaration\n";
		RealVariable thisControlVariable;
		while ( controlVariableIterator.hasNext() ) {
			thisControlVariable = controlVariableIterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisControlVariable.todRealString() + " () Real)\n";
		}

		System.out.println("INFO: drealkit requires robustparameter set to be a singleton" +
					" (cannot evaluate Exists[ Forall[] ]  queries)");
		System.out.println("INFO: Checking this is difficult, so you may get a very cryptic error"
					+ " if this condition is not met");
		Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
		refinementQuery = refinementQuery + "\n;; Envelope-invariant parameter declaration\n";
		RealVariable thisEIParameter;
		while ( eiparameteriterator.hasNext() ) {
			thisEIParameter = eiparameteriterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisEIParameter.todRealString() + " () Real)\n";
		}

		refinementQuery = refinementQuery + "\n;; Assert the parameter valuation\n";
		refinementQuery = refinementQuery + ";; " + robustparameters.toString() + "\n";
		refinementQuery = refinementQuery + robustparameters.todRealString();

		refinementQuery = refinementQuery + "\n;; Assert the invariant\n";
		refinementQuery = refinementQuery + ";; " + invariant.toMathematicaString() + "\n";
		refinementQuery = refinementQuery + "(assert " + invariant.todRealString() + " )\n";

		refinementQuery = refinementQuery + "\n;; Assert the controllaw\n";
		refinementQuery = refinementQuery + ";; " + controllaw.toMathematicaString() + "\n";
		refinementQuery = refinementQuery + "(assert " + controllaw.todRealString() + " )\n";

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
		queryFile.println(";; Automatically generated by HoneyBee on " + date.toString() + "\n\n");
		queryFile.println( refinementQuery );
		queryFile.close();

		return new File( filename );

	}


}

