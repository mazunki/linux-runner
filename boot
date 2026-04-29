#!/bin/sh
BINARY=""
SOURCES=""
KVM="@KVM@"
MEM="@MEM@"
KERNEL="@KERNEL@"
CC=""
CFLAGS=""
CPU="@CPU@"
DEBUG="@DEBUG@"
VERBOSE="@VERBOSE@"

while [ $# -gt 0 ]; do
    case "$1" in
        --kvm)      KVM="-enable-kvm" ;;
        --cpu)      CPU="$2"; shift ;;
        --mem)      MEM="$2"; shift ;;
        --kernel)   KERNEL="$2"; shift ;;
        --compiler) CC="$2"; shift ;;
        --cflags)   CFLAGS="$2"; shift ;;
        --debug)    DEBUG="true"; ;;
        --verbose)  VERBOSE="true"; ;;
        -*)         echo "boot: unknown flag: $1" >&2; exit 1 ;;
        *.c)        SOURCES="$SOURCES $1" ;;
        *)          BINARY="$1" ;;
    esac
    shift
done

[ -z "$SOURCES" ] && [ -z "$BINARY" ] && SOURCES="main.c"

if [ -n "$SOURCES" ]; then
    BINARY=$(@COMPILE@ ${CC:+--compiler "$CC"} ${CFLAGS:+--cflags "$CFLAGS"} $SOURCES)
elif [ -z "$BINARY" ]; then
    echo "usage: boot [--kvm] [--mem SIZE] [--kernel PATH] [--compiler CC] [--cflags FLAGS] [--debug] BINARY|FILE.c..." >&2
    exit 1
fi

INITRD=$(mktemp --suffix=.cpio)
trap 'rm -f "$INITRD"' EXIT

@MKINITRD@ "$BINARY" > "$INITRD"

if [ "${VERBOSE}" = true ]; then
    echo "elf: $BINARY"
    echo "cpio: $INITRD"
fi

if [ -n "${BINARY}" ]; then
    echo "couldn't find binary" >&2
elif [ -n "${INITRD}" ]; then
    echo "couldn't find cpio" >&2
fi

exec @QEMU@ \
    -kernel "$KERNEL" \
    -initrd "$INITRD" \
    -m "$MEM" \
    -display none \
    -serial stdio \
    -monitor none \
    -no-reboot \
    $KVM ${CPU:+-cpu $CPU} ${DEBUG:+-s -S} \
    -append 'console=ttyS0 rdinit=/sbin/init root=/dev/ram0 rw quiet'

