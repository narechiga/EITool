package perseus.abstractions;

import manticore.dl.*;

public abstract class ProblemStatement {
	List<RealVariable> stateVariables;
	List<RealVariable> eiParameters;
	dLFormula envelope;
	dLFormula invariant;
	dLFormula robustParameters;
	dLFormula domain;
}
