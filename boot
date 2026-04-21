#!/bin/sh
BINARY=""
SOURCES=""
KVM="@KVM@"
MEM="@MEM@"
KERNEL="@KERNEL@"
CC=""
CFLAGS=""
CPU="@CPU@"

while [ $# -gt 0 ]; do
    case "$1" in
        --kvm)    KVM="-enable-kvm" ;;
        --cpu)    CPU="$2"; shift ;;
        --mem)    MEM="$2"; shift ;;
        --kernel) KERNEL="$2"; shift ;;
        --cc)     CC="$2"; shift ;;
        --cflags) CFLAGS="$2"; shift ;;
        -*)       echo "boot: unknown flag: $1" >&2; exit 1 ;;
        *.c)      SOURCES="$SOURCES $1" ;;
        *)        BINARY="$1" ;;
    esac
    shift
done

[ -z "$SOURCES" ] && [ -z "$BINARY" ] && SOURCES="main.c"

if [ -n "$SOURCES" ]; then
    BINARY=$(@COMPILE@ ${CC:+--cc "$CC"} ${CFLAGS:+--cflags "$CFLAGS"} $SOURCES)
elif [ -z "$BINARY" ]; then
    echo "usage: boot [--kvm] [--mem SIZE] [--kernel PATH] [--cc CC] [--cflags FLAGS] BINARY|FILE.c..." >&2
    exit 1
fi

INITRD=$(mktemp --suffix=.cpio)
trap 'rm -f "$INITRD"' EXIT

@MKINITRD@ "$BINARY" > "$INITRD"

exec @QEMU@ \
    -kernel "$KERNEL" \
    -initrd "$INITRD" \
    -m "$MEM" \
    -display none \
    -serial stdio \
    -monitor none \
    -no-reboot \
    $KVM ${CPU:+-cpu $CPU} \
    -append 'console=ttyS0 rdinit=/sbin/init root=/dev/ram0 rw quiet'

