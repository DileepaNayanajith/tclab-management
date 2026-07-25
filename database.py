from __future__ import annotations

import os
from collections.abc import Iterable
from typing import Any

import pymysql
from dotenv import load_dotenv

load_dotenv()


class HybridRow(dict):
    """Dictionary row that also supports SQLite-style numeric indexes."""

    def __getitem__(self, key: Any) -> Any:
        if isinstance(key, int):
            return list(self.values())[key]
        return super().__getitem__(key)

    def keys(self):
        return super().keys()


class CursorAdapter:
    """Adapts PyMySQL cursor behaviour to the old SQLite-based application."""

    def __init__(self, cursor: pymysql.cursors.DictCursor):
        self._cursor = cursor

    @staticmethod
    def _translate_sql(sql: str) -> str:
        # The old application uses SQLite '?' parameters. PyMySQL uses '%s'.
        return sql.replace("?", "%s")

    @staticmethod
    def _row(row: dict[str, Any] | None) -> HybridRow | None:
        return HybridRow(row) if row is not None else None

    def execute(self, sql: str, params: Iterable[Any] | None = None):
        self._cursor.execute(self._translate_sql(sql), tuple(params or ()))
        return self

    def executemany(self, sql: str, params: Iterable[Iterable[Any]]):
        self._cursor.executemany(self._translate_sql(sql), params)
        return self

    def fetchone(self) -> HybridRow | None:
        return self._row(self._cursor.fetchone())

    def fetchall(self) -> list[HybridRow]:
        return [HybridRow(row) for row in self._cursor.fetchall()]

    @property
    def lastrowid(self) -> int | None:
        return self._cursor.lastrowid

    @property
    def rowcount(self) -> int:
        return self._cursor.rowcount

    def close(self) -> None:
        self._cursor.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()


class ConnectionAdapter:
    """Small compatibility layer used while migrating the existing Flask app."""

    def __init__(self, connection: pymysql.Connection):
        self._connection = connection
        self._row_factory = None

    @property
    def row_factory(self):
        return self._row_factory

    @row_factory.setter
    def row_factory(self, value):
        # Kept only so old `conn.row_factory = sqlite3.Row` lines do not fail.
        self._row_factory = value

    def cursor(self) -> CursorAdapter:
        return CursorAdapter(self._connection.cursor())

    def execute(self, sql: str, params: Iterable[Any] | None = None) -> CursorAdapter:
        cursor = self.cursor()
        return cursor.execute(sql, params)

    def commit(self) -> None:
        self._connection.commit()

    def rollback(self) -> None:
        self._connection.rollback()

    def close(self) -> None:
        self._connection.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        if exc_type is None:
            self.commit()
        else:
            self.rollback()
        self.close()


def get_mysql_connection() -> pymysql.Connection:
    required = ["MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DATABASE"]
    missing = [name for name in required if not os.getenv(name)]
    if missing:
        raise RuntimeError(f"Missing environment variables: {', '.join(missing)}")

    return pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USER"),
        password=os.getenv("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DATABASE"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def get_db_connection() -> ConnectionAdapter:
    return ConnectionAdapter(get_mysql_connection())


def get_db() -> ConnectionAdapter:
    return get_db_connection()


def test_mysql_connection() -> None:
    connection = None
    try:
        connection = get_db_connection()
        result = connection.execute("SELECT DATABASE() AS database_name").fetchone()
        print("MySQL connection successful")
        print("Connected database:", result["database_name"])
    except Exception as error:
        print("MySQL connection failed")
        print("Error:", error)
        raise
    finally:
        if connection:
            connection.close()


if __name__ == "__main__":
    test_mysql_connection()