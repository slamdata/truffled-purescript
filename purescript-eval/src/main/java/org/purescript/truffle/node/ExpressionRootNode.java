package org.purescript.truffle.node;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ExpressionRootNode extends RootNode {
    @Child
    private ExpressionNode body;

    public ExpressionRootNode(ExpressionNode body, FrameDescriptor frameDescriptor) {
        super(null, frameDescriptor);
        this.body = body;
    }

    public ExpressionNode getBody() {
        return body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }
}
