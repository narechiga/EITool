package perseus.verification;

import perseus.abstractions.*;
import manticore.dl.*;
import java.util.*;

public class VerificationProblem extends ProblemStatement {
	public dLFormula control;

	public VerificationProblem ( List<RealVariable> stateVariables, List<RealVariable> eiParameters, 
					dLFormula envelope, dLFormula invariant, dLFormula robustParameters, 
					dLFormula domain, dLFormula control ) {
		this.stateVariables = stateVariables;
		this.eiParameters = eiParameters;
		this.envelope = envelope;
		this.invariant = invariant;
		this.robustParameters = robustParameters;
		this.domain = domain;
		this.control = control;
	}

	public String toString() {
		String returnString = "";

		returnString = returnString +"{ :: verification problem instance\n";
		returnString = returnString +"stateVariables: " + stateVariables.toString() + "\n";
		returnString = returnString +"eiParameters: " + eiParameters.toString() + "\n";
		returnString = returnString +"envelope: " + envelope.toMathematicaString() + "\n";
		returnString = returnString +"invariant: " + invariant.toMathematicaString() + "\n";
		returnString = returnString +"robustParameters: " + robustParameters.toMathematicaString() + "\n";
		returnString = returnString +"domain: " + domain.toMathematicaString() + "\n";
		returnString = returnString +"control: " + control.toMathematicaString() + "\n";
		returnString = returnString + "}";

		return returnString;
	}


}
