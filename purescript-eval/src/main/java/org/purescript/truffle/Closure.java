package org.purescript.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.RootCallTarget;

import org.purescript.truffle.node.CallRootNode;

public final class Closure {
    public final CallRootNode callNode;
    public final RootCallTarget callTarget;

    public Closure(CallRootNode callNode) {
        this.callNode = callNode;
        this.callTarget = Truffle.getRuntime().createCallTarget(callNode);
    }

    @Override
    public String toString() {
        return "(" + callNode + ")";
    }
}
