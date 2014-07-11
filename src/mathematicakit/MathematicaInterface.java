package honeybee.mathematicakit;

import java.util.*;
import java.io.*;
import manticore.dl.*;

public class MathematicaInterface {

    public static void writeSingleRefinementVerificationQuery(
							      ArrayList<RealVariable> statevariables,
							      ArrayList<RealVariable> eiparameters,
							      dLFormula envelope,
							      dLFormula invariant,
							      dLFormula robustparameters,
							      dLFormula controllaw ) {



	String refinementQuery = "";
	String closingBraces = "";
	String eliminationVariables = "{ ";

	Iterator<RealVariable> eiparameteriterator = eiparameters.iterator();
	RealVariable thisEIParameter;
	while ( eiparameteriterator.hasNext() ) {
	    thisEIParameter = eiparameteriterator.next();
	    refinementQuery = refinementQuery + "\tExists[ " + thisEIParameter.toMathematicaString() + ", \n";
	    eliminationVariables = eliminationVariables + thisEIParameter.toMathematicaString() +", ";
	    closingBraces = closingBraces + " ]";
	}
	refinementQuery = refinementQuery + "\t\t(* robust paramters to choose from *)\n";
	refinementQuery = refinementQuery + "\t\t" + robustparameters.toMathematicaString() + ",\n";
	

	
	Iterator<RealVariable> statevariableiterator = statevariables.iterator();
	RealVariable thisStateVariable;
	while ( statevariableiterator.hasNext() ) {
	    thisStateVariable = statevariableiterator.next();
	    refinementQuery = refinementQuery + "\t\tForAll[ " + thisStateVariable.toMathematicaString() + ", \n";
	    closingBraces = closingBraces + " ]";

	    if ( statevariableiterator.hasNext() ) {
		eliminationVariables = eliminationVariables + thisStateVariable.toMathematicaString() + ", ";
	    } else {
		eliminationVariables =  eliminationVariables + thisStateVariable.toMathematicaString() + " }";
	    }
	}

	
	refinementQuery = "Reduce[\n" + refinementQuery + "\t\t\t" + invariant.toMathematicaString() + ",\n";
	refinementQuery = refinementQuery + "\t\t" + "Implies[\n\t\t\t " + controllaw.toMathematicaString() + ",\n ";
	refinementQuery = refinementQuery + "\t\t\t" + envelope.toMathematicaString() + "\n\t\t]\n\t";
	refinementQuery = refinementQuery + closingBraces + "\n";
	refinementQuery = refinementQuery + ", " + eliminationVariables + ", Reals ]\n";


	try {

	    File workspacedir = new File("workspace");
	    if (!workspacedir.exists()) {
		workspacedir.mkdir();
	    }

	    PrintWriter queryFile = new PrintWriter("workspace/refinementQueryFile.m");
	    Date date = new Date();
	    queryFile.println("(* Automatically generated on " + date.toString() + "*)\n\n");
	    queryFile.println( refinementQuery );
	    queryFile.close();

	} catch ( Exception e ) {
	    e.printStackTrace();
	}
	
	//Set<RealVariable> varList = new Set<RealVariable>();
	//varList.addAll( envelope.getVariables() );
	// Actually, I may need to declare within the file, who are state variables and who are 
	// envelope parameters, and who are controlparameters
	// Since this is single refinement, the only parameters belong to the ei-pair, since the controller has no
	// parameters

	// Actually, robust parameters are already declared! I could infer state variables just from taking them
	// out of the controller, which has no parameters, but perhaps it is better form to actually declare them
	// in the input file.
	
	
	

    }


}



