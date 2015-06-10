package org.purescript.truffle.node;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;

import org.purescript.truffle.Closure;
import org.purescript.truffle.Types;
import org.purescript.truffle.TypesGen;

@TypeSystemReference(Types.class)
@NodeInfo(description = "The abstract base node for all expressions")
public abstract class ExpressionNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return TypesGen.expectDouble(executeGeneric(frame));
    }

    public Closure executeClosure(VirtualFrame frame) throws UnexpectedResultException {
        return TypesGen.expectClosure(executeGeneric(frame));
    }
}
