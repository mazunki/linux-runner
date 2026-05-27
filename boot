#!/usr/bin/env python3
import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

QEMU     = "@QEMU@"
KERNEL   = "@KERNEL@"
MKINITRD = "@MKINITRD@"
MEM      = "@MEM@"
KVM      = "@KVM@"    # "-enable-kvm" or ""
CPU      = "@CPU@"    # "kvm64,+rdrand,+rdseed" or ""
DEBUG    = "@DEBUG@"  # "true" or ""


def boot(binary: Path, mem: str, kvm: bool, cpu: str, kernel: str, debug: bool, verbose: bool):
    fd, initrd = tempfile.mkstemp(suffix=".cpio")
    try:
        with os.fdopen(fd, "wb") as f:
            subprocess.run([MKINITRD, str(binary)], stdout=f, check=True)

        if verbose:
            print(f"elf: {binary}", file=sys.stderr)
            print(f"cpio: {initrd}", file=sys.stderr)

        cmd = [
            QEMU,
            "-kernel", kernel,
            "-initrd", initrd,
            "-m", mem,
            "-display", "none",
            "-serial", "stdio",
            "-monitor", "none",
            "-no-reboot",
        ]

        if kvm:
            cmd.append("-enable-kvm")
        if cpu:
            cmd += ["-cpu", cpu]
        if debug:
            cmd += ["-s", "-S"]

        cmd += ["-append", "console=ttyS0 rdinit=/sbin/init root=/dev/ram0 rw quiet"]

        result = subprocess.run(cmd)
    finally:
        os.unlink(initrd)

    sys.exit(result.returncode)


def main():
    parser = argparse.ArgumentParser(prog="boot")
    parser.add_argument("binary", type=Path)
    parser.add_argument("--mem", default=MEM)
    parser.add_argument("--kernel", default=KERNEL)
    parser.add_argument("--kvm", dest="kvm", action="store_true", default=bool(KVM))
    parser.add_argument("--no-kvm", dest="kvm", action="store_false")
    parser.add_argument("--cpu", default=CPU or None)
    parser.add_argument("--debug", action="store_true", default=(DEBUG == "true"))
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    boot(
        binary=args.binary,
        mem=args.mem,
        kvm=args.kvm,
        cpu=args.cpu or "",
        kernel=args.kernel,
        debug=args.debug,
        verbose=args.verbose,
    )


if __name__ == "__main__":
    main()
