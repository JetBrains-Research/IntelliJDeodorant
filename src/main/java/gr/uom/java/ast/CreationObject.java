package gr.uom.java.ast;

public abstract class CreationObject {
	private final TypeObject type;
	ASTInformation creation;
	
	CreationObject(TypeObject type) {
		this.type = type;
	}

	public TypeObject getType() {
		return type;
	}
}
