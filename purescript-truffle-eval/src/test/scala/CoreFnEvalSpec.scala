package org.purescript.truffle

import org.specs2.mutable.Specification

class CoreFnEvalSpec extends Specification {
  "modules of simple application" >> {
    "id id 42.0" >> {
      val Main = CoreFnModuleName(List("Main"))
      val id = CoreFnIdentIdent("id")
      val a = CoreFnIdentIdent("a")
      val x = CoreFnIdentIdent("x")

      val frame =
        CoreFnEval.evaluate(
          CoreFnModule(
            Main,
            List(
              CoreFnBindNonRec(
                id,
                CoreFnExprAbs(
                  a,
                  CoreFnExprVar(CoreFnQualifiedIdent(None, a))
                )
              ),
              CoreFnBindNonRec(
                x,
                CoreFnExprApp(
                  CoreFnExprApp(
                    CoreFnExprVar(CoreFnQualifiedIdent(Some(Main), id)),
                    CoreFnExprVar(CoreFnQualifiedIdent(Some(Main), id))
                  ),
                  CoreFnExprLiteral(
                    CoreFnLiteralDouble(42.0)
                  )
                )
              )
            )
          )
        )

      val slot = frame.getFrameDescriptor.findFrameSlot("Main.x")
      frame.getValue(slot) must_== 42.0
    }
  }
}
