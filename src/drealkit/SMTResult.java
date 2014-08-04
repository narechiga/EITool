package perseus.drealkit;

import manticore.dl.*;

public class SMTResult {
    
    String satisfiability;
    Valuation valuation;

    public SMTResult( String satisfiability, Valuation valuation ) {
	this.satisfiability = satisfiability;
	this.valuation = valuation;
    }

    public SMTResult() {
	this.satisfiability = "unknown";
	this.valuation = null;
    }

    public String toString() {
    	    return "(SMT result:\n\tsatisfiability: " + satisfiability 
    	    		+ "\n\tvaluation: " 
    	    		+ valuation.toString() +"\n)\n";
    }


}


    
    
