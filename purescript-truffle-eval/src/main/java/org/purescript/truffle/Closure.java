package org.purescript.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

import org.purescript.truffle.node.CallRootNode;

public final class Closure {
    public final CallRootNode callNode;
    public final MaterializedFrame frame;
    public final RootCallTarget callTarget;

    public Closure(CallRootNode callNode, MaterializedFrame frame) {
        this.callNode = callNode;
        this.frame = frame;
        this.callTarget = Truffle.getRuntime().createCallTarget(callNode);
    }

    @Override
    public String toString() {
        return "(" + callNode + ")";
    }
}
