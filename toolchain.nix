{ }:
final: prev:
let
  llvmPkgs = prev.pkgsStatic.llvmPackages_20;
in
{
  toolchain =
    prev.pkgsStatic.lib.makeScope prev.pkgsStatic.newScope (self: {
      # libc
      musl_static = self.callPackage ./musl-static.nix {
        linuxHeaders = prev.linuxHeaders;
      };

      clang_musl = llvmPkgs.clangNoLibcxx.override (old: {
        bintools = prev.pkgsStatic.bintools.override {
          defaultHardeningFlags = [];
          libc = self.musl_static;
        };
        libc = self.musl_static;
      });


      # libcxx
      libcxx_musl = llvmPkgs.libcxx.override (old: {
        stdenv = (prev.overrideCC llvmPkgs.libcxxStdenv self.clang_musl);
      });

      clang_musl_libcxx = llvmPkgs.libcxxClang.override (old: {
        bintools = prev.pkgsStatic.bintools.override {
          defaultHardeningFlags = [];
          libc = self.musl_static;
        };
        libc = self.musl_static;
        libcxx = self.libcxx_musl;
      });
      musl_stdenv_libcxx = (prev.overrideCC llvmPkgs.libcxxStdenv self.clang_musl_libcxx);


      # toolchain
      stdenv' = self.musl_stdenv_libcxx;

      libraries = {
        libc = self.musl-static;

        libcxx = {
          lib = "${self.libcxx}/lib";
          include = "${self.libcxx.dev}/include/c++/v1";
        };

        libunwind = llvmPkgs.libraries.libunwind;
        libgcc = llvmPkgs.compiler-rt;
      };
    }
  );

  pkgsToolchain = prev.pkgsStatic.lib.makeScope prev.pkgsStatic.newScope (self: {
    stdenv = final.toolchain.stdenv';

    passthru = {
      pkgsStatic = prev.pkgsStatic;
      pkgs = prev.pkgs;

      toolchainLibs = final.toolchain.libraries;
    };
  });
}
