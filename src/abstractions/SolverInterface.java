package perseus.abstractions;

import manticore.dl.*;
import java.util.*;
import java.io.*;

public abstract class SolverInterface {

// Basic ways to do "quantifier elimination"
	public abstract SolverResult checkValidity( String filename, dLFormula thisFormula, String comment ) 
				throws Exception;
	 
// Basic ways to find an instance
	public abstract SolverResult findInstance( String filename, List<dLFormula> theseFormulas, String comment ) 
				throws Exception;

// Convenient aliades for findInstance
	public SolverResult findInstance( dLFormula thisFormula ) throws Exception {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return findInstance( theseFormulas );
	}

	public SolverResult findInstance( List<dLFormula> theseFormulas ) throws Exception {
		String filename = generateFilename();
		String comment = generateFindInstanceComment( theseFormulas );

		return findInstance( filename, theseFormulas, comment );
	}

	public SolverResult findInstance( String filename, dLFormula thisFormula, String comment ) throws Exception {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return findInstance( filename, theseFormulas, comment );
	}

// Convenient alias for checkvalidity
	public SolverResult checkValidity ( dLFormula thisFormula ) throws Exception {
	    String comment = generateCheckValidityComment( new NotFormula(thisFormula) );
	    String filename = decorateFilename( "checkValidity" );

	    return checkValidity( filename, thisFormula, comment );
	}

// Automatically generate comments and filenames, in accordance with what the solver likes
// this is so I can keep track of what each query does
	public abstract String commentLine( String comment );
	public abstract String decorateFilename( String base );
	public abstract String generateFilename();

// Convenience functions for auto-commenting
	protected abstract String generateFindInstanceComment( List<dLFormula> theseFormulas );
	protected abstract String generateCheckValidityComment( List<dLFormula> theseFormulas );
	protected String generateFindInstanceComment( dLFormula thisFormula ) {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return generateFindInstanceComment( theseFormulas );
	}

	protected String generateCheckValidityComment( dLFormula thisFormula ) {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return generateCheckValidityComment( theseFormulas );
	}

}

