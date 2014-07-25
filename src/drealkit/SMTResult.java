package honeybee.drealkit;

import manticore.dl.*;

public class SMTResult {
    
    boolean isValid;
    Valuation valuation;

    public SMTResult( boolean isValid, Valuation valuation ) {
	this.isValid = isValid;
	this.valuation = valuation;
    }

    public SMTResult() {
	this.isValid = false;
	this.valuation = null;
    }

    public String toString() {
    	    return "(SMT result:\n\tisValid: " + isValid 
    	    		+ "\n\tvaluation: " 
    	    		+ valuation.toString() +"\n)\n";
    }


}


    
    
