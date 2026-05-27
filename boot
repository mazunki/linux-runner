#!/usr/bin/env python3
import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

from command import Command, FlagArg

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
            print(f"elf:  {binary}", file=sys.stderr)
            print(f"cpio: {initrd}", file=sys.stderr)

        cmd = Command([QEMU])
        cmd += FlagArg("-kernel",  kernel)
        cmd += FlagArg("-initrd",  initrd)
        cmd += FlagArg("-m",       mem)
        cmd += FlagArg("-display", "none")
        cmd += FlagArg("-serial",  "stdio")
        cmd += FlagArg("-monitor", "none")
        cmd += "-no-reboot"

        if kvm:
            cmd += "-enable-kvm"
        if cpu:
            cmd += FlagArg("-cpu", cpu)
        if debug:
            cmd += ["-s", "-S"]

        cmd += FlagArg("-append", "console=ttyS0 rdinit=/sbin/init root=/dev/ram0 rw quiet")

        cmd.run(verbose="multiline" if verbose else None)
    finally:
        os.unlink(initrd)

    sys.exit(0)


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

    if not args.binary.is_file():
        print(f"error: not a file: {args.binary}", file=sys.stderr)
        sys.exit(1)

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
