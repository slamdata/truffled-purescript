package org.purescript.truffle.node;

import com.oracle.truffle.api.frame.VirtualFrame;

public class VarNode extends ExpressionNode {
    public final String name;

    public VarNode(String name) {
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return lookupVariable(frame, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
