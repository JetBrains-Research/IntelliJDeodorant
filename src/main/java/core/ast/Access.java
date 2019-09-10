package core.ast;

public enum Access {
    NONE, PUBLIC, PRIVATE, PROTECTED;

    public String toString() {
        switch(this) {
            case PUBLIC: return "public";
            case PRIVATE: return "private";
            case PROTECTED: return "protected";
            default: return "";
        }
    }
}
