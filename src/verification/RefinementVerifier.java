package perseus.verification;

import proteus.dl.parser.*;
import proteus.dl.syntax.*;
import proteus.dl.semantics.*;

import proteus.logicsolvers.abstractions.*;
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

	LogicSolverInterface solver;
	boolean debug = false;

// Constructor
	public RefinementVerifier( LogicSolverInterface solver ) {
		this.solver = solver;
	}

// 
	public /*HashMap<dLFormula,Valuation>*/ void parametricVerifyByParts (
			List<RealVariable> statevariables,
			List<RealVariable> eiparameters,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula eiParameterSet,
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
			parameterSamples = cleverlySampleSet( eiParameterSet, numberOfParts, 2*resolution, resolution  );
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
			LogicSolverResult refinementResult = singleRefinementVerificationQuery(
					statevariables,
					overallEnvelope,
					overallInvariant,
					controllaw );
			if ( refinementResult.validity.equals("valid") ) {
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
	protected ArrayList<Valuation> cleverlySampleSet( dLFormula thisSet,
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
	protected ArrayList<Valuation> dumblySampleSet( dLFormula thisSet,
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
//	public LogicSolverResult singleRefinementVerificationQuery(
//			List<RealVariable> statevariables,
//			List<RealVariable> eiparameters,
//			dLFormula envelope,
//			dLFormula invariant,
//			Valuation eiParameterSet,
//			dLFormula controllaw ) throws Exception {
//
//		String comment = "";
//		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
//		String filename = solver.decorateFilename("refinementQuery");
//
//		// State variables
//		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
//		comment = comment + solver.commentLine("State variables are");
//		while ( stateVariableIterator.hasNext() ) {
//			comment = comment + solver.commentLine(stateVariableIterator.next().toMathematicaString());
//		}
//
//		// EIParameters
//		Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
//		comment = comment + "\n" + solver.commentLine("EI Parameters are");
//		while ( eiparameteriterator.hasNext() ) {
//			comment = comment + solver.commentLine(eiparameteriterator.next().toMathematicaString());
//		}
//		// Control variables
//		Set<RealVariable> controlVariables = controllaw.getFreeVariables();
//		controlVariables.removeAll( statevariables );
//		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
//		comment = comment + "\n" + solver.commentLine("Control variables are");
//		while ( controlVariableIterator.hasNext() ) {
//			comment = comment + solver.commentLine(controlVariableIterator.next().toMathematicaString());
//		}
//
//		// Invariant
//		comment = comment + "\n" + solver.commentLine("Invariant is: ") 
//					+ solver.commentLine(invariant.toMathematicaString());
//
//		// Control law
//		comment = comment + "\n" + solver.commentLine("Control law is: ")
//					+ solver.commentLine(controllaw.toMathematicaString());
//
//		// Envelope
//		comment = comment + "\n" + solver.commentLine("Envelope is (note that we will assert its negation): "
//				+ envelope.toMathematicaString());
//
//		AndFormula invariantAndControl = new AndFormula( invariant.substituteConcreteValuation( eiParameterSet), 
//								controllaw );
//		ImpliesFormula invariantAndControlImpliesEnvelope = new ImpliesFormula(
//					invariantAndControl, envelope.substituteConcreteValuation( eiParameterSet ) );
//
//		return solver.checkValidity( filename, invariantAndControlImpliesEnvelope, comment );
//
//
//	}


// *** assorted helper functions and the like
// Writes a single refinement verification query file, but with no ei parameters--assume they have been substituted
	public LogicSolverResult singleRefinementVerificationQuery(
			List<RealVariable> statevariables,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula controllaw ) throws Exception {


		String comment = "";
		String filename = solver.decorateFilename("refinementQuery");

		// State variables
		Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		comment = comment + solver.commentLine("State variables are");
		while ( stateVariableIterator.hasNext() ) {
			comment = comment + solver.commentLine(stateVariableIterator.next().toMathematicaString());
		}

		// Control variables
		Set<RealVariable> controlVariables = controllaw.getFreeVariables();
		controlVariables.removeAll( statevariables );
		Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		comment = comment + solver.commentLine("Control variables are");
		while ( controlVariableIterator.hasNext() ) {
			comment = comment + solver.commentLine(controlVariableIterator.next().toMathematicaString());
		}

		// Invariant
		comment = comment + solver.commentLine("Invariant is") + solver.commentLine(invariant.toMathematicaString());

		// Control law
		comment = comment + solver.commentLine("Control law is") + solver.commentLine(controllaw.toMathematicaString());

		// Envelope
		comment = comment + solver.commentLine("Envelope is")
				+ solver.commentLine(envelope.toMathematicaString());

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
	protected ComparisonFormula createBallExclusionFormula( Valuation center, Real radius ) throws Exception {

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

// Tries to verify the given control law by refinement.
// 	1. Chooses a parameter point, 
// 	2. Checks if the invariant with this parameter covers the domain
// 	3. then tries refinement. 
// 	4. If it fails, it chooses a new parameter point, that is outside a ball of radius "resolution" 
// 	from the original point
// 	5. Keeps doing this until it succeeds.
	public Valuation parametricVerify (
			List<RealVariable> statevariables,
			dLFormula initialSet,
			dLFormula safeSet,
			List<RealVariable> eiParameters,
			dLFormula eiParameterSet,
			dLFormula envelope,
			dLFormula invariant,
			dLFormula controlLaw,
			double delta ) throws Exception {
		
		// Build the refinement formula
		dLFormula refinementFormula = constructRefinementFormula( envelope, invariant, controlLaw);
		//System.out.println("Refinement formula is: ");
		//System.out.println( refinementFormula.toMathematicaString() );
		dLFormula invariantInitializedFormula = constructInvariantInitializedFormula( initialSet, invariant );
		dLFormula invariantSafeFormula = constructInvariantSafeFormula( invariant, safeSet);

		//System.out.println("Refinement formula is: " + refinementFormula.toMathematicaString() );
		//System.out.println("Free variables are: " + refinementFormula.getFreeVariables().toString() );

		// Pick an initial eStar, to kick things off
		Valuation eStar = solver.sample( eiParameterSet );
		ArrayList<Valuation> newSamples = new ArrayList<>();
		ArrayList<dLFormula> constraints = new ArrayList<>();
		constraints.add( eiParameterSet );
		
		int i = 0;
		while ( true ) {
			i = i + 1;
			
			// Check if e* is good
			//System.out.println("Going to check e*...: " + eStar.toMathematicaString() );
			//System.out.println("Substituted formula is: " + refinementFormula.plugIn( eStar ).toMathematicaString() );
			//System.out.println("Is initialization valid?");
			//solver.isValid( invariantInitializedFormula.plugIn( eStar ));
			//System.out.println("Is refinement valid?");
			//solver.isValid( refinementFormula.plugIn( eStar ) );
			//System.out.println("Is safety valid?");
			//solver.isValid( invariantSafeFormula.plugIn( eStar ) );

			//LogicSolverResult refinementResult = singleRefinementVerificationQuery(statevariables, envelope.plugIn( eStar ), invariant.plugIn(eStar), controlLaw );
			//boolean refinementIsValid = false;
			//System.out.println("Refinement validity: " + refinementResult.validity );
			//if ( refinementResult.validity.equals("valid") ) {
			//	refinementIsValid = true;
			//} else {
			//	System.out.println("Possible counterexample is: " + refinementResult.valuation.toMathematicaString() );
			//}
			if ( 
				solver.isValid( invariantInitializedFormula.plugIn( eStar ))
				&& solver.isValid( invariantSafeFormula.plugIn( eStar ) ) 
				&& solver.isValid( refinementFormula.plugIn( eStar ) )
			) {

				System.out.println(ANSI_GREEN + ANSI_BOLD + "Verified successfully!" + ANSI_RESET );
				System.out.println("(...) with envelope parameters: "+ ANSI_CYAN + ANSI_BOLD + eStar.toMathematicaString() + ANSI_RESET );
				return eStar;
			} 
			
			// If it isn't, try to pick a bunch of cases that break it
			newSamples = solver.multiSample( (refinementFormula.plugIn(eStar)).negate(), 2, delta );
			//newSamples.addAll( solver.multiSample( (invariantInitializedFormula.plugIn(eStar)).negate(), 2, delta ) );
			//newSamples.addAll( solver.multiSample( (invariantSafeFormula.plugIn(eStar)).negate(), 2, delta ) );

			// If there aren't any at this resolution, just go ahead with the constraints on e
			if ( newSamples.isEmpty() ) {
				System.out.println("Couldn't find any more samples!");
				//return null;
			}

			// Add constraints for the new samples
			for ( Valuation newSample : newSamples ) {
				System.out.println("(sample is: " + newSample.toMathematicaString() + " )");

				//constraints.add( invariantInitializedFormula.plugIn( newSample ) );
				//constraints.add( refinementFormula.plugIn( newSample ) );

				//constraints.add( invariantSafeFormula.plugIn( newSample ) );
				constraints.add( createBallExclusionFormula(eStar, new Real( delta) ) );
			}

			// Pick a new e*
			System.out.println("Picking a new e*:...");
			Valuation newEStar = solver.sample( constraints );
			if ( newEStar == null ) {
				return null;

			} else if ( newEStar.getVariables().isEmpty() ) {
				System.out.println("Could not find a new parameter value, sample came back empty");
				System.out.println(ANSI_RED + ANSI_BOLD + "Verification failed." + ANSI_RESET);

				//System.out.println(ANSI_BOLD + ANSI_RED + "Probably dReal didn't finish writing out its file!");
				//System.out.println("...on iteration " + i);
				//System.out.println("Trying again with eStar: " + eStar.toMathematicaString() );
				//System.out.println("But waiting to see if whatever issue resolves itself in a second?");

				//try {
				//	    Thread.sleep(3000);                 //1000 milliseconds is one second.
				//} catch(InterruptedException ex) {
				//	    Thread.currentThread().interrupt();
				//}
				return null;


			} else {
				eStar = newEStar;
			}

			System.out.println("New parameters is: " + eStar.toMathematicaString() );
		}
	}

	protected dLFormula constructRefinementFormula( 
							dLFormula envelope,
							dLFormula invariant,
							dLFormula controlLaw ) {

		Replacement control = null;
		if ( ( controlLaw instanceof ComparisonFormula )
			&& ( ((ComparisonFormula)controlLaw).getLHS() instanceof RealVariable ) ) {

			control = new Replacement(
							((RealVariable)(((ComparisonFormula)controlLaw).getLHS())),
							((ComparisonFormula)controlLaw).getRHS() );

		} else {
			throw new RuntimeException("Malformed control law: " + controlLaw.toMathematicaString() );
		}

		dLFormula refinementFormula = new ImpliesFormula(
							invariant,
							envelope.replace( control )
							);

		System.out.println("Refinement formula is: " + refinementFormula.toMathematicaString() );
		//System.exit(1);

		return refinementFormula;


	}

	protected dLFormula constructInvariantInitializedFormula( dLFormula initialSet,
									dLFormula invariant ) {
		return new ImpliesFormula(
					initialSet,
					invariant );

	}

	protected dLFormula constructInvariantSafeFormula( dLFormula invariant,
								dLFormula safeSet ) {
		return new ImpliesFormula(
					invariant,
					safeSet );
	}



}

