"""
Copy existing Natural Foliage Lab data from SQLite (tclab.db) to MySQL.

Safety:
- The SQLite database is read-only.
- Migration stops if any target MySQL table already contains rows.
- All inserts run inside one transaction. An error rolls everything back.
"""

from __future__ import annotations

import os
import sqlite3
import sys
from pathlib import Path
from typing import Any

import pymysql
from dotenv import load_dotenv


BASE_DIR = Path(__file__).resolve().parent
SQLITE_PATH = BASE_DIR / "tclab.db"

TABLES = [
    "users",
    "plants",
    "media",
    "media_compositions",
    "media_hormones",
    "bottles",
    "mother_bottles",
    "subculture_bottles",
    "discards",
    "taken_out",
    "price_list",
    "sales",
    "pos_transactions",
    "pos_invoices",
    "pos_invoice_items",
]


def required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Missing {name} in .env")
    return value


def sqlite_table_exists(conn: sqlite3.Connection, table: str) -> bool:
    row = conn.execute(
        "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
        (table,),
    ).fetchone()
    return row is not None


def sqlite_columns(conn: sqlite3.Connection, table: str) -> list[str]:
    return [row[1] for row in conn.execute(f"PRAGMA table_info(`{table}`)")]


def mysql_columns(cursor: pymysql.cursors.Cursor, table: str) -> list[str]:
    cursor.execute(f"SHOW COLUMNS FROM `{table}`")
    return [row[0] for row in cursor.fetchall()]


def normalise_value(value: Any) -> Any:
    # PyMySQL accepts Python None, numbers, strings, dates and datetimes.
    # SQLite date/timestamp values in this project are mainly ISO strings,
    # which MySQL can insert directly.
    return value


def main() -> int:
    load_dotenv(BASE_DIR / ".env")

    if not SQLITE_PATH.exists():
        print(f"ERROR: SQLite file not found: {SQLITE_PATH}")
        return 1

    mysql_config = {
        "host": required_env("MYSQL_HOST"),
        "port": int(os.getenv("MYSQL_PORT", "3306")),
        "user": required_env("MYSQL_USER"),
        "password": required_env("MYSQL_PASSWORD"),
        "database": required_env("MYSQL_DATABASE"),
        "charset": "utf8mb4",
        "autocommit": False,
    }

    sqlite_conn = sqlite3.connect(f"file:{SQLITE_PATH}?mode=ro", uri=True)
    sqlite_conn.row_factory = sqlite3.Row
    mysql_conn = pymysql.connect(**mysql_config)

    try:
        mysql_cur = mysql_conn.cursor()

        print("Checking MySQL target tables...")
        for table in TABLES:
            mysql_cur.execute(f"SELECT COUNT(*) FROM `{table}`")
            count = int(mysql_cur.fetchone()[0])
            if count != 0:
                print(
                    f"STOPPED: MySQL table '{table}' already has {count} row(s). "
                    "No data was changed."
                )
                return 2

        print(f"Reading SQLite database: {SQLITE_PATH.name}")
        mysql_cur.execute("SET FOREIGN_KEY_CHECKS = 0")

        copied_counts: dict[str, int] = {}

        for table in TABLES:
            if not sqlite_table_exists(sqlite_conn, table):
                print(f"SKIP  {table}: not present in SQLite")
                copied_counts[table] = 0
                continue

            source_columns = sqlite_columns(sqlite_conn, table)
            target_columns = mysql_columns(mysql_cur, table)
            columns = [col for col in source_columns if col in target_columns]

            if not columns:
                print(f"SKIP  {table}: no matching columns")
                copied_counts[table] = 0
                continue

            column_sql = ", ".join(f"`{col}`" for col in columns)
            sqlite_rows = sqlite_conn.execute(
                f"SELECT {column_sql} FROM `{table}`"
            ).fetchall()

            if sqlite_rows:
                placeholders = ", ".join(["%s"] * len(columns))
                insert_sql = (
                    f"INSERT INTO `{table}` ({column_sql}) "
                    f"VALUES ({placeholders})"
                )
                values = [
                    tuple(normalise_value(row[col]) for col in columns)
                    for row in sqlite_rows
                ]
                mysql_cur.executemany(insert_sql, values)

            copied_counts[table] = len(sqlite_rows)
            print(f"COPIED {table}: {len(sqlite_rows)} row(s)")

        mysql_cur.execute("SET FOREIGN_KEY_CHECKS = 1")
        mysql_conn.commit()

        print("\nMigration completed successfully.")
        print("Summary:")
        for table, count in copied_counts.items():
            print(f"  {table}: {count}")

        return 0

    except Exception as exc:
        mysql_conn.rollback()
        print("\nMIGRATION FAILED. MySQL changes were rolled back.")
        print(f"{type(exc).__name__}: {exc}")
        return 3

    finally:
        sqlite_conn.close()
        mysql_conn.close()


if __name__ == "__main__":
    sys.exit(main())
