package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;

import java.util.*;

public class MyClass {

    private final String name;
    private String superclass;
    private final List<MyAttribute> attributeList;
    private final List<MyMethod> methodList;
    private ClassObject classObject;
    private volatile int hashCode = 0;

    public MyClass(String name) {
        this.name = name;
        this.attributeList = new ArrayList<>();
        this.methodList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getSuperclass() {
        return superclass;
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    public ClassObject getClassObject() {
        return this.classObject;
    }

    public void setClassObject(ClassObject classObject) {
        this.classObject = classObject;
    }

    public void addAttribute(MyAttribute attribute) {
        if (!attributeList.contains(attribute))
            attributeList.add(attribute);
    }

    public MyAttribute getAttribute(MyAttributeInstruction attributeInstruction) {
        for (MyAttribute attribute : attributeList) {
            if (attribute.equals(attributeInstruction))
                return attribute;
        }
        return null;
    }

    public List<MyMethod> getMethodList() {
        return methodList;
    }

    public void addMethod(MyMethod method) {
        if (!methodList.contains(method))
            methodList.add(method);
    }

    public void removeMethod(MyMethod method) {
        methodList.remove(method);
    }

    public void removeAttribute(MyAttribute attribute) {
        attributeList.remove(attribute);
    }

    public List<MyAttribute> getAttributeList() {
        return attributeList;
    }

    public ListIterator<MyAttribute> getAttributeIterator() {
        return attributeList.listIterator();
    }

    public ListIterator<MyMethod> getMethodIterator() {
        return methodList.listIterator();
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<>();
        for (MyAttribute attribute : attributeList) {
            if (!attribute.isReference())
                set.add(attribute.toString());
        }
        for (MyMethod method : methodList) {
            set.add(method.toString());
        }
        return set;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof MyClass) {
            MyClass myClass = (MyClass) o;
            return this.name.equals(myClass.name);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + name.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return name;
    }
}
