package org.purescript.truffle

import argonaut.{ ACursor, DecodeJson, DecodeResult }

sealed trait CoreFnIdent
case class CoreFnIdentIdent(ident: String) extends CoreFnIdent
case class CoreFnIdentOp(op: String) extends CoreFnIdent

object CoreFnIdent {
  implicit val coreFnIdentDecodeJson: DecodeJson[CoreFnIdent] =
    DecodeJson { c =>
      val cursor = (c --\ "type")
      cursor.as[String].flatMap {
        case "ident" => (c --\ "ident").as[String].map(CoreFnIdentIdent)
        case "op" => (c --\ "op").as[String].map(CoreFnIdentOp)
        case _ => DecodeResult.fail("Invalid ident type", cursor.history)
      }
    }
}

case class CoreFnQualifiedIdent(moduleName: Option[CoreFnModuleName], ident: CoreFnIdent)

object CoreFnQualifiedIdent {
  implicit val coreFnQualifiedIdentDecodeJson: DecodeJson[CoreFnQualifiedIdent] =
    DecodeJson(c =>
      for {
        moduleName <- (c --\ "module-name").as[Option[CoreFnModuleName]]
        ident <- (c --\ "ident").as[CoreFnIdent]
      } yield CoreFnQualifiedIdent(moduleName, ident)
    )
}

sealed trait CoreFnLiteral
case class CoreFnLiteralDouble(d: Double) extends CoreFnLiteral
case class CoreFnLiteralString(s: String) extends CoreFnLiteral

object CoreFnLiteral {
  implicit val coreFnLiteralDecodeJson: DecodeJson[CoreFnLiteral] =
    DecodeJson { c =>
      val cursor = (c --\ "tag")
      cursor.as[String].flatMap {
        case "numeric" =>
          (c --\ "numeric").as[Double].map(CoreFnLiteralDouble)
        case "string" =>
          (c --\ "string").as[String].map(CoreFnLiteralString)
      }
    }
}

sealed trait CoreFnExpr
case class CoreFnExprApp(func: CoreFnExpr, arg: CoreFnExpr) extends CoreFnExpr
case class CoreFnExprAbs(arg: CoreFnIdent, body: CoreFnExpr) extends CoreFnExpr
case class CoreFnExprVar(ident: CoreFnQualifiedIdent) extends CoreFnExpr
case class CoreFnExprLiteral(ident: CoreFnLiteral) extends CoreFnExpr
case class CoreFnExprConstructor(name: String, fieldNames: List[CoreFnIdent]) extends CoreFnExpr

object CoreFnExpr {
  implicit val coreFnExprDecodeJson: DecodeJson[CoreFnExpr] =
    DecodeJson { c =>
      val cursor = (c --\ "tag")
      cursor.as[String].flatMap {
        case "app" =>
          for {
            func <- (c --\ "func").as[CoreFnExpr]
            arg <- (c --\ "arg").as[CoreFnExpr]
          } yield CoreFnExprApp(func, arg)
        case "abs" =>
          for {
            arg <- (c --\ "arg").as[CoreFnIdent]
            body <- (c --\ "body").as[CoreFnExpr]
          } yield CoreFnExprAbs(arg, body)
        case "var" =>
          (c --\ "ident").as[CoreFnQualifiedIdent].map(CoreFnExprVar)
        case "literal" =>
          (c --\ "literal").as[CoreFnLiteral].map(CoreFnExprLiteral)
        case "constructor" =>
          for {
            constructorName <- (c --\ "constructor-name").as[String]
            fieldNames <- (c --\ "field-names").as[List[CoreFnIdent]]
          } yield CoreFnExprConstructor(constructorName, fieldNames)
      }
    }
}

sealed trait CoreFnBind
case class CoreFnBindNonRec(ident: CoreFnIdent, expr: CoreFnExpr) extends CoreFnBind

object CoreFnBind {
  implicit val coreFnBindDecodeJson: DecodeJson[CoreFnBind] =
    DecodeJson { c =>
      val cursor = (c --\ "tag")
      cursor.as[String].flatMap {
        case "non-rec" =>
          for {
            ident <- (c --\ "ident").as[CoreFnIdent]
            expr <- (c --\ "expr").as[CoreFnExpr]
          } yield CoreFnBindNonRec(ident, expr)
        case _ => DecodeResult.fail("TODO", cursor.history)
      }
    }
}

case class CoreFnModuleName(properNames: List[String])

object CoreFnModuleName {
  implicit val coreFnModuleNameDecodeJson: DecodeJson[CoreFnModuleName] =
    DecodeJson(c =>
      c.as[List[String]].map(CoreFnModuleName.apply)
    )
}

case class CoreFnModule(name: CoreFnModuleName, decls: List[CoreFnBind])

object CoreFnModule {
  implicit val coreFnModuleDecodeJson: DecodeJson[CoreFnModule] =
    DecodeJson(c =>
      for {
        name <- (c --\ "name").as[CoreFnModuleName]
        decls <- (c --\ "decls").as[List[CoreFnBind]]
      } yield CoreFnModule(name, decls)
    )
}
