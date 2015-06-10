package org.purescript.truffle

import argonaut.Parse
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.{ FrameDescriptor, MaterializedFrame, VirtualFrame }

object CoreFnMain {
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
        val rootNode = Truffle.getRuntime().createCallTarget(callNode)
        new node.ClosureNode(new Closure(rootNode))

      case CoreFnExprApp(func, arg) =>
        new node.AppNode(rec(func), rec(arg))

      case CoreFnExprVar(ident) =>
        new node.VarNode(qualifiedIdentName(ident))

      case CoreFnExprLiteral(literal) =>
        literal match {
          case CoreFnLiteralDouble(d) =>
            new node.DoubleLiteralNode(d)
        }
    }
  }

  def loadModule(mf: MaterializedFrame)(cfm: CoreFnModule) = {
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

  def main(args: Array[String]): Unit = {
    import java.nio.charset.StandardCharsets.UTF_8
    import java.nio.file.{ Files, Paths }

    if (args.isEmpty) {
      System.err.println("Usage: TODO")
      return
    }

    val path = Paths.get(args(0))
    val content = new String(Files.readAllBytes(path), UTF_8)

    val result = Parse.decodeEither[CoreFnModule](content)

    val vf = Truffle.getRuntime.createVirtualFrame(Array.empty, FrameDescriptor.create)
    val mf = vf.materialize()

    result.fold(error, loadModule(mf))
  }
}
