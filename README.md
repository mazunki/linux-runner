# linux-runner

Inspiried by [includeos/vmrunner](https://github.com/includeos/vmrunner), this application lets you boot ELF files on a linux kernel through QEMU with no boilerplate:

```sh
nix run linux-runner#boot -- ./main.c
```

The source file is compiled down to an executable static binary that is placed into a minimal busybox ramdisk. You can inspect the ramdisk through the `#mkInitrd` application:
```sh
nix run linux-runner#mkInitrd -- ./main.c | cpio -itv 2>&-`
nix run linux-runner#mkInitrd -- ./main.c | cpio -i --to-stdout sbin/init 2>&-
```

Thanks to nix flakes, static builds (see `toolchain.nix`) and running under QEMU, this should be entirely reproducible.

Additionally, the boot script supports some flags:
```
nix run linux-runner#boot -- ./main.c --kvm                          # enables kvm (off by default)
nix run linux-runner#boot -- ./main.c --mem 2G                       # sets the memory of the machine (defaults to 4G)
nix run linux-runner#boot -- ./main.c --kernel /boot/vmlinuz-6.15.6  # specifies the kernel to use for booting

# mkInitrd flags also work for the #boot application
nix run linux-runner#mkInitrd -- ./main.c --cc /path/to/compiler             # specifies the compiler to use
nix run linux-runner#mkInitrd -- ./main.c --cflags '-flags -for -compiler'   # specifies the cflags to append to compiler
```

