#!/bin/sh
BINARY=""
KVM=""
MEM="@MEM@"
KERNEL="@KERNEL@"
CC=""
CFLAGS=""

while [ $# -gt 0 ]; do
    case "$1" in
        --kvm)    KVM="-enable-kvm -cpu host" ;;
        --mem)    MEM="$2";    shift ;;
        --kernel) KERNEL="$2"; shift ;;
        --cc)     CC="$2";     shift ;;
        --cflags) CFLAGS="$2"; shift ;;
        -*)       echo "boot: unknown flag: $1" >&2; exit 1 ;;
        *)        BINARY="$1" ;;
    esac
    shift
done

[ -z "$BINARY" ] && { echo "usage: boot [--kvm] [--mem SIZE] [--kernel PATH] [--cc CC] [--cflags FLAGS] BINARY|FILE.c" >&2; exit 1; }

INITRD=$(mktemp --suffix=.cpio)
trap 'rm -f "$INITRD"' EXIT

@MKINITRD@ \
    ${CC:+--cc "$CC"} \
    ${CFLAGS:+--cflags "$CFLAGS"} \
    "$BINARY" > "$INITRD"

exec @QEMU@ \
    -kernel "$KERNEL" \
    -initrd "$INITRD" \
    -m "$MEM" \
    -display none \
    -serial stdio \
    -monitor none \
    -no-reboot \
    $KVM \
    -append 'console=ttyS0 rdinit=/sbin/init root=/dev/ram0 rw quiet'
