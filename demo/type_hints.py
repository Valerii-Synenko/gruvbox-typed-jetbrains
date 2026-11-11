"""Gruvbox Typed demo: stable colors for type hints."""

from dataclasses import dataclass
from typing import Optional, TypeAlias

UserId: TypeAlias = int
type Pair[T] = tuple[T, T]


@dataclass
class AuthenticatedUser:
    id: UserId
    name: str
    roles: dict[str, list[int]]
    email: Optional[str] = None


def authenticate(token: str, strict: bool | None = None) -> AuthenticatedUser:
    if strict and len(token) < 32:
        raise ValueError("token is too short")
    return AuthenticatedUser(id=1, name="admin", roles={"admin": [1, 2]})


def first[T](items: list[T], default: T | None = None) -> T | None:
    return items[0] if items else default


def parent_of(user: "AuthenticatedUser") -> Optional["AuthenticatedUser"]:
    print(f"Looking up parent of {user.name}")
    return None
