{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE DataKinds #-}

module Main where

import qualified Data.Aeson as J
import qualified Data.ByteString.Lazy.Char8 as BSL

import Control.Monad.Trans.Reader (ReaderT(), runReaderT)
import Control.Monad.Trans.Writer (WriterT(), runWriterT)
import Data.Foldable (traverse_)
import Language.PureScript (compile)
import Language.PureScript.AST.Declarations (ImportDeclarationType(..), Declaration(..), Module(..))
import Language.PureScript.CoreFn.Ann (Ann())
import Language.PureScript.CoreFn.Binders (Binder(..))
import Language.PureScript.CoreFn.Expr (Bind(..), CaseAlternative(..), Expr(..))
import Language.PureScript.CoreFn.Literals (Literal(..))
import Language.PureScript.Errors (MultipleErrors())
import Language.PureScript.Names
import Language.PureScript.Options
import Language.PureScript.Parser
import System.Environment (getArgs)
import qualified Language.PureScript.CoreFn.Module as CFM

addDefaultImport :: ModuleName -> Module -> Module
addDefaultImport toImport m@(Module coms mn decls exps)  =
  if isExistingImport `any` decls || mn == toImport then m
  else Module coms mn (ImportDeclaration toImport Implicit Nothing : decls) exps
  where
  isExistingImport (ImportDeclaration mn' _ _) | mn' == toImport = True
  isExistingImport (PositionedDeclaration _ _ d) = isExistingImport d
  isExistingImport _ = False

importPrim :: Module -> Module
importPrim = addDefaultImport (ModuleName [ProperName "Prim"])

importPrelude :: Module -> Module
importPrelude = addDefaultImport (ModuleName [ProperName "Prelude"])

compileModules :: [(String, String)] -> ReaderT (Options 'Compile) (WriterT MultipleErrors (Either MultipleErrors)) [CFM.Module Ann]
compileModules moduleFiles = do
  r <- parseModulesFromFiles id moduleFiles
  fmap (\(a, _, _, _) -> a) . compile $ fmap snd r

jsonAnn :: Ann -> J.Value
jsonAnn (sp, cs, tys, ms) =
  J.object []

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
           , "ident" J..= jsonQualified ident
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
jsonBinder (NullBinder _) =
  J.object [ "tag" J..= ("null" :: J.Value)
           ]
jsonBinder (VarBinder _ ident) =
  J.object [ "tag" J..= ("var" :: J.Value)
           , "ident" J..= jsonIdent ident
           ]

jsonQualified :: Qualified Ident -> J.Value
jsonQualified (Qualified mn ident) =
  J.object [ "module-name" J..= (jsonModuleName <$> mn)
           , "ident" J..= jsonIdent ident
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

runPSC :: ReaderT (Options 'Compile) (WriterT MultipleErrors (Either MultipleErrors)) a -> Either MultipleErrors a
runPSC = evalWriterT . flip runReaderT defaultCompileOptions

main :: IO ()
main = do
  files <- getArgs
  contents <- traverse readFile files
  either print dumpModules . runPSC . compileModules $ zip files contents
