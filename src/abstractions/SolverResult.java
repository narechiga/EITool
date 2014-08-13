package perseus.abstractions;

import manticore.dl.*;

public class SolverResult {
    
    public final String satisfiability;
    public final String validity;
    public final Valuation valuation;

    public SolverResult( String satisfiability, String validity, Valuation valuation ) 
    		throws SolverResultIntegrityException {
	this.satisfiability = satisfiability;
	this.validity = validity;
	this.valuation = valuation;

	checkIntegrity();
    }

    public String toString() {
    	    return "(Solver result:\n\tsatisfiability: " + satisfiability 
    	    		+ "\n\tvaluation: " 
    	    		+ valuation.toString() +"\n)\n";
    }

    public void checkIntegrity() throws SolverResultIntegrityException {

	// Check that validity contains a validity string
	if ( !validity.equals("valid")
		&& !validity.equals("notvalid")
		&& !validity.equals("unknown") ) {
		throw new SolverResultIntegrityException( "Incorrect validity string:\n" + this.toString() );
	}

	// Check that satisfiability contains a satisfiability string
	if ( !satisfiability.equals("sat")
		&& !satisfiability.equals("unsat")
		&& !satisfiability.equals("delta-sat")
		&& !satisfiability.equals("unknown") ) {
		throw new SolverResultIntegrityException( "Incorrect satisfiability string:\n" + this.toString() );

	}

	// Check that it is not both unsat and valid
	if ( satisfiability.equals("unsat")
		&& validity.equals("valid") ) {
		throw new SolverResultIntegrityException("Result cannot be unsat and valid:\n" + this.toString() );
	}

	// If it's unsat, you better not be giving me a valuation
	if ( satisfiability.equals("unsat") && !valuation.isEmpty() ) {
		throw new SolverResultIntegrityException("Valuation given for purportedly unsat formula\n" 
								+ this.toString() );
	}

	// Make sure I get a valuation when you tell me there is one
//	if ( satisfiability.equals("sat") && valuation == null ) {
//		throw new SolverResultIntegrityException("Formula is allegedly sat, but no valuation is given\n" 
//								+ this.toString() );
//	}

//	if ( validity.equals("valid") && valuation == null ) {
//		throw new SolverResultIntegrityException("Formula is allegedly valid, but no valuation is given\n" 
//								+ this.toString() );
//	}

   }


}


    
    
