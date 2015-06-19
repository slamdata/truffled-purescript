package org.purescript.truffle

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.{ Frame, FrameDescriptor, FrameSlot, MaterializedFrame }

object CoreFnEval {
  def moduleName(mn: CoreFnModuleName) =
    mn.properNames.mkString(".")

  def qualifiedIdentName(qi: CoreFnQualifiedIdent) = {
    val moduleStrings =
      qi.moduleName.fold(List.empty[String])(mn => List(moduleName(mn)))

    (moduleStrings :+ identName(qi.ident)).mkString(".")
  }

  def identName(i: CoreFnIdent) =
    i match {
      case CoreFnIdentIdent(s) => s
      case CoreFnIdentOp(s) => s
    }

  def exprToNode(fd: FrameDescriptor)(expr: CoreFnExpr): node.ExpressionNode = {
    val rec = exprToNode(fd)(_)

    expr match {
      case CoreFnExprAbs(arg, body) =>
        val callNode =
          new node.CallRootNode(identName(arg), rec(body), fd)
        new node.ClosureNode(callNode)

      case CoreFnExprApp(func, arg) =>
        new node.AppNode(rec(func), rec(arg))

      case CoreFnExprVar(ident) =>
        new node.VarNode(qualifiedIdentName(ident))

      case CoreFnExprLiteral(literal) =>
        literal match {
          case CoreFnLiteralDouble(d) =>
            new node.DoubleLiteralNode(d)
        }

      case CoreFnExprConstructor(name, fields) =>
        val fieldNames = fields.map(identName)
        fieldNames.foldRight[node.ExpressionNode](new node.TaggedNode(name, fieldNames.toArray)) { (field, n) =>
          new node.ClosureNode(new node.CallRootNode(field, n, fd))
        }
    }
  }

  def loadModule(mf: MaterializedFrame, cfm: CoreFnModule) = {
    val qualify = (i: CoreFnIdent) => CoreFnQualifiedIdent(Some(cfm.name), i)
    val vf = Truffle.getRuntime.createVirtualFrame(Array.empty, FrameDescriptor.create)
    val fd = vf.getFrameDescriptor

    cfm.decls.foreach {
      case CoreFnBindNonRec(ident, expr) =>
        val name = qualifiedIdentName(qualify(ident))
        val slot = mf.getFrameDescriptor.findOrAddFrameSlot(name)

        val exprNode = exprToNode(fd)(expr)
        val rootNode = new node.ExpressionRootNode(exprNode, fd)
        val target = Truffle.getRuntime.createCallTarget(rootNode)
        val evaluated = target.call(mf)
        mf.setObject(slot, evaluated)
    }
  }

  def dumpFrame(frame: Frame) = {
    val slots = frame.getFrameDescriptor.getSlots.toArray[FrameSlot](Array.empty)
    slots.foreach { slot =>
      val identifier = slot.getIdentifier
      val value = frame.getValue(slot)
      println(s"$identifier: $value")
    }
  }

  def evaluate(m: CoreFnModule) = {
    val vf = Truffle.getRuntime.createVirtualFrame(Array.empty, FrameDescriptor.create)
    val mf = vf.materialize()
    loadModule(mf, m)
    mf
  }
}
