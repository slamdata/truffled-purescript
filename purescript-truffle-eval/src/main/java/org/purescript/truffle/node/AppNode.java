package org.purescript.truffle.node;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import org.purescript.truffle.Closure;
import org.purescript.truffle.PureScriptException;

@NodeInfo(shortName = "Application")
public final class AppNode extends ExpressionNode {
    @Child
    public ExpressionNode func;

    @Child
    public ExpressionNode arg;

    public AppNode(ExpressionNode func, ExpressionNode arg) {
        this.func = func;
        this.arg = arg;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        final Closure abs;
        try {
            abs = func.executeClosure(frame);
        } catch (UnexpectedResultException e) {
            throw new PureScriptException("Expression did not result in a function");
        }
        final DirectCallNode callNode = Truffle.getRuntime().createDirectCallNode(abs.callTarget);
        final MaterializedFrame materializedFrame = frame.materialize();
        final Object[] args = new Object[] { materializedFrame, arg.executeGeneric(frame) };
        return callNode.call(frame, args);
    }
}
