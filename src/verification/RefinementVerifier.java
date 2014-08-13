package perseus.verification;

import manticore.dl.*;
import perseus.abstractions.*;

public class RefinementVerifier{

	SolverInterface solver;

// Constructor
	public RefinementVerifer( SolverInterface solver ) {
		this.solver = solver;
	}

// 
	public /*HashMap<dLFormula,Valuation>*/ void parametricVerifyByParts (
			List<RealVariable> statevariables,
			List<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula robustparameters,
			dLFormula domain,
			ConcreteAssignmentProgram controllaw,
			double resolution ) throws Exception {

		int numberOfParts = 1;
		ArrayList<Valuation> parameterSamples;
		Iterator<Valuation> parameterIterator;
		dLFormula overallInvariant = null;
		dLFormula overallEnvelope = null;
		Valuation thisParameter;
		boolean success; boolean robustDomainCoverage;
		do {
			// Choose the some parameters
			parameterSamples = cleverlySampleSet( robustparameters, numberOfParts, 2*resolution, resolution  );
			if ( parameterSamples.size() < numberOfParts ) {
				throw new Exception( ANSI_BOLD + ANSI_RED + "No more parameters at this resolution!" 
								+ ANSI_RESET);
			}
			System.out.println("Using: (..)" ); 
			
			parameterIterator = parameterSamples.iterator();

			// Generate the overall invariant and envelope
			if ( parameterIterator.hasNext() ) {

				thisParameter = parameterIterator.next();
				System.out.println(thisParameter.toMathematicaString());

				overallInvariant = invariant.substituteConcreteValuation( thisParameter );
				overallEnvelope = new AndFormula( invariant.substituteConcreteValuation( thisParameter ),
								envelope.substituteConcreteValuation( thisParameter ) );
			}
			while ( parameterIterator.hasNext() ) {
				thisParameter = parameterIterator.next();
				System.out.println(thisParameter.toMathematicaString());

				overallInvariant = new OrFormula( overallInvariant, 
							invariant.substituteConcreteValuation( thisParameter ) );

				overallEnvelope = new OrFormula( overallEnvelope,
					new AndFormula( invariant.substituteConcreteValuation( thisParameter ),
						envelope.substituteConcreteValuation( thisParameter ) ) );

			}

			// Check if the overall invariant covers the domain
			if ( !setARobustlyCoversSetB( overallInvariant, domain ) ) {
				System.out.println( ANSI_BOLD + ANSI_YELLOW + "WARNING:" + ANSI_RESET 
							+ " Invariant parametrization does not cover the domain robustly");
				robustDomainCoverage = false;
			} else {
				System.out.println( ANSI_BOLD + ANSI_CYAN + "INFO:" + ANSI_RESET 
							+" Invariant parametrization covers domain robustly");
				robustDomainCoverage = true;
			}
			// Then try refinement with the overall envelope and invariant
			SolverResult refinementResult = singleRefinementVerificationQuery(
					statevariables,
					overallEnvelope,
					overallInvariant,
					controllaw );
			if ( refinementResult.satisfiability.equals("unsat") ) {
				System.out.println( ANSI_BOLD + ANSI_GREEN + "Refinement successful!" + ANSI_RESET); 
				success = true;

			} else { //update parameter formula to try a different point
				System.out.println("Refinement " + ANSI_BOLD + ANSI_RED 
							+"not" + ANSI_RESET +" succesful with " 
							+ numberOfParts + " parts, incrementing...");
				success = false;
			}
			numberOfParts = 2*numberOfParts;

		} while( (!success ) || (!robustDomainCoverage) );


		//return new HashMap<dLFormula,Valuation>();
	}

//

// 
	public ArrayList<Valuation> cleverlySampleSet( dLFormula thisSet,
							int numberOfPoints,
							double suggestedRadius,
							double resolution ) throws Exception {

		// This function may fail to allocate all of the points desired, at the given
		// resolution, but it will allocate as many as it can
		
		ArrayList<Valuation> points;
		double lowerBound = 0.0;
		double upperBound = suggestedRadius;
		double thisRadius = (lowerBound + upperBound)/2;


		// The states of the search automaton
		int REACH = 0;
		int CONVERGE = 1;
		int STATE = REACH;
		points = dumblySampleSet( thisSet, numberOfPoints, thisRadius );
		if ( numberOfPoints == 1 ) {
			return points;
		}

		while( upperBound - lowerBound > resolution ) {
			if ( (STATE == REACH) && (points.size() == numberOfPoints) ) { 
				//succesfully allocated points, grow the interval
				lowerBound = thisRadius;
				upperBound = upperBound*2;
				System.out.println("Successfully allocated points, growing interval");
				System.out.println("Next State: REACH; lower:" + lowerBound + "; upper: " + upperBound);
				STATE = REACH;

			} else if ( (STATE == REACH) && (points.size() < numberOfPoints) ) {
				// could not allocate all the points :( too big radius!
				lowerBound = lowerBound;
				upperBound = thisRadius;
				System.out.println("Could not allocate points, switching to converge mode");
				System.out.println("Next State: CONVERGE; lower:" + lowerBound + "; upper: " + upperBound);
				STATE = CONVERGE;

			} else if ( (STATE == CONVERGE) && points.size() == numberOfPoints ) {
				lowerBound = thisRadius;
				upperBound = upperBound;
				System.out.println("Successfully allocated points, continuing convergence");
				System.out.println("Next State: CONVERGE; lower:" + lowerBound + "; upper: " + upperBound);
				STATE = CONVERGE;

			} else if ( (STATE == CONVERGE) && (points.size() < numberOfPoints) ) {
				lowerBound = lowerBound;
				upperBound = thisRadius;
				System.out.println("Could not allocate all points, continuing convergence");
				System.out.println("Next State: CONVERGE; lower:" + lowerBound + "; upper: " + upperBound);
				STATE = CONVERGE;
			}

			thisRadius = (lowerBound + upperBound)/2;
			points = dumblySampleSet( thisSet, numberOfPoints, thisRadius );
		}
			
		if ( (points.size() < numberOfPoints) && (lowerBound > 0) ) {	
			points = dumblySampleSet( thisSet, numberOfPoints, lowerBound );
		}

		return points;

	}


//
	public ArrayList<Valuation> dumblySampleSet( dLFormula thisSet,
							int numberOfPoints,
							double suggestedRadius ) throws Exception {
		// This function just tries to allocate the given number of points at the suggested radius
		ArrayList<dLFormula> queryFormulas = new ArrayList<dLFormula>();
		ArrayList<Valuation> samplePoints = new ArrayList<Valuation>();
		Valuation thisPoint;

		queryFormulas.add( thisSet );

		for ( int i = 0; i < numberOfPoints; i++ ) {
			thisPoint = solver.findInstance( queryFormulas );
			if ( thisPoint != null ) {
				samplePoints.add( thisPoint );
				queryFormulas.add( createBallExclusionFormula( thisPoint, new Real(suggestedRadius) ) );
				//System.out.println("Iteration: " + i + ";  Ball Exclusion Formula: " +
				//			createBallExclusionFormula( thisPoint, new Real(suggestedRadius) ).toMathematicaString() );

			} else {
				break;
			}
		}

		return samplePoints;

	}


// Writes a single refinement verification query. The main reason this function is nice w.r.t. the simple 
// query writing function is that it adds neat comments to point out the different portions
// TAINTED
	public SolverResult singleRefinementVerificationQuery(
			List<RealVariable> statevariables,
			List<RealVariable> eiparameters,
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
		controlVariables.removeAll( statevariables ); //controlVariables.removeAll( eiparameters );
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
		refinementQuery = refinementQuery + "\n;; Choice of ei parameters is\n";
		refinementQuery = refinementQuery + ";; " + robustparameters.toMathematicaString() + "\n";
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


		return solver.runQuery( new File( filename ) );
	}


// *** assorted helper functions and the like
// Writes a single refinement verification query file, but with no ei parameters--assume they have been substituted
	public SolverResult singleRefinementVerificationQuery(
			List<RealVariable> statevariables,
			dLFormula envelope,
			dLFormula invariant,
			ConcreteAssignmentProgram controllaw ) throws Exception {


		String comment = "";
		ArrayList<dLFormula> theseFormulas;

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
		controlVariables.removeAll( statevariables );
		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		refinementQuery = refinementQuery + "\n;; Control variable declaration\n";
		RealVariable thisControlVariable;
		while ( controlVariableIterator.hasNext() ) {
			thisControlVariable = controlVariableIterator.next();
			refinementQuery = refinementQuery + "(declare-fun " 
						+ thisControlVariable.todRealString() + " () Real)\n";
		}

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

		return solver.runQuery( new File(filename) );

	}

// Check if the invariant robustly covers the domain
	public boolean setARobustlyCoversSetB( dLFormula setA, dLFormula setB ) throws Exception {

		NotFormula negatedSetA = new NotFormula( setA );
		dLFormula simplifiedSetA = negatedSetA.pushNegation();
		ArrayList<dLFormula> formulas = splitOnAnds( simplifiedSetA );

		formulas.add( setB );

		if ( solver.findInstance( formulas ) == null ) {
			return true;
		} else {
			return false;
		}
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

// A utility formula
	public ArrayList<dLFormula> splitOnAnds ( dLFormula thisFormula ) {
		ArrayList<dLFormula> subFormulas = new ArrayList<dLFormula>();

		if ( !(thisFormula instanceof AndFormula ) ) {
			subFormulas.add( thisFormula );
		} else {
			subFormulas.addAll( splitOnAnds( ((AndFormula)thisFormula).getLHS() ) );
			subFormulas.addAll( splitOnAnds( ((AndFormula)thisFormula).getRHS() ) );
		}

		return subFormulas;
	}

// Tries to verify the given control law by refinement.
// 	1. Chooses a parameter point, 
// 	2. Checks if the invariant with this parameter covers the domain
// 	3. then tries refinement. 
// 	4. If it fails, it chooses a new parameter point, that is outside a ball of radius "resolution" 
// 	from the original point
// 	5. Keeps doing this until it succeeds.
	public Valuation parametricVerify (
			List<RealVariable> statevariables,
			List<RealVariable> eiparameters,
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
			thisParameter = solver.findInstance( parameterSamplingFormula );
			
			if ( thisParameter == null ) {
				throw new Exception( ANSI_BOLD + ANSI_RED + "No more parameters at this resolution!" + ANSI_RESET);
			}
			System.out.println("Trying refinement with parameter valuation: " + thisParameter.toMathematicaString() );
			dLFormula substitutedInvariant = invariant.substituteConcreteValuation( thisParameter );

			if( debug ) {
				System.out.println("thisParameter is: " + thisParameter.toMathematicaString() );
				System.out.println("substituted invariant is: " + substitutedInvariant.toMathematicaString() );
			}

			if ( !setARobustlyCoversSetB( substitutedInvariant, domain ) ) {
				System.out.println( ANSI_BOLD + ANSI_YELLOW + "WARNING:" + ANSI_RESET 
							+ " Invariant parametrization does not cover the domain robustly");
			} else {
				System.out.println( ANSI_BOLD + ANSI_CYAN + "INFO:" + ANSI_RESET 
							+" Invariant parametrization covers domain robustly");
			}

			// Try refinement verification
			SolverResult refinementResult = singleRefinementVerificationQuery(
					statevariables,
					eiparameters,
					envelope,
					invariant,
					thisParameter,
					controllaw );
			if ( refinementResult.satisfiability.equals("unsat") ) {
				System.out.println( ANSI_BOLD + ANSI_GREEN + "Refinement successful!" + ANSI_RESET);
				success = true;
				witnessParameters = thisParameter;
				System.out.println("With envelope parameters:\n" +witnessParameters.toMathematicaString());

			} else { //update parameter formula to try a different point
				System.out.println("Refinement " + ANSI_BOLD + ANSI_RED +"not" 
							+ ANSI_RESET +" succesful, choosing a new parameter vector");
									//+ refinementResult);
				parameterSamplingFormula = new AndFormula( 
								parameterSamplingFormula, 
								createBallExclusionFormula( thisParameter, 
									new Real( resolution ) ) );
			}
		}


		return witnessParameters;

	}


}

