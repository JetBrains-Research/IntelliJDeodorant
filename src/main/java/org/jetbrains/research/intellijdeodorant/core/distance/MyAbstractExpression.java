package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.FieldInstructionObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;
import org.jetbrains.research.intellijdeodorant.core.ast.ASTReader;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodInvocationObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

class MyAbstractExpression {

    private AbstractExpression expression;
    private MyCompositeStatement owner;
    private List<MyMethodInvocation> methodInvocationList;
    private List<MyAttributeInstruction> attributeInstructionList;

    public MyAbstractExpression(AbstractExpression expression) {
        this.expression = expression;
        this.owner = null;
        this.methodInvocationList = new ArrayList<>();
        this.attributeInstructionList = new ArrayList<>();
        SystemObject system = ASTReader.getSystemObject();

        List<FieldInstructionObject> fieldInstructions = expression.getFieldInstructions();
        for (FieldInstructionObject fio : fieldInstructions) {
            if (system.getClassObject(fio.getOwnerClass()) != null && !fio.isStatic()) {
                MyAttributeInstruction myAttributeInstruction = new MyAttributeInstruction(fio.getOwnerClass(), fio.getType().toString(), fio.getName());

                if (!attributeInstructionList.contains(myAttributeInstruction))
                    attributeInstructionList.add(myAttributeInstruction);
            }
        }

        List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
        for (MethodInvocationObject mio : methodInvocations) {
            if (system.getClassObject(mio.getOriginClassName()) != null && !mio.isStatic()) {
                MethodInvocationObject methodInvocation;
                if (!isAccessor(mio, system) && (methodInvocation = recurseDelegations(mio, system)) != null) {
                    if (!isAccessor(methodInvocation, system)) {
                        MyMethodInvocation myMethodInvocation = new MyMethodInvocation(methodInvocation.getOriginClassName(),
                                methodInvocation.getMethodName(), methodInvocation.getReturnType().toString(), methodInvocation.getParameterList());
                        if (!methodInvocationList.contains(myMethodInvocation))
                            methodInvocationList.add(myMethodInvocation);
                    }
                }
            }
        }
    }

    private boolean isAccessor(MethodInvocationObject methodInvocation, SystemObject system) {
        FieldInstructionObject fieldInstruction = null;
        if ((system.containsGetter(methodInvocation)) != null) {
            fieldInstruction = system.containsGetter(methodInvocation);
        } else if ((system.containsSetter(methodInvocation)) != null) {
            fieldInstruction = system.containsSetter(methodInvocation);
        } else if ((system.containsCollectionAdder(methodInvocation)) != null) {
            fieldInstruction = system.containsCollectionAdder(methodInvocation);
        }

        if (fieldInstruction != null) {
            MyAttributeInstruction myAttributeInstruction =
                    new MyAttributeInstruction(fieldInstruction.getOwnerClass(), fieldInstruction.getType().toString(), fieldInstruction.getName());

            if (!attributeInstructionList.contains(myAttributeInstruction))
                attributeInstructionList.add(myAttributeInstruction);
            return true;
        }
        return false;
    }

    private MethodInvocationObject recurseDelegations(MethodInvocationObject methodInvocation, SystemObject system) {
        MethodInvocationObject delegation;
        if ((delegation = system.containsDelegate(methodInvocation)) != null && system.getClassObject(delegation.getOriginClassName()) != null)
            return recurseDelegations(delegation, system);
        else
            return methodInvocation;
    }

    public void setMethodInvocationList(List<MyMethodInvocation> list) {
        this.methodInvocationList = list;
    }

    public void setAttributeInstructionList(List<MyAttributeInstruction> list) {
        this.attributeInstructionList = list;
    }

    public boolean containsAttributeInstruction(MyAttributeInstruction instruction) {
        return attributeInstructionList.contains(instruction);
    }

    public boolean containsMethodInvocation(MyMethodInvocation invocation) {
        return methodInvocationList.contains(invocation);
    }

    public void addMethodInvocation(MyMethodInvocation myMethodInvocation) {
        if (!methodInvocationList.contains(myMethodInvocation))
            methodInvocationList.add(myMethodInvocation);
    }

    public void addAttributeInstruction(MyAttributeInstruction myAttributeInstruction) {
        if (!attributeInstructionList.contains(myAttributeInstruction))
            attributeInstructionList.add(myAttributeInstruction);
    }

    public int getNumberOfAttributeInstructions() {
        return this.attributeInstructionList.size();
    }

    public int getNumberOfMethodInvocations() {
        return this.methodInvocationList.size();
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return methodInvocationList.listIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return attributeInstructionList.listIterator();
    }

    public void setOwner(MyCompositeStatement owner) {
        this.owner = owner;
    }

    public MyCompositeStatement getOwner() {
        return this.owner;
    }

    public AbstractExpression getExpression() {
        return this.expression;
    }

    public void setExpression(AbstractExpression expression) {
        this.expression = expression;
    }

    public String toString() {
        return this.expression.toString();
    }

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
        int index = attributeInstructionList.indexOf(myAttributeInstruction);
        if (index != -1) {
            MyAttributeInstruction attributeInstruction = attributeInstructionList.get(index);
            attributeInstruction.setReference(reference);
        }
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<>();
        ListIterator<MyAttributeInstruction> attributeInstructionIterator = getAttributeInstructionIterator();
        while (attributeInstructionIterator.hasNext()) {
            MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
            if (!attributeInstruction.isReference())
                set.add(attributeInstruction.toString());
        }
        ListIterator<MyMethodInvocation> methodInvocationIterator = getMethodInvocationIterator();
        while (methodInvocationIterator.hasNext()) {
            MyMethodInvocation methodInvocation = methodInvocationIterator.next();
            set.add(methodInvocation.toString());
        }
        return set;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof MyAbstractExpression) {
            MyAbstractExpression myAbstractExpression = (MyAbstractExpression) o;
            return this.expression.equals(myAbstractExpression.expression);
        }
        return false;
    }
}
