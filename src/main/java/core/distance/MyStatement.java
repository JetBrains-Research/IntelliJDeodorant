package core.distance;

import core.ast.decomposition.AbstractStatement;

class MyStatement extends MyAbstractStatement {

	public MyStatement(AbstractStatement statement) {
		super(statement);
	}

	public MyStatement(MyMethodInvocation methodInvocation) {
		super(methodInvocation);
	}
}
