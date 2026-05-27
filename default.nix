{ pkgs
, kernel ? pkgs.linux
, mem ? "4G"
, kvm ? true
, debug ? false
}:

let
  busybox = pkgs.busybox.override { enableStatic = true; };
in

pkgs.stdenv.mkDerivation {
  pname = "linux-runner";
  version = "dev";
  src = ./.;

  dontConfigure = true;
  dontBuild = true;

  installPhase = ''
    install -D mkInitrd $out/bin/mkInitrd
    substituteInPlace $out/bin/mkInitrd \
      --subst-var-by BUSYBOX ${busybox}/bin/busybox \
      --subst-var-by CPIO    ${pkgs.cpio}/bin/cpio

    install -D boot $out/bin/boot
    substituteInPlace $out/bin/boot \
      --replace-fail "#!/usr/bin/env python3" "#!${pkgs.python3}/bin/python3" \
      --subst-var-by MKINITRD $out/bin/mkInitrd \
      --subst-var-by QEMU     ${pkgs.qemu}/bin/qemu-system-x86_64 \
      --subst-var-by KERNEL   ${kernel}/bzImage \
      --subst-var-by MEM      ${mem} \
      --subst-var-by KVM      "${if kvm then "-enable-kvm" else ""}" \
      --subst-var-by CPU      "${if kvm then "kvm64,+rdrand,+rdseed" else ""}" \
      --subst-var-by DEBUG    "${if debug then "true" else ""}"
  '';
}
