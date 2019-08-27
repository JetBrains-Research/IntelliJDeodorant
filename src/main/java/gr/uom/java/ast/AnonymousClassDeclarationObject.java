package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;

public class AnonymousClassDeclarationObject extends ClassDeclarationObject {
	private ASTInformation anonymousClassDeclaration;
	private ClassObject classObject;
	
	public void setAnonymousClassDeclaration(PsiAnonymousClass anonymous) {
		this.anonymousClassDeclaration = ASTInformationGenerator.generateASTInformation(anonymous);
	}
	
	public PsiAnonymousClass getAnonymousClassDeclaration() {
		return (PsiAnonymousClass)anonymousClassDeclaration.recoverASTNode();
	}

	public ClassObject getClassObject() {
		return classObject;
	}

	public void setClassObject(ClassObject classObject) {
		this.classObject = classObject;
	}

	public PsiType getITypeRoot() {
		return anonymousClassDeclaration.getITypeRoot();
	}

	public PsiFile getIFile() {
		if(classObject != null) {
			return classObject.getIFile();
		}
		return null;
	}

	public TypeObject getSuperclass() {
		return null;
	}

	protected void accessedFieldFromThisClass(Set<FieldObject> fields, FieldInstructionObject fieldInstruction) {
		List<FieldObject> allFields = new ArrayList<FieldObject>(fieldList);
		if(classObject != null) {
			//add the fields of the class in which the anonymous class is declared
			allFields.addAll(classObject.fieldList);
		}
		for(FieldObject field : allFields) {
			if(field.equals(fieldInstruction)) {
				if(!fields.contains(field))
					fields.add(field);
				break;
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("\n\n").append("Fields:");
        for(FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Methods:");
        for(MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
	}
}
