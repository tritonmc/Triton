{
  pkgs ? import <nixpkgs> { },
}:
let
  gen-libby-zip = pkgs.writeShellScriptBin "gen-libby-zip" ''
    ./gradlew genLibbyLibsJson
    nix-build ./build-support/gen-libby-zip.nix -A libsZip
  '';
in
pkgs.mkShell {
  packages = with pkgs; [
    jdk25
    gen-libby-zip
  ];
}
