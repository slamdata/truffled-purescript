package org.purescript.truffle;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeInfo;

public final class Closure {
    public final RootCallTarget callTarget;

    public Closure(RootCallTarget callTarget) {
        this.callTarget = callTarget;
    }
}
