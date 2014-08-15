package perseus.abstractions;

import manticore.dl.*;
import java.util.*;
import java.io.*;

public interface SolverInterface {

	// Basic ways to do "quantifier elimination"
	public SolverResult checkValidity( String filename, dLFormula thisFormula, String comment ) throws Exception;
	 
	// Basic ways to find an instance
	public SolverResult findInstance( String filename, List<dLFormula> theseFormulas, String comment ) 
				throws Exception;

	// Automatically generate comments and filenames, in accordance with what the solver likes
	// this is so I can keep track of what each query does
	public String generateFindInstanceComment( List<dLFormula> theseFormulas );
	public String generateCheckValidityComment( List<dLFormula> theseFormulas );
	public String decorateFilename( String base );
	public String generateFilename();

// Convenient aliases for findInstance
	default String generateFindInstanceComment( dLFormula thisFormula ) {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return generateFindInstanceComment( theseFormulas );
	}

	default String generateCheckValidityComment( dLFormula thisFormula ) {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return generateCheckValidityComment( theseFormulas );
	}

	default SolverResult findInstance( dLFormula thisFormula ) throws Exception {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return findInstance( theseFormulas );
	}

	default SolverResult findInstance( List<dLFormula> theseFormulas ) throws Exception {
		String filename = generateFilename();
		String comment = generateFindInstanceComment( theseFormulas );

		return findInstance( filename, theseFormulas, comment );
	}

	default SolverResult findInstance( String filename, dLFormula thisFormula, String comment ) throws Exception {
		ArrayList<dLFormula> theseFormulas = new ArrayList<dLFormula>();
		theseFormulas.add( thisFormula );

		return findInstance( filename, theseFormulas, comment );
	}

// Convenient alias for checkvalidity
	default SolverResult checkValidity ( dLFormula thisFormula ) throws Exception {
	    String comment = generateCheckValidityComment( new NotFormula(thisFormula) );
	    String filename = decorateFilename( "checkValidity" );

	    return checkValidity( filename, thisFormula, comment );
	}




}
