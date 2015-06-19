package org.purescript.truffle.node;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class TaggedNode extends ExpressionNode {
    public final String name;
    public final String[] fields;

    public TaggedNode(String name, String[] fields) {
        this.name = name;
        this.fields = fields;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        final Map<String, Object> result = new HashMap<>();
        result.put("$tag", name);

        for (final String field : fields) {
            result.put(field, lookupVariable(frame, field));
        }

        return result;
    }

    @Override
    public String toString() {
        return name + " " + String.join(" ", fields);
    }
}
