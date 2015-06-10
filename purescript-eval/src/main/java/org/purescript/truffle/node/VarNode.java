package org.purescript.truffle.node;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;

import org.purescript.truffle.PureScriptException;

public class VarNode extends ExpressionNode {
    public final String name;

    public VarNode(String name) {
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object value = null;
        Frame lookupFrame = frame;
        while (true) {
            final FrameSlot slot = lookupFrame.getFrameDescriptor().findFrameSlot(name);
            if (slot != null) {
                value = lookupFrame.getValue(slot);
                break;
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
