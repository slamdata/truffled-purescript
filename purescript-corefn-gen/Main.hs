{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE DataKinds #-}

module Main where

import qualified Data.Aeson as J
import qualified Data.ByteString.Lazy.Char8 as BSL

import Control.Monad.Supply (runSupplyT)
import Control.Monad.Trans.Writer (WriterT(), runWriterT)
import Data.Foldable (traverse_)
import Language.PureScript.AST.Declarations (ImportDeclarationType(..), Declaration(..), Module(..))
import Language.PureScript.CoreFn.Ann (Ann())
import Language.PureScript.CoreFn.Binders (Binder(..))
import Language.PureScript.CoreFn.Desugar (moduleToCoreFn)
import Language.PureScript.CoreFn.Expr (Bind(..), CaseAlternative(..), Expr(..))
import Language.PureScript.CoreFn.Literals (Literal(..))
import Language.PureScript.CoreFn.Meta (ConstructorType(..), Meta(..))
import Language.PureScript.Environment (initEnvironment)
import Language.PureScript.Errors (MultipleErrors())
import Language.PureScript.Linter (lint)
import Language.PureScript.Linter.Exhaustive (checkExhaustiveModule)
import Language.PureScript.Names
import Language.PureScript.Parser (parseModulesFromFiles)
import Language.PureScript.Sugar (desugar)
import Language.PureScript.Sugar.BindingGroups (createBindingGroups, collapseBindingGroups)
import Language.PureScript.TypeChecker (typeCheckModule)
import Language.PureScript.TypeChecker.Monad (runCheck')
import System.Environment (getArgs)
import qualified Language.PureScript.CoreFn.Module as CFM

addDefaultImport :: ModuleName -> Module -> Module
addDefaultImport toImport m@(Module ss coms mn decls exps)  =
  if isExistingImport `any` decls || mn == toImport then m
  else Module ss coms mn (ImportDeclaration toImport Implicit Nothing : decls) exps
  where
  isExistingImport (ImportDeclaration mn' _ _) | mn' == toImport = True
  isExistingImport (PositionedDeclaration _ _ d) = isExistingImport d
  isExistingImport _ = False

importPrim :: Module -> Module
importPrim = addDefaultImport (ModuleName [ProperName "Prim"])

importPrelude :: Module -> Module
importPrelude = addDefaultImport (ModuleName [ProperName "Prelude"])

compileModules :: [(String, String)] -> Either MultipleErrors [CFM.Module Ann]
compileModules moduleFiles = do
  ms <- parseModulesFromFiles id moduleFiles
  traverse (f . snd) ms
    where
      f :: Module -> Either MultipleErrors (CFM.Module Ann)
      f m@(Module _ _ moduleName _ _) = evalWriterT $ do
        let env = initEnvironment
        lint m
        ([desugared], _) <- runSupplyT 0 $ desugar [] [m]
        (checked@(Module ss coms _ elaborated exps), env') <- runCheck' env $ typeCheckModule desugared
        checkExhaustiveModule env' checked
        regrouped <- createBindingGroups moduleName . collapseBindingGroups $ elaborated
        let mod' = Module ss coms moduleName regrouped exps
        return $ moduleToCoreFn env' mod'

runConstructorType :: ConstructorType -> String
runConstructorType ProductType = "product-type"
runConstructorType SumType = "sum-type"

jsonMeta :: Meta -> J.Value
jsonMeta (IsConstructor ct fields) =
  J.object [ "tag" J..= ("is-constructor" :: J.Value)
           , "constructor-type" J..= runConstructorType ct
           , "fields" J..= (jsonIdent <$> fields)
           ]
jsonMeta IsNewtype =
  J.object [ "tag" J..= ("is-newtype" :: J.Value)
           ]

jsonAnn :: Ann -> J.Value
jsonAnn (sp, cs, tys, ms) =
  J.object [ "meta" J..= (jsonMeta <$> ms)
           ]

jsonLiteral :: Literal (Expr Ann) -> J.Value
jsonLiteral (StringLiteral s) =
  J.object [ "tag" J..= ("string" :: J.Value)
           , "string" J..= s
           ]
jsonLiteral (NumericLiteral d) =
  J.object [ "tag" J..= ("numeric" :: J.Value)
           , "numeric" J..= either fromIntegral id d
           ]

jsonExpr :: Expr Ann -> J.Value
jsonExpr (App ann func arg) =
  J.object [ "tag" J..= ("app" :: J.Value)
           -- , "ann" J..= jsonAnn ann
           , "func" J..= jsonExpr func
           , "arg" J..= jsonExpr arg
           ]
jsonExpr (Var ann ident) =
  J.object [ "tag" J..= ("var" :: J.Value)
           -- , "ann" J..= jsonAnn ann
           , "ident" J..= jsonQualified jsonIdent ident
           ]
jsonExpr (Literal ann lit) =
  J.object [ "tag" J..= ("literal" :: J.Value)
           -- , "ann" J..= jsonAnn ann
           , "literal" J..= jsonLiteral lit
           ]
jsonExpr (Abs ann arg body) =
  J.object [ "tag" J..= ("abs" :: J.Value)
           , "arg" J..= jsonIdent arg
           , "body" J..= jsonExpr body
           ]
jsonExpr (Case ann es as) =
  J.object [ "tag" J..= ("case" :: J.Value)
           -- , "ann" J..= jsonAnn ann
           , "exprs" J..= (jsonExpr <$> es)
           , "alternatives" J..= (jsonAlternative <$> as)
           ]
jsonExpr (Constructor ann tn cn fns) =
  J.object [ "tag" J..= ("constructor" :: J.Value)
           , "typeName" J..= runProperName tn
           , "constructor-name" J..= runProperName cn
           , "field-names" J..= (jsonIdent <$> fns)
           ]

jsonAlternative :: CaseAlternative Ann -> J.Value
jsonAlternative (CaseAlternative bs r) =
  J.object [ "binders" J..= (jsonBinder <$> bs)
           , "result" J..= either left right r
           ]
  where left _ = error "TODO: Left of CaseAlternative result"
        right expr = J.object [ "tag" J..= ("value" :: J.Value)
                              , "value" J..= jsonExpr expr
                              ]

jsonBinder :: Binder Ann -> J.Value
jsonBinder (NullBinder ann) =
  J.object [ "tag" J..= ("null" :: J.Value)
           , "ann" J..= jsonAnn ann
           ]
jsonBinder (VarBinder ann ident) =
  J.object [ "tag" J..= ("var" :: J.Value)
           , "ann" J..= jsonAnn ann
           , "ident" J..= jsonIdent ident
           ]
jsonBinder (ConstructorBinder ann typeName constructorName binders) =
  J.object [ "tag" J..= ("constructor" :: J.Value)
           , "ann" J..= jsonAnn ann
           , "type-name" J..= jsonQualified (J.toJSON . runProperName) typeName
           , "constructor-name" J..= jsonQualified (J.toJSON . runProperName) constructorName
           , "binders" J..= (jsonBinder <$> binders)
           ]

jsonQualified :: (a -> J.Value) -> Qualified a -> J.Value
jsonQualified f (Qualified mn a) =
  J.object [ "module-name" J..= (jsonModuleName <$> mn)
           , "value" J..= f a
           ]

jsonIdent :: Ident -> J.Value
jsonIdent (Ident s) =
  J.object [ "type" J..= ("ident" :: J.Value)
           , "ident" J..= s
           ]
jsonIdent (Op s) =
  J.object [ "type" J..= ("op" :: J.Value)
           , "op" J..= s
           ]

jsonBind :: Bind Ann -> J.Value
jsonBind (NonRec ident expr) =
  J.object [ "tag" J..= ("non-rec" :: J.Value)
           , "ident" J..= jsonIdent ident
           , "expr" J..= jsonExpr expr
           ]

jsonModuleName :: ModuleName -> J.Value
jsonModuleName (ModuleName properNames) =
  J.toJSON (runProperName <$> properNames)

jsonModule :: CFM.Module Ann -> J.Value
jsonModule (CFM.Module comments name imports exports foreigns decls) =
  J.object [ "name" J..= jsonModuleName name
           , "decls" J..= (jsonBind <$> decls)
           ]

isMain :: CFM.Module a -> Bool
isMain (CFM.Module _ (ModuleName [ProperName "Main"]) _ _ _ _) = True
isMain _ = False

dumpModules :: [CFM.Module Ann] -> IO ()
dumpModules = traverse_ (BSL.putStrLn . J.encode . jsonModule)

evalWriterT :: Functor m => WriterT w m a -> m a
evalWriterT = fmap fst . runWriterT

main :: IO ()
main = do
  files <- getArgs
  contents <- traverse readFile files
  either print dumpModules . compileModules $ zip files contents
