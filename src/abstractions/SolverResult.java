package perseus.abstractions;

import manticore.dl.*;

public class SolverResult {
    
    public final String satisfiability;
    public final Valuation valuation;

    public SolverResult( String satisfiability, Valuation valuation ) throws SolverResultIntegrityException {
	this.satisfiability = satisfiability;
	this.valuation = valuation;

	checkIntegrity();
    }

    public String toString() {
    	    return "(Solver result:\n\tsatisfiability: " + satisfiability 
    	    		+ "\n\tvaluation: " 
    	    		+ valuation.toString() +"\n)\n";
    }

    public void checkIntegrity() throws SolverResultIntegrityException {
    	    if ( satisfiability.equals("sat") && valuation != null ) {
    	    	    // we are good
		} else if ( satisfiability.equals("unsat") && valuation == null ) {
			// still good
		} else if ( satisfiability.equals("unknown") ) {
			// ternary logic, still good
		} else {
			throw new SolverResultIntegrityException( this.toString() );
		}
	}


}


    
    
