{
  description = "run your application over qemu with linux";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/25.05";
    compiler.url = "github:mazunki/compiler";
  };

  outputs = { self, nixpkgs, compiler }:
  let
    system = "x86_64-linux";
    pkgs = import nixpkgs { inherit system; };

    runner = pkgs.callPackage ./default.nix { inherit pkgs; };

    mkRunner = args: pkgs.callPackage ./default.nix (args // { inherit pkgs; });

    shell = pkgs.symlinkJoin {
      name = "linux-runner-shell";
      paths = [
        runner
        compiler.packages.${system}.shell
        pkgs.qemu
      ];
    };

    boot = args: {
      type = "app";
      program = "${mkRunner args}/bin/boot";
    };

    mkBoot = args:
      let
        tcg   = boot { kvm = false; };
        kvm   = boot { kvm = true; };
        debug = boot { debug = true; };
      in
        kvm // { inherit kvm debug tcg; };

  in
  {
    packages.${system} = { default = shell; inherit runner shell; };

    apps.${system} = {
      default  = { type = "app"; program = "${runner}/bin/boot"; };
      boot     = { type = "app"; program = "${runner}/bin/boot"; };
      mkInitrd = { type = "app"; program = "${runner}/bin/mkInitrd"; };
    };

    devShells.${system}.default = pkgs.mkShell {
      packages = [ shell ];
    };

    # for downstream flakes
    lib.${system} = {
      inherit mkRunner mkBoot;
      mkInitrd = args: {
        type = "app";
        program = "${mkRunner args}/bin/mkInitrd";
      };
    };
  };
}
