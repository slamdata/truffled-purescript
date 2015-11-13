with import <nixpkgs> { };

runCommand "dummy" {
  buildInputs = [
    oraclejdk8
    (sbt.override { jre = oraclejre8; })
    (haskell.packages.ghc7102.ghcWithPackages (p: [
      p.aeson
      p.purescript
    ]))
  ];
} ""
