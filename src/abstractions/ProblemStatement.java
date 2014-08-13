package perseus.abstractions;

import manticore.dl.*;
import java.util.*;

public abstract class ProblemStatement {
	public List<RealVariable> stateVariables;
	public List<RealVariable> eiParameters;
	public dLFormula envelope;
	public dLFormula invariant;
	public dLFormula robustParameters;
	public dLFormula domain;
}
