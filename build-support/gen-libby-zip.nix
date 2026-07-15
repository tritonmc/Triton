{
  pkgs ? import <nixpkgs> { },
  libsJson ? ../core/build/generated/libby-libs.json,
  tritonVersion ? null,
}:
let
  inherit (pkgs) lib;
  libs = lib.importJSON libsJson;
  repositories = [
    "https://repo.diogotc.com/mirror"
    "https://repo.diogotc.com/releases"
  ];
  suffix = lib.optionalString (tritonVersion != null) "-${tritonVersion}";

  mkJar =
    {
      groupId,
      artifactId,
      version,
      sha256Checksum,
    }:
    let
      groupId' = lib.replaceStrings [ "{}" ] [ "/" ] groupId;
      jarPath = "${groupId'}/${artifactId}/${version}/${artifactId}-${version}.jar";
    in
    pkgs.stdenvNoCC.mkDerivation (finalAttrs: {
      pname = artifactId;
      inherit version;
      src = pkgs.fetchurl {
        urls = lib.map (repository: "${repository}/${jarPath}") repositories;
        hash = lib.optionalString (sha256Checksum != "") "sha256-${sha256Checksum}";
      };

      dontUnpack = true;

      installPhase = ''
        install -D -m 444 ${finalAttrs.src} $out/lib/${lib.escapeShellArg jarPath}
      '';
    });

  allLibs = pkgs.symlinkJoin {
    name = "triton-libs${suffix}";
    paths = map mkJar libs;
  };

  readme = pkgs.writeText "triton-libs-readme${suffix}.txt" ''
    This zip contains a copy of all libraries needed for Triton ${lib.defaultTo "<unknown>" tritonVersion}.
    In case your server does not have an internet connection,
    or the library distribution server is offline,
    you may use this archive as a replacement.

    To do so, copy the `lib` directory to the `plugins/Triton` directory (or `plugins/triton` on Velocity).
  '';

  libsZip = pkgs.runCommand "triton-libs-dist${suffix}" { } ''
    dir="triton-libs${suffix}"
    mkdir $dir
    cp -rL ${allLibs}/lib $dir/lib
    cp ${readme} $dir/README.txt
    chmod -R u+rwX,go+rX $dir
    mkdir -p $out
    ${lib.getExe pkgs.zip} -r $out/"triton-libs${suffix}.zip" $dir
  '';

in
{
  inherit allLibs libsZip;
}
