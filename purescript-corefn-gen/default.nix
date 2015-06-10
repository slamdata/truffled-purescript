with import <nixpkgs> { };

let purescript =
  haskell.lib.overrideCabal haskell.packages.ghc7101.purescript (drv: rec {
    version = "0.7.0-rc.1";
    src = fetchFromGitHub {
      owner = "purescript";
      repo = "purescript";
      rev = "v${version}";
      sha256 = "1x0nkxp01q1w4szhzc046qn774ka0szanjj19gmwzd1bw60hzi5f";
    };
    buildDepends = with haskell.packages.ghc7101; [
      Glob
      HUnit
      aeson
      aeson-better-errors
      bower-json
      boxes
      optparse-applicative
      pattern-arrows
      safe
      semigroups
      split
      utf8-string
    ];
  });
in

runCommand "dummy" {
  buildInputs = [
    (haskell.packages.ghc7101.ghcWithPackages (pkgs: [
      pkgs.aeson
      purescript
    ]))
  ];
} ""
