package perseus.verification;

import proteus.dl.parser.*;
import proteus.dl.syntax.*;
import proteus.dl.semantics.*;
import proteus.logicsolvers.abstractions.*;
import java.util.*;

public class VerificationProblem extends ProblemStatement {
	public HybridProgram control;

	public VerificationProblem ( 
					List<RealVariable> stateVariables, 
					dLFormula initialSet,
					dLFormula safeSet,
					dLFormula eiparameterset,
					List<RealVariable> eiParameters, 
					dLFormula envelope, 
					dLFormula invariant, 
					HybridProgram control ) {
		this.stateVariables = stateVariables;
		this.initialSet = initialSet;
		this.safeSet = safeSet;
		this.eiParameterSet = eiparameterset;
		this.eiParameters = eiParameters;
		this.envelope = envelope;
		this.invariant = invariant;
		this.control = control;
	}

	public String toString() {
		String returnString = "";

		returnString = returnString +"{ :: verification problem instance\n";
		returnString = returnString +"stateVariables: " + stateVariables.toString() + "\n";
		returnString = returnString +"initialSet: " + initialSet.toMathematicaString() + "\n";
		returnString = returnString +"safeSet: " + safeSet.toKeYmaeraString() + "\n";
		returnString = returnString +"eiParameters: " + eiParameters.toString() + "\n";
		returnString = returnString +"eiParameterSet: " + eiParameterSet.toKeYmaeraString() + "\n";
		returnString = returnString +"envelope: " + envelope.toMathematicaString() + "\n";
		returnString = returnString +"invariant: " + invariant.toMathematicaString() + "\n";
		returnString = returnString +"control: " + control.toKeYmaeraString() + "\n";
		returnString = returnString + "}";

		return returnString;
	}


}
