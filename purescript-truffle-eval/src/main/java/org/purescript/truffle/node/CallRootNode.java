package org.purescript.truffle.node;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class CallRootNode extends RootNode {
    @Child
    private ExpressionNode body;

    private String name;

    public CallRootNode(String name, ExpressionNode body, FrameDescriptor frameDescriptor) {
        super(null, frameDescriptor);
        this.body = body;
        this.name = name;
    }

    public ExpressionNode getBody() {
        return body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        FrameSlot slot = getFrameDescriptor().addFrameSlot(name);
        frame.setObject(slot, frame.getArguments()[1]);
        return body.executeGeneric(frame);
    }

    @Override
    public String toString() {
        return "\\" + name + " -> " + body;
    }
}
