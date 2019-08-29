package gr.uom.java.distance;

import gr.uom.java.ast.decomposition.AbstractStatement;

class MyStatement extends MyAbstractStatement {

	public MyStatement(AbstractStatement statement) {
		super(statement);
	}

	public MyStatement(MyMethodInvocation methodInvocation) {
		super(methodInvocation);
	}
}
