package org.purescript.truffle.node;

import org.purescript.truffle.Closure;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "Closure")
public final class ClosureNode extends ExpressionNode {
    public final Closure abs;

    public ClosureNode(Closure abs) {
        this.abs = abs;
    }

    public Object executeGeneric(VirtualFrame frame) {
        return abs;
    }
}
