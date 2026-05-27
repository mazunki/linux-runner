import dataclasses
import re
import subprocess
import sys
from collections.abc import Iterable
from pathlib import Path
from typing import Optional

_FLAG_RE = re.compile(r'^(-{1,2})([^=]+)(=?)(.*)$')


def _color_path(p: Path) -> str:
    if p.exists():
        return f"\033[36m{p}\033[0m"
    return f"\033[48;5;125;38;5;255m{p}\033[0m"


def _as_path(value: str | Path) -> str | Path:
    if isinstance(value, Path):
        return value
    if value.startswith("/"):
        return Path(value)
    return value


class KwArg:
    __slots__ = ("value",)

    def __init__(self, value: "str | Path | KwArg") -> None:
        if isinstance(value, KwArg):
            self.value = value.value
        else:
            self.value = _as_path(value)

    def __str__(self) -> str:
        return str(self.value)

    def __repr__(self) -> str:
        return f"KwArg({self.value!r})"

    def expand(self) -> list[str]:
        return [str(self.value)]

    def __format__(self, spec: str) -> str:
        reset = "\033[0m"
        match spec:
            case "binary":
                if isinstance(self.value, Path):
                    return f"\033[1m{_color_path(self.value)}"
                return f"\033[1m{self.value}{reset}"
            case "color":
                if isinstance(self.value, Path):
                    return _color_path(self.value)
                return str(self.value)
            case _:
                return str(self.value)


class FlagArg:
    __slots__ = ("prefix", "key", "sep", "value")

    def __init__(self, raw: "str | FlagArg", value: Optional["str | Path"] = None) -> None:
        if isinstance(raw, FlagArg):
            self.prefix = raw.prefix
            self.key    = raw.key
            self.sep    = raw.sep
            self.value  = raw.value
            return
        if m := _FLAG_RE.match(raw):
            self.prefix = m.group(1)
            self.key    = m.group(2)
            if value is not None:
                self.sep   = " "
                self.value = _as_path(value)
            else:
                self.sep   = m.group(3)
                self.value = _as_path(m.group(4)) if m.group(3) else m.group(4)
                if not self.sep and "/" in self.key:
                    slash = self.key.index("/")
                    self.value = Path(self.key[slash:])
                    self.key   = self.key[:slash]
        else:
            raise ValueError(f"not a flag: {raw!r}")

    def __str__(self) -> str:
        return self.prefix + self.key + self.sep + str(self.value)

    def __repr__(self) -> str:
        return f"FlagArg({str(self)!r})"

    def expand(self) -> list[str]:
        if self.sep == " ":
            return [self.prefix + self.key, str(self.value)]
        return [str(self)]

    def __format__(self, spec: str) -> str:
        yellow = "\033[33m"
        reset  = "\033[0m"
        match spec:
            case "color":
                flag = f"{yellow}{self.prefix}{self.key}{reset}"
                if self.value:
                    val = str(self.value)
                    rhs = _color_path(self.value) if isinstance(self.value, Path) else val
                    return f"{flag}{self.sep}{rhs}"
                return flag
            case _:
                return str(self)


type CmdArg = KwArg | FlagArg
type RawArg = str | Path
type Args   = list[RawArg]


def _classify(value: "RawArg | CmdArg") -> CmdArg:
    if isinstance(value, (KwArg, FlagArg)):
        return value
    if not isinstance(value, Path) and _FLAG_RE.match(str(value)):
        return FlagArg(str(value))
    return KwArg(value)


@dataclasses.dataclass
class Command:
    args: list[CmdArg] = dataclasses.field(default_factory=list)

    def __post_init__(self) -> None:
        self.args = [_classify(a) for a in self.args]

    def __iadd__(self, other: "RawArg | CmdArg | Iterable") -> "Command":
        if isinstance(other, (str, Path, KwArg, FlagArg)):
            self.args.append(_classify(other))
        else:
            self.args.extend(_classify(a) for a in other)
        return self

    def _render(self, oneline: bool = False) -> str:
        if not self.args:
            return ""
        parts = [f"{self.args[0]:binary}"]
        parts += [f"{a:color}" for a in self.args[1:]]
        sep = " " if oneline else "  \033[2m\\\n  \033[0m"
        return sep.join(parts)

    def __str__(self) -> str:
        return self._render()

    def __format__(self, spec: str) -> str:
        match spec:
            case "oneline":
                return self._render(oneline=True)
            case _:
                return self._render()

    def run(self, verbose: Optional[str] = None) -> None:
        if verbose in ("oneline", "1"):
            print(f"{self:oneline}", file=sys.stderr)
        elif verbose:
            print(self, file=sys.stderr)

        try:
            result = subprocess.run([s for a in self.args for s in a.expand()])
        except FileNotFoundError:
            print(f"error: executable not found: {self.args[0]}", file=sys.stderr)
            sys.exit(127)
        if result.returncode != 0:
            sys.exit(result.returncode)
