package org.purescript.truffle.node;

import org.purescript.truffle.Closure;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "Closure")
public final class ClosureNode extends ExpressionNode {
    public final CallRootNode callNode;

    public ClosureNode(CallRootNode callNode) {
        this.callNode = callNode;
    }

    public Object executeGeneric(VirtualFrame frame) {
        return new Closure(callNode, frame.materialize());
    }

    @Override
    public String toString() {
        return callNode.toString();
    }
}
