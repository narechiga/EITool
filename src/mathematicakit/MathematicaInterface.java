package honeybee.mathematicakit;

import java.util.*;
import java.io.*;
import manticore.dl.*;


public class MathematicaInterface {

    public static void writeParametricRefinementVerificationQuery(
							      ArrayList<RealVariable> statevariables,
							      ArrayList<RealVariable> eiparameters,
							      dLFormula envelope,
							      dLFormula invariant,
							      dLFormula robustparameters,
							      ConcreteAssignmentProgram controllaw ) {



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
	refinementQuery = refinementQuery + "\t\t(* robust parameters to choose from *)\n";
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
	refinementQuery = controllaw.toMathematicaString() + ";\n\n" + refinementQuery;
	refinementQuery = refinementQuery + "\t\t\t" + envelope.toMathematicaString() + "\n";
	refinementQuery = refinementQuery + "\t" + closingBraces + "\n";
	refinementQuery = refinementQuery + ", " + eliminationVariables + ", Reals ]\n";


	try {

	    File mathematicaworkspacedir = new File("mathematicaworkspace");
	    if (!mathematicaworkspacedir.exists()) {
		mathematicaworkspacedir.mkdir();
	    }

	    PrintWriter queryFile = new PrintWriter("mathematicaworkspace/refinementVerificationQueryFile.m");
	    Date date = new Date();
	    queryFile.println("(* Automatically generated on " + date.toString() + "*)\n\n");
	    queryFile.println( refinementQuery );
	    queryFile.close();

	} catch ( Exception e ) {
	    e.printStackTrace();
	}
	
    }

    
    public static void writeParametricRefinementSynthesisQuery(
							      ArrayList<RealVariable> statevariables,
							      ArrayList<RealVariable> eiparameters,
							      dLFormula envelope,
							      dLFormula invariant,
							      dLFormula robustparameters,
							      ConcreteAssignmentProgram controltemplate ) {


	Set<RealVariable> controlParameters = controltemplate.getRHS().getVariables();
	controlParameters.removeAll( statevariables );
	String controlParameterString = controlParameters.toString();
	controlParameterString = controlParameterString.replace("[", "{");
	controlParameterString = controlParameterString.replace("]", "}");

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
	refinementQuery = refinementQuery + "\t\t(* robust parameters to choose from *)\n";
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

	
	refinementQuery = "FindInstance[\n" + refinementQuery + "\t\t\t" + invariant.toMathematicaString() + ",\n";
	refinementQuery = controltemplate.toMathematicaString() + ";\n\n" + refinementQuery;
	refinementQuery = refinementQuery + "\t\t\t" + envelope.toMathematicaString() + "\n";
	refinementQuery = refinementQuery + "\t\t" + closingBraces + "\n";
	refinementQuery = refinementQuery + ", " + controlParameterString + ", Reals ]\n";


	try {

	    File mathematicaworkspacedir = new File("mathematicaworkspace");
	    if (!mathematicaworkspacedir.exists()) {
		mathematicaworkspacedir.mkdir();
	    }

	    PrintWriter queryFile = new PrintWriter("mathematicaworkspace/refinementSynthesisQueryFile.m");
	    Date date = new Date();
	    queryFile.println("(* Automatically generated on " + date.toString() + "*)\n\n");
	    queryFile.println( refinementQuery );
	    queryFile.close();

	} catch ( Exception e ) {
	    e.printStackTrace();
	}
	
    }

}



