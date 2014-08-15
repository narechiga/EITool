package perseus.mathematicakit;

import perseus.abstractions.*;
import java.util.*;
import java.io.*;
import manticore.dl.*;


public class MathematicaInterface extends SolverInterface {

// Constructor
	public MathematicaInterface() {

		// Generate a workspace folder
		File mathematicaworkspace = new File("mathematicaworkspace");
		if (!mathematicaworkspace.exists()) {
			mathematicaworkspace.mkdir();
		}
	}

// Basic way to do "quantifier elimination"
	public SolverResult checkValidity( String filename, dLFormula thisFormula, String comment ) throws Exception {

		SolverResult result = new SolverResult("unknown", "unknown", new Valuation() );

		String variableString = thisFormula.getVariables().toString();
		variableString = variableString.replace("[", "{").replace("]", "}");

		// Write the file
		File queryFile = new File( filename );
		PrintWriter queryFileWriter = new PrintWriter( queryFile );

		queryFileWriter.println( timeStampComment( comment ));

		queryFileWriter.println("Print[ Quiet[ Reduce[\n");
		queryFileWriter.println("\t" + thisFormula.universalClosure().toMathematicaString() + "\n");
		queryFileWriter.println(", " + variableString + ", Reals ] ] ];");
		queryFileWriter.close();
		
		// Run Mathematica
		ProcessBuilder builder = new ProcessBuilder("MathematicaScript", "-script", queryFile.getAbsolutePath() );
		//builder.redirectErrorStream(true);
		Process process = builder.start();
		InputStream stdout = process.getInputStream();
		BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
		
		// Parse the result
		Lexer thisLexer = new Lexer( reader );
		YYParser thisParser = new YYParser( thisLexer );
		thisParser.parse();
			
		if ( thisParser.parsedStructure instanceof TrueFormula ){
		        result = new SolverResult("sat", "valid", new Valuation() );
		} else if ( thisParser.parsedStructure instanceof FalseFormula ) { 
		        result = new SolverResult("unsat", "notvalid", new Valuation() );
		} else if ( thisParser.parsedStructure instanceof dLFormula ) {
			result = new SolverResult("sat", "notvalid", new Valuation() );
		} else {
			throw new Exception("Mathematica checkValidity is confused by " 
						+ thisParser.parsedStructure.toString() );
		}

		return result;

	}
	 
// Basic way to find an instance
	public SolverResult findInstance( String filename, List<dLFormula> theseFormulas, String comment ) 
				throws Exception {

		SolverResult result = new SolverResult("unknown", "unknown", new Valuation() );

		Iterator<dLFormula> formulaIterator = theseFormulas.iterator();
		dLFormula totalFormula = formulaIterator.next();
		while( formulaIterator.hasNext() ) {
			totalFormula = new AndFormula( totalFormula, formulaIterator.next() );
		}

		String variableString = totalFormula.getVariables().toString();
		variableString = variableString.replace("[", "{").replace("]", "}");
		
		// Write the file
		File queryFile = new File( filename );
		PrintWriter queryFileWriter = new PrintWriter( queryFile );

		queryFileWriter.println( timeStampComment( comment ));

		queryFileWriter.println("Print[ Flatten[ Quiet[ FindInstance[\n");
		queryFileWriter.println("\t" + totalFormula.toMathematicaString() + "\n");
		queryFileWriter.println(", " + variableString + ", Reals ] ] ] ];");
		queryFileWriter.close();

		// Run Mathematica
		ProcessBuilder builder = new ProcessBuilder("MathematicaScript", "-script", queryFile.getAbsolutePath() );
		builder.redirectErrorStream(true);
		Process process = builder.start();
		InputStream stdout = process.getInputStream();
		BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
		
		// Parse the result
		Lexer thisLexer = new Lexer( reader );
		YYParser thisParser = new YYParser( thisLexer );
		thisParser.parse();
			
		if ( thisParser.valuation != null ){
		        result = new SolverResult("sat", "unknown", thisParser.valuation );
		} 
		// else we don't know anything, and just return the result that knows nothing, already initialized

		return result;
		
	}

// Automatically generate comments and filenames, in accordance with what the solver likes
// this is so I can keep track of what each query does
	protected String generateFindInstanceComment( List<dLFormula> theseFormulas ) {
		String comment = "(* Find a valuation of variables that satisfies the formulas: *)\n";
		Iterator<dLFormula> formulaIterator = theseFormulas.iterator();
		int counter = 1;
		while ( formulaIterator.hasNext() ) {
			comment = comment + "(* Formula " + counter + ": *)\n";
			comment = comment + "(* " + formulaIterator.next().toMathematicaString() + " *)\n";
			counter = counter + 1;
		}

		return comment;
	}
	protected String generateCheckValidityComment( List<dLFormula> theseFormulas ) {
		String comment = "(* Check the (conjunctive) validity of: *)\n";
		Iterator<dLFormula> formulaIterator = theseFormulas.iterator();
		int counter = 1;
		while ( formulaIterator.hasNext() ) {
			comment = comment + "(* Formula " + counter + ": *)\n";
			comment = comment + "(* " + formulaIterator.next().toMathematicaString() + " *)\n";
			counter = counter + 1;
		}

		return comment;
	}

	protected String timeStampComment( String comment ) {
		Date date = new Date();
		String stampedComment = "(* Automatically generated by Perseus on " + date.toString() + " *)\n\n";
		stampedComment = stampedComment + comment;
		
		return stampedComment;
	}

	public String commentLine( String comment ) {
		return "(* " + comment + " *)\n";
	}
//
	public String decorateFilename( String base ) {
		double randomID = Math.round(Math.random());
		Date date = new Date();
		return "mathematicaworkspace/" + base + + date.getTime() + "." + randomID + ".m";
	}
//
	public String generateFilename() {
		double randomID = Math.round(Math.random());
		Date date = new Date();
		return "mathematicaworkspace/query." + date.getTime() + "." + randomID + ".m";
	}

//
//	protected File writeQueryFile( String filename, List<dLFormula> theseFormulas, String comment ) {
//		return null;
//
//		//String queryString = "Reduce[\n";
//
//		//// get all the variables
//
//		//try {
//		//	queryFile = new File( filename );
//		//	PrintWriter queryFileWriter = new PrintWriter( queryFile );
//		//	queryFileWriter.println( queryString );
//		//	queryFileWriter.close();
//		//	return queryFile;
//		//} catch ( Exception e ) {
//		//	e.printStackTrace();
//		//}
//	}
//


   // public static void writeParametricRefinementVerificationQuery(
   //     						      ArrayList<RealVariable> statevariables,
   //     						      ArrayList<RealVariable> eiparameters,
   //     						      dLFormula envelope,
   //     						      dLFormula invariant,
   //     						      dLFormula robustparameters,
   //     						      ConcreteAssignmentProgram controllaw ) {



   //     String refinementQuery = "";
   //     String closingBraces = "";
   //     String eliminationVariables = "{ ";

   //     Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
   //     RealVariable thisEIParameter;
   //     while ( eiparameteriterator.hasNext() ) {
   //         thisEIParameter = eiparameteriterator.next();
   //         refinementQuery = refinementQuery + "\tExists[ " + thisEIParameter.toMathematicaString() + ", \n";
   //         eliminationVariables = eliminationVariables + thisEIParameter.toMathematicaString() +", ";
   //         closingBraces = closingBraces + " ]";
   //     }
   //     refinementQuery = refinementQuery + "\t\t(* robust parameters to choose from *)\n";
   //     refinementQuery = refinementQuery + "\t\t" + robustparameters.toMathematicaString() + ",\n";
   //     

   //     
   //     Iterator<RealVariable> statevariableiterator = statevariables.iterator();
   //     RealVariable thisStateVariable;
   //     while ( statevariableiterator.hasNext() ) {
   //         thisStateVariable = statevariableiterator.next();
   //         refinementQuery = refinementQuery + "\t\tForAll[ " + thisStateVariable.toMathematicaString() + ", \n";
   //         closingBraces = closingBraces + " ]";

   //         if ( statevariableiterator.hasNext() ) {
   //     	eliminationVariables = eliminationVariables + thisStateVariable.toMathematicaString() + ", ";
   //         } else {
   //     	eliminationVariables =  eliminationVariables + thisStateVariable.toMathematicaString() + " }";
   //         }
   //     }

   //     
   //     refinementQuery = "Reduce[\n" + refinementQuery + "\t\t\t" + invariant.toMathematicaString() + ",\n";
   //     refinementQuery = controllaw.toMathematicaString() + ";\n\n" + refinementQuery;
   //     refinementQuery = refinementQuery + "\t\t\t" + envelope.toMathematicaString() + "\n";
   //     refinementQuery = refinementQuery + "\t" + closingBraces + "\n";
   //     refinementQuery = refinementQuery + ", " + eliminationVariables + ", Reals ]\n";


   //     try {

   //         File mathematicaworkspacedir = new File("mathematicaworkspace");
   //         if (!mathematicaworkspacedir.exists()) {
   //     	mathematicaworkspacedir.mkdir();
   //         }

   //         PrintWriter queryFile = new PrintWriter("mathematicaworkspace/refinementVerificationQueryFile.m");
   //         Date date = new Date();
   //         queryFile.println("(* Automatically generated on " + date.toString() + "*)\n\n");
   //         queryFile.println( refinementQuery );
   //         queryFile.close();

   //     } catch ( Exception e ) {
   //         e.printStackTrace();
   //     }
   //     
   // }

   // 
   // public static void writeParametricRefinementSynthesisQuery(
   //     						      ArrayList<RealVariable> statevariables,
   //     						      ArrayList<RealVariable> eiparameters,
   //     						      dLFormula envelope,
   //     						      dLFormula invariant,
   //     						      dLFormula robustparameters,
   //     						      ConcreteAssignmentProgram controltemplate ) {


   //     Set<RealVariable> controlParameters = controltemplate.getRHS().getVariables();
   //     controlParameters.removeAll( statevariables );
   //     String controlParameterString = controlParameters.toString();
   //     controlParameterString = controlParameterString.replace("[", "{");
   //     controlParameterString = controlParameterString.replace("]", "}");

   //     String refinementQuery = "";
   //     String closingBraces = "";
   //     String eliminationVariables = "{ ";

   //     Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
   //     RealVariable thisEIParameter;
   //     while ( eiparameteriterator.hasNext() ) {
   //         thisEIParameter = eiparameteriterator.next();
   //         refinementQuery = refinementQuery + "\tExists[ " + thisEIParameter.toMathematicaString() + ", \n";
   //         eliminationVariables = eliminationVariables + thisEIParameter.toMathematicaString() +", ";
   //         closingBraces = closingBraces + " ]";
   //     }
   //     refinementQuery = refinementQuery + "\t\t(* robust parameters to choose from *)\n";
   //     refinementQuery = refinementQuery + "\t\t" + robustparameters.toMathematicaString() + ",\n";
   //     

   //     
   //     Iterator<RealVariable> statevariableiterator = statevariables.iterator();
   //     RealVariable thisStateVariable;
   //     while ( statevariableiterator.hasNext() ) {
   //         thisStateVariable = statevariableiterator.next();
   //         refinementQuery = refinementQuery + "\t\tForAll[ " + thisStateVariable.toMathematicaString() + ", \n";
   //         closingBraces = closingBraces + " ]";

   //         if ( statevariableiterator.hasNext() ) {
   //     	eliminationVariables = eliminationVariables + thisStateVariable.toMathematicaString() + ", ";
   //         } else {
   //     	eliminationVariables =  eliminationVariables + thisStateVariable.toMathematicaString() + " }";
   //         }
   //     }

   //     
   //     refinementQuery = "FindInstance[\n" + refinementQuery + "\t\t\t" + invariant.toMathematicaString() + ",\n";
   //     refinementQuery = controltemplate.toMathematicaString() + ";\n\n" + refinementQuery;
   //     refinementQuery = refinementQuery + "\t\t\t" + envelope.toMathematicaString() + "\n";
   //     refinementQuery = refinementQuery + "\t\t" + closingBraces + "\n";
   //     refinementQuery = refinementQuery + ", " + controlParameterString + ", Reals ]\n";


   //     try {

   //         File mathematicaworkspacedir = new File("mathematicaworkspace");
   //         if (!mathematicaworkspacedir.exists()) {
   //     	mathematicaworkspacedir.mkdir();
   //         }

   //         PrintWriter queryFile = new PrintWriter("mathematicaworkspace/refinementSynthesisQueryFile.m");
   //         Date date = new Date();
   //         queryFile.println("(* Automatically generated on " + date.toString() + "*)\n\n");
   //         queryFile.println( refinementQuery );
   //         queryFile.close();

   //     } catch ( Exception e ) {
   //         e.printStackTrace();
   //     }
   //     
   // }

}



