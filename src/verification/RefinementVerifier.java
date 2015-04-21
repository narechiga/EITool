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
	public boolean checkParts( 
			List<RealVariable> statevariables,
			dLFormula initialSet,
			dLFormula safeSet,
			List<RealVariable> eiParameters,
			dLFormula eiParameterSet,
			dLFormula envelope,
			dLFormula invariant,
			HybridProgram controlLaw,
			List<Valuation> parameters
			) throws Exception {


		// Construct the overall invariant and overall refinement query
		dLFormula OverallInvariant= new FalseFormula();
		dLFormula OverallRefinement = new FalseFormula();
		dLFormula baseRefinement = constructRefinementFormula(
						envelope,
						invariant,
						controlLaw
						);
		for( Valuation parameter : parameters ) {
			OverallInvariant = new OrFormula(
						OverallInvariant,
						invariant.plugIn( parameter ) 
						);

			OverallRefinement = new OrFormula( 
						OverallRefinement,
						baseRefinement.plugIn( parameter )
						);
		}


		// Construct the other two "overall" formulas
		dLFormula invariantInitializedFormula = constructInvariantInitializedFormula(
								initialSet,
								OverallInvariant
								);

		dLFormula invariantSafeFormula = constructInvariantSafeFormula(
								OverallInvariant,
								safeSet
								);

		// Check the validity of the three formulas, and comment on validity of each
		System.out.println("Checking validity of invariant initialization;");
		boolean invariantInitialized = solver.isValid( invariantInitializedFormula );
		if ( invariantInitialized ) {
			System.out.println(ANSI_GREEN 
					+ "Invariant is initialized." + ANSI_RESET);
		} else {
			System.out.println(ANSI_RED 
					+ "Invariant is not initialized." + ANSI_RESET);

			System.out.println(ANSI_BOLD + "Counterexample: " 
						+ ANSI_RED
						+ solver.sample( 
							invariantInitializedFormula.negate() ) 
						+ ANSI_RESET 
						);
		}

		System.out.println("Checking validity of invariant safety;");
		boolean invariantSafe = solver.isValid( invariantSafeFormula );
		if ( invariantSafe ) {
			System.out.println(ANSI_GREEN 
					+ "Invariant is safe." + ANSI_RESET);
		} else {
			System.out.println(ANSI_RED 
					+ "Invariant is not safe." + ANSI_RESET);

			System.out.println(ANSI_BOLD + "Counterexample: " 
						+ ANSI_RED 
						+ solver.sample( invariantSafeFormula.negate() ) 
						+ANSI_RESET );
		}

		System.out.println("Checking validity of refinement;");
		boolean refinementValid = solver.isValid( OverallRefinement );
		if ( refinementValid ) {
			System.out.println(ANSI_GREEN 
					+ "Refinement holds." + ANSI_RESET);
		} else {
			System.out.println(ANSI_RED 
					+ "Refinement does not hold." + ANSI_RESET);

			System.out.println(ANSI_BOLD + "Counterexample: " 
						+ ANSI_RED
						+ solver.sample( OverallRefinement.negate() ) 
						+ ANSI_RESET );
		}

		boolean result;
		if ( invariantInitialized
			&& invariantSafe
			&& refinementValid ){
			System.out.println(ANSI_BOLD + ANSI_GREEN 
						+"Verification successful!" + ANSI_RESET);
			result = true;
		} else {
			System.out.println(ANSI_BOLD + ANSI_RED 
						+"Verification not successful, see issues above." 
						+ ANSI_RESET);

			result = false;
		}
		return result;
			

	}

// 
	public  Valuation parametricVerifyByParts (
			List<RealVariable> statevariables,
			dLFormula initialSet,
			dLFormula safeSet,
			List<RealVariable> eiParameters,
			dLFormula eiParameterSet,
			dLFormula envelope,
			dLFormula invariant,
			HybridProgram controlLaw,
			double delta ) throws Exception {


		int numberOfParts = 1;

		dLFormula simpleRefinementFormula = constructRefinementFormula( envelope, invariant, controlLaw );
		dLFormula invariantInitializedFormula = new FalseFormula();
		dLFormula invariantSafeFormula = new FalseFormula();

		dLFormula OverallInvariant = new FalseFormula();
		dLFormula EcartprodK = new TrueFormula();
		dLFormula OverallRefinement = new FalseFormula();
		Replacement eta = new Replacement();

		while ( true ) {

			ArrayList<Replacement> partParameters = new ArrayList<>();
			Replacement thisPartParameter;

			OverallInvariant = new FalseFormula();
			EcartprodK = new TrueFormula();
			OverallRefinement = new FalseFormula();
			eta = new Replacement();

			invariantInitializedFormula = new FalseFormula();
			invariantSafeFormula = new FalseFormula();

			for ( Integer i = 0; i < numberOfParts; i = i + 1 ) {
				// Generate the parameter vector for each part
				thisPartParameter = new Replacement();
				for ( RealVariable eiParameter : eiParameters ) {
					eta.put( eiParameter, new RealVariable( eiParameter.toString() + i.toString() ) );
					thisPartParameter.put( eiParameter, new RealVariable( eiParameter.toString() + i.toString() ));
				}
				partParameters.add( thisPartParameter );

				// Replace each parameter vector into the invariant
				EcartprodK = new AndFormula( EcartprodK, eiParameterSet.replace( thisPartParameter ) );


				OverallRefinement = new OrFormula( OverallRefinement,
								simpleRefinementFormula.replace( thisPartParameter ) );


				OverallInvariant = new OrFormula( OverallInvariant,
									invariant.replace( thisPartParameter ) );
				
			}
			System.out.println("E^k is: " + EcartprodK.toKeYmaeraString() );
			System.out.println("Overall refinement formula is: " + OverallRefinement.toKeYmaeraString() );
			System.out.println("Overall invariant is: " + OverallInvariant.toKeYmaeraString() );

			invariantInitializedFormula = constructInvariantInitializedFormula( initialSet, OverallInvariant );
			invariantSafeFormula = constructInvariantSafeFormula( OverallInvariant, safeSet );

			//1. Pick an eta to kick things off
			ArrayList<dLFormula> constraints = new ArrayList<>();
			constraints.add( EcartprodK );
			while ( true ) { //inner loop that searches for an eta

				Valuation etaStar = solver.sample( constraints );
				System.out.println("Picked initial eta value: " + etaStar.toMathematicaString() );
				
				
				System.out.println("Substituted formulas: ");
				System.out.println("Invariant initialized: " + invariantInitializedFormula.plugIn(etaStar).toKeYmaeraString() );
				System.out.println("Invariant safe: " + invariantSafeFormula.plugIn(etaStar).toKeYmaeraString() );
				System.out.println("Refinement condition: " + OverallRefinement.plugIn(etaStar).toKeYmaeraString() );

				System.out.println("A brief overview of results.");
				System.out.println("Invariant initialized: " + solver.isValid( invariantInitializedFormula.plugIn(etaStar)) );
				System.out.println("Invariant safe: " + solver.isValid( invariantSafeFormula.plugIn(etaStar)) );
				System.out.println("Refinement condition: " + solver.isValid( OverallRefinement.plugIn(etaStar) ));


				if ( 
					solver.isValid( invariantInitializedFormula.plugIn( etaStar ))
					&& solver.isValid( invariantSafeFormula.plugIn( etaStar ) ) 
					&& solver.isValid( OverallRefinement.plugIn( etaStar ) )
				) {

					System.out.println(ANSI_GREEN + ANSI_BOLD + "Verified successfully!" + ANSI_RESET );
					System.out.println("(...) with envelope parameters: "+ ANSI_CYAN + ANSI_BOLD + etaStar.toMathematicaString() + ANSI_RESET );
					return etaStar;
				} 

				//2. Find cex'es, if any
				ArrayList<Valuation> newSamples = solver.multiSample( (OverallRefinement.plugIn(etaStar)).negate(), 2, delta );

				// If there aren't any at this resolution, just go ahead with the constraints on e
				if ( newSamples.isEmpty() ) {
					System.out.println("Couldn't find any more samples!");
					System.out.println(ANSI_RED + ANSI_BOLD + "Verification failed." + ANSI_RESET);
					return null;
				}

				// Add constraints for the new samples
				for ( Valuation newSample : newSamples ) {
					System.out.println("(sample is: " + newSample.toMathematicaString() + " )");
					System.out.println("Adding constraint: ");
					System.out.println( OverallRefinement.plugIn( newSample ).toMathematicaString() );

					constraints.add( OverallRefinement.plugIn( newSample ) );
					//constraints.add( createBallExclusionFormula(etaStar, new Real( delta) ) );
				}

				// Pick a new eta*
				System.out.println("Picking a new eta*:...");
				Valuation newEtaStar = solver.sample( constraints );

				if ( newEtaStar == null ) {
					return null;

				} else if ( newEtaStar.getVariables().isEmpty() ) {
					System.out.println("Could not find a new parameter value, sample came back empty");
					System.out.println(ANSI_YELLOW + ANSI_BOLD + "Verification failed with " + numberOfParts + "." + ANSI_RESET);
					break;

				} else {
					etaStar = newEtaStar;
				}

				System.out.println("New parameter is: " + etaStar.toMathematicaString() );
			}

			numberOfParts = numberOfParts + 1;
			System.out.println("Incrementing to " + numberOfParts + " parts.");

		}

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
//			HybridProgram controlLaw ) throws Exception {
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
//		Set<RealVariable> controlVariables = controlLaw.getFreeVariables();
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
//					+ solver.commentLine(controlLaw.toMathematicaString());
//
//		// Envelope
//		comment = comment + "\n" + solver.commentLine("Envelope is (note that we will assert its negation): "
//				+ envelope.toMathematicaString());
//
//		AndFormula invariantAndControl = new AndFormula( invariant.substituteConcreteValuation( eiParameterSet), 
//								controlLaw );
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
			HybridProgram controlLaw ) throws Exception {


		System.out.println("RefinementVerifier.singleRefinementVerificationQuery is out of order.");
		return null;
		//String comment = "";
		//String filename = solver.decorateFilename("refinementQuery");

		//// State variables
		//Iterator<RealVariable> stateVariableIterator = statevariables.iterator();
		//comment = comment + solver.commentLine("State variables are");
		//while ( stateVariableIterator.hasNext() ) {
		//	comment = comment + solver.commentLine(stateVariableIterator.next().toMathematicaString());
		//}

		//// Control variables
		//Set<RealVariable> controlVariables = controlLaw.getFreeVariables();
		//controlVariables.removeAll( statevariables );
		//Iterator<RealVariable> controlVariableIterator = controlVariables.iterator();
		//comment = comment + solver.commentLine("Control variables are");
		//while ( controlVariableIterator.hasNext() ) {
		//	comment = comment + solver.commentLine(controlVariableIterator.next().toMathematicaString());
		//}

		//// Invariant
		//comment = comment + solver.commentLine("Invariant is") + solver.commentLine(invariant.toMathematicaString());

		//// Control law
		//comment = comment + solver.commentLine("Control law is") + solver.commentLine(controlLaw.toMathematicaString());

		//// Envelope
		//comment = comment + solver.commentLine("Envelope is")
		//		+ solver.commentLine(envelope.toMathematicaString());

		//AndFormula invariantAndControl = new AndFormula( invariant, controlLaw );
		//ImpliesFormula invariantAndControlImpliesEnvelope = new ImpliesFormula(
		//			invariantAndControl, envelope );

		//return solver.checkValidity( filename, invariantAndControlImpliesEnvelope, comment );

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
			HybridProgram controlLaw,
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
				System.out.println(ANSI_RED + ANSI_BOLD + "Verification failed." + ANSI_RESET);
				return null;
			}

			// Add constraints for the new samples
			for ( Valuation newSample : newSamples ) {
				System.out.println("(sample is: " + newSample.toMathematicaString() + " )");

				//constraints.add( invariantInitializedFormula.plugIn( newSample ) );

				System.out.println("Adding constraint: ");
				System.out.println( refinementFormula.plugIn( newSample ).toMathematicaString() );
				constraints.add( refinementFormula.plugIn( newSample ) );
				//constraints.add( createBallExclusionFormula(eStar, new Real( delta) ) );

			}

			// Pick a new e*
			System.out.println("Picking a new e*:...");
			Valuation newEStar = solver.sample( constraints );

			// for debugging
			//Valuation newEStar = new Valuation( new RealVariable("e"), new Real("0.88")); 

			if ( newEStar == null ) {
				System.out.println("Could not find a new parameter value, sample came back empty");
				System.out.println(ANSI_RED + ANSI_BOLD + "Verification failed." + ANSI_RESET);
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

			System.out.println("New parameter is: " + eStar.toMathematicaString() );
		}
	}

	protected dLFormula constructRefinementFormula( 
							dLFormula envelope,
							dLFormula invariant,
							HybridProgram controlLaw ) {

		System.out.println("Examining envelope-invariant-control relationship");
		
		Replacement control = null;
		dLFormula refinementFormula = null;
		if ( ( controlLaw instanceof ConcreteAssignmentProgram ) ) {
			control = new Replacement(
							((ConcreteAssignmentProgram)controlLaw).getLHS(),
							((ConcreteAssignmentProgram)controlLaw).getRHS() );

			refinementFormula = new ImpliesFormula(
							invariant,
							envelope.replace( control )
							);


		} else if ( controlLaw instanceof ChoiceProgram ) {
			System.out.println("Found something that looks like a piecewise control law,");
			List<HybridProgram> controlPieces = ((ChoiceProgram)controlLaw).splitOnChoice();

			dLFormula refinedEnvelopeByRegions = new FalseFormula();
			dLFormula thisRefinementClause = null;
			dLFormula thisRegime = null;
			ConcreteAssignmentProgram thisControl = null;
			Replacement thisControlReplacement = null;
			int i = 0;
			for ( HybridProgram piece : controlPieces ) {
				if ( (piece instanceof SequenceProgram)
					&& ((SequenceProgram)piece).getLHS() instanceof TestProgram
					&& ((SequenceProgram)piece).getRHS() instanceof ConcreteAssignmentProgram ) {


					thisRegime = ((TestProgram)((SequenceProgram)piece).getLHS()).getFormula();
					thisControl = ((ConcreteAssignmentProgram)((SequenceProgram)piece).getRHS());

					thisControlReplacement = new Replacement(
									thisControl.getLHS(),
									thisControl.getRHS() 
									);

					System.out.println("Piece " + i + " is " + 
								thisControl.toKeYmaeraString() );
					System.out.println("(...) over region " + thisRegime ); 

					thisRefinementClause = new AndFormula( thisRegime, 
									envelope.replace(thisControlReplacement  ) );

					refinedEnvelopeByRegions = new OrFormula( refinedEnvelopeByRegions,
									thisRefinementClause
									);
				} else {
					throw new RuntimeException("Malformed control law: " + controlLaw.toKeYmaeraString() );
				}

			}

			refinementFormula = new ImpliesFormula(
							invariant,
							refinedEnvelopeByRegions );

		} else {
			throw new RuntimeException("Malformed control law: " + controlLaw.toKeYmaeraString() );
			
		}


		System.out.println("Refinement formula is: " + refinementFormula.toMathematicaString() );
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

