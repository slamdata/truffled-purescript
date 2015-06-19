package org.purescript.truffle.node;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import org.purescript.truffle.Closure;
import org.purescript.truffle.PureScriptException;
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

    public static Object lookupVariable(VirtualFrame frame, String name) {
        Object value = null;
        Frame lookupFrame = frame;
        while (true) {
            final FrameSlot slot = lookupFrame.getFrameDescriptor().findFrameSlot(name);
            if (slot != null) {
                value = lookupFrame.getValue(slot);
                if (value != null) {
                    break;
                }
            }

            Object[] args = lookupFrame.getArguments();
            if (args.length > 0 && args[0] instanceof Frame) {
                lookupFrame = (Frame) lookupFrame.getArguments()[0];
            } else {
                break;
            }
        }

        if (value == null) {
            throw new PureScriptException("Undefined variable: " + name);
        }
        return value;
    }
}
