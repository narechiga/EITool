package perseus.verification;

import manticore.dl.*;
import perseus.abstractions.*;
import java.util.*;

public class RefinementVerifier{
	// COLORS! OMG COLORS!
	public final String ANSI_RESET = "\u001B[0m";
	public final String ANSI_BLACK = "\u001B[30m";
	public final String ANSI_RED = "\u001B[31m";
	public final String ANSI_GREEN = "\u001B[32m";
	public final String ANSI_YELLOW = "\u001B[33m";
	public final String ANSI_BLUE = "\u001B[34m";
	public final String ANSI_PURPLE = "\u001B[35m";
	public final String ANSI_CYAN = "\u001B[36m";
	public final String ANSI_WHITE = "\u001B[37m";
	public final String ANSI_BOLD = "\u001B[1m";

	SolverInterface solver;
	boolean debug = false;

// Constructor
	public RefinementVerifier( SolverInterface solver ) {
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
			dLFormula controllaw,
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
			thisPoint = solver.findInstance( queryFormulas ).valuation;
			if ( thisPoint != null && !thisPoint.isEmpty() ) {
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
			dLFormula controllaw ) throws Exception {

		String comment = "";
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		String filename = solver.decorateFilename("refinementQuery");

		// State variables
		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		comment = comment + "\n;; State variables are\n;; ";
		while ( stateVariableIterator.hasNext() ) {
			comment = comment + " " + stateVariableIterator.next().toMathematicaString();
		}

		// EIParameters
		Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
		comment = comment + "\n;; EI Parameters are\n;; ";
		while ( eiparameteriterator.hasNext() ) {
			comment = comment + " " + eiparameteriterator.next().toMathematicaString();
		}
		// Control variables
		Set<RealVariable> controlVariables = controllaw.getVariables();
		controlVariables.removeAll( statevariables );
		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		comment = comment + "\n;; Control variables are\n;; ";
		while ( controlVariableIterator.hasNext() ) {
			comment = comment + " " + controlVariableIterator.next().toMathematicaString();
		}

		// Invariant
		comment = comment + "\n;; Invariant is\n;; " + invariant.toMathematicaString();

		// Control law
		comment = comment + "\n;; Control law is\n;; " + controllaw.toMathematicaString();

		// Envelope
		comment = comment + "\n;; Envelope is (note that we will assert its negation\n;; "
				+ envelope.toMathematicaString();

		AndFormula invariantAndControl = new AndFormula( invariant.substituteConcreteValuation( robustparameters), 
								controllaw );
		ImpliesFormula invariantAndControlImpliesEnvelope = new ImpliesFormula(
					invariantAndControl, envelope.substituteConcreteValuation( robustparameters ) );

		return solver.checkValidity( filename, invariantAndControlImpliesEnvelope, comment );


	}


// *** assorted helper functions and the like
// Writes a single refinement verification query file, but with no ei parameters--assume they have been substituted
	public SolverResult singleRefinementVerificationQuery(
			List<RealVariable> statevariables,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula controllaw ) throws Exception {


		String comment = "";
		String filename = solver.decorateFilename("refinementQuery");

		// State variables
		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		comment = comment + "\n;; State variables are\n;; ";
		while ( stateVariableIterator.hasNext() ) {
			comment = comment + stateVariableIterator.next().toMathematicaString();
		}

		// Control variables
		Set<RealVariable> controlVariables = controllaw.getVariables();
		controlVariables.removeAll( statevariables );
		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		comment = comment + "\n;; Control variables are\n;; ";
		while ( controlVariableIterator.hasNext() ) {
			comment = comment + controlVariableIterator.next().toMathematicaString();
		}

		// Invariant
		comment = comment + "\n;; Invariant is\n;; " + invariant.toMathematicaString();

		// Control law
		comment = comment + "\n;; Control law is\n;; " + controllaw.toMathematicaString();

		// Envelope
		comment = comment + "\n;; Envelope is (note that we will assert its negation\n;; "
				+ envelope.toMathematicaString();

		AndFormula invariantAndControl = new AndFormula( invariant, controllaw );
		ImpliesFormula invariantAndControlImpliesEnvelope = new ImpliesFormula(
					invariantAndControl, envelope );

		return solver.checkValidity( filename, invariantAndControlImpliesEnvelope, comment );

	}

// Check if the invariant robustly covers the domain
	public boolean setARobustlyCoversSetB( dLFormula setA, dLFormula setB ) throws Exception {

		//NotFormula negatedSetA = new NotFormula( setA );
		//dLFormula simplifiedSetA = negatedSetA.pushNegation();
		//ArrayList<dLFormula> formulas = splitOnAnds( simplifiedSetA );

		//formulas.add( setB );
		ImpliesFormula BimpliesA = new ImpliesFormula( setB, setA );

		if ( solver.checkValidity( BimpliesA ).validity.equals("valid") ) {
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
//	public ArrayList<dLFormula> splitOnAnds ( dLFormula thisFormula ) {
//		ArrayList<dLFormula> subFormulas = new ArrayList<dLFormula>();
//
//		if ( !(thisFormula instanceof AndFormula ) ) {
//			subFormulas.add( thisFormula );
//		} else {
//			subFormulas.addAll( splitOnAnds( ((AndFormula)thisFormula).getLHS() ) );
//			subFormulas.addAll( splitOnAnds( ((AndFormula)thisFormula).getRHS() ) );
//		}
//
//		return subFormulas;
//	}

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
			dLFormula controllaw,
			double resolution ) throws Exception {

		
		Valuation witnessParameters = new Valuation();
		boolean success = false;
		dLFormula parameterSamplingFormula = robustparameters;
		Valuation thisParameter;
		
		while ( success == false ) {
			// Pick a parameter point
			if ( debug ) {
				System.out.println("Choosing a parameter valuation...");
				System.out.println("Parameter sampling formula is: " + parameterSamplingFormula);
			}
			
			thisParameter = solver.findInstance( parameterSamplingFormula ).valuation;

			
			if ( thisParameter.isEmpty() ) {
				throw new Exception( ANSI_BOLD + ANSI_RED + "No more parameters at this resolution!" 
										+ ANSI_RESET);
			}
			System.out.println("Trying refinement with parameter valuation: " 
							+ thisParameter.toMathematicaString() );
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
			if ( refinementResult.validity.equals("valid") ) {
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

