package perseus.abstractions;

import manticore.dl.*;
import java.util.*;
import java.io.*;

public interface SolverInterface {

	// Basic ways to do "quantifier elimination"
	public SolverResult checkValidity( dLFormula thisFormula ) throws Exception;
	public SolverResult checkValidity( String filename, dLFormula thisFormula, String comment ) throws Exception;
	 
	// Basic ways to find an instance
	public SolverResult findInstance( dLFormula thisFormula ) throws Exception;
	public SolverResult findInstance( List<dLFormula> theseFormulas ) throws Exception;
	public SolverResult findInstance( String filename, List<dLFormula> theseFormulas, String comment ) 
				throws Exception;

	// Automatically generate comments and filenames, in accordance with what the solver likes
	// this is so I can keep track of what each query does
	public String generateComment( List<dLFormula> theseFormulas );
	public String decorateFilename( String base );
	public String generateFilename();

	// Retire these eventually
	public SolverResult runQuery( File queryFile ) throws Exception;
	public Valuation extractModel( File modelfile ) throws Exception;


}
