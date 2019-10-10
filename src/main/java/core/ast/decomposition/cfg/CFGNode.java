package core.ast.decomposition.cfg;

import com.intellij.psi.PsiStatement;
import core.ast.decomposition.AbstractStatement;

public class CFGNode extends GraphNode implements Comparable<CFGNode> {
    private AbstractStatement statement;
    private BasicBlock basicBlock;
    private PDGNode pdgNode;
    private volatile int hashCode = 0;

    public CFGNode(AbstractStatement statement) {
        super();
        this.statement = statement;
    }

    public AbstractStatement getStatement() {
        return statement;
    }

    public PsiStatement getASTStatement() {
        return statement.getStatement();
    }

    public boolean isLeader() {
        return isFirst() || isJoin() || immediatelyFollowsBranchNode();
    }

    public boolean immediatelyFollowsBranchNode() {
        for (GraphEdge edge : incomingEdges) {
            CFGNode srcNode = (CFGNode) edge.src;
            if (srcNode.isBranch())
                return true;
        }
        return false;
    }

    public boolean isFirst() {
        int numberOfNonLoopbackFlows = 0;
        for (GraphEdge edge : incomingEdges) {
            Flow flow = (Flow) edge;
            if (!flow.isLoopbackFlow())
                numberOfNonLoopbackFlows++;
        }
        return numberOfNonLoopbackFlows == 0;
    }

    public boolean isBranch() {
        return outgoingEdges.size() > 1 || this instanceof CFGBranchNode;
    }

    public boolean isJoin() {
        return incomingEdges.size() > 1;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public PDGNode getPDGNode() {
        return pdgNode;
    }

    public void setPDGNode(PDGNode pdgNode) {
        this.pdgNode = pdgNode;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof CFGNode) {
            CFGNode node = (CFGNode) o;
            return this.getId() == node.getId();
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + getId();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return id + "\t" + statement.toString();
    }

    public int compareTo(CFGNode node) {
        return Integer.compare(this.getId(), node.getId());
    }
}
