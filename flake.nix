{
  description = "run your application over qemu with linux";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/25.05";

  outputs = { self, nixpkgs }:
 let
    system = "x86_64-linux";

    pkgs = import nixpkgs {
      inherit system;
      overlays = [ (import ./toolchain.nix {}) ];
    };

    stdenv = pkgs.toolchain.stdenv';

    runner = pkgs.callPackage ./default.nix {
      inherit pkgs;
      inherit stdenv;
    };

    mkRunner = args:
      pkgs.callPackage ./default.nix (args // {
        inherit pkgs stdenv;
      });
  in
  {
    packages.${system}.default = runner;

    apps.${system} = {
      default  = { type = "app"; program = "${runner}/bin/boot"; };
      boot     = { type = "app"; program = "${runner}/bin/boot"; };
      mkInitrd = { type = "app"; program = "${runner}/bin/mkInitrd"; };
    };

    # for downstream flakes
    lib.${system} = {
      inherit mkRunner;
      mkBoot = args: {
        type = "app";
        program = "${mkRunner args}/bin/boot";
      };
      mkInitrd = args: {
        type = "app";
        program = "${mkRunner args}/bin/mkInitrd";
      };
    };
  };
}
