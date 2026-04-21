{ pkgs
, stdenv
, kernel ? pkgs.linux
, mem ? "4G"
, kvm ? false
}:

let
  busybox = pkgs.busybox.override { enableStatic = true; };
in

stdenv.mkDerivation {
  pname = "linux-runner";
  version = "dev";
  src = ./.;

  nativeBuildInputs = [ pkgs.makeWrapper ];

  dontConfigure = true;
  dontBuild = true;

  installPhase = ''
    install -D compile $out/bin/compile
    substituteInPlace $out/bin/compile \
      --subst-var-by COMPILER ${stdenv.cc}/bin/cc \
      --subst-var-by CFLAGS   "-O2 -static -rtlib=compiler-rt"

    install -D mkInitrd $out/bin/mkInitrd

    substituteInPlace $out/bin/mkInitrd \
      --subst-var-by BUSYBOX ${busybox}/bin/busybox \
      --subst-var-by CPIO    ${pkgs.cpio}/bin/cpio \

    install -D boot $out/bin/boot

    substituteInPlace $out/bin/boot \
      --subst-var-by MKINITRD $out/bin/mkInitrd \
      --subst-var-by QEMU     ${pkgs.qemu}/bin/qemu-system-x86_64 \
      --subst-var-by KERNEL   ${kernel}/bzImage \
      --subst-var-by MEM      ${mem} \
      --subst-var-by KVM      "${if kvm then "-enable-kvm" else ""}" \
      --subst-var-by CPU      "${if kvm then "kvm64,+rdrand,+rdseed" else ""}" \
      --subst-var-by COMPILE  $out/bin/compile
  '';
}
