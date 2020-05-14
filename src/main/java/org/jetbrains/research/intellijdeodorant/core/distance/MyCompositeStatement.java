package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyCompositeStatement extends MyAbstractStatement {
	private final List<MyAbstractStatement> statementList;
	private final List<MyAbstractExpression> expressionList;
	
	public MyCompositeStatement(AbstractStatement statement) {
		super(statement);
		this.statementList = new ArrayList<>();
		this.expressionList = new ArrayList<>();
	}

	public void addStatement(MyAbstractStatement statement) {
		statementList.add(statement);
	}

	public void addExpression(MyAbstractExpression expression) {
		expressionList.add(expression);
		expression.setOwner(this);
	}

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
    	super.setAttributeInstructionReference(myAttributeInstruction, reference);
    	for(MyAbstractStatement statement : statementList) {
			statement.setAttributeInstructionReference(myAttributeInstruction, reference);
		}
    	for(MyAbstractExpression expression : expressionList) {
    		expression.setAttributeInstructionReference(myAttributeInstruction, reference);
    	}
    }

    public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
    	MyAbstractStatement abstractStatement = super.getAbstractStatement(statement);
    	if(abstractStatement != null) {
    		return abstractStatement;
    	}
    	else {
    		for(MyAbstractStatement myStatement : statementList) {
    			abstractStatement = myStatement.getAbstractStatement(statement);
    			if(abstractStatement != null) {
    	    		return abstractStatement;
    	    	}
    		}
    	}
    	return null;
    }

    public Set<String> getEntitySet() {
    	Set<String> entitySet = super.getEntitySet();
    	for(MyAbstractExpression expression : expressionList)
    		entitySet.addAll(expression.getEntitySet());
    	return entitySet;
    }
}
