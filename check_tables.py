# check_tables.py
import sqlite3

DB_NAME = "tclab.db"   # change if your DB file has another name

def check_tables():
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()

    try:
        # List all tables
        cur.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cur.fetchall()

        if not tables:
            print("❌ No tables found in the database.")
            return

        print("📋 Tables in the database:\n")
        for (table_name,) in tables:
            print(f"=== {table_name} ===")
            cur.execute(f"PRAGMA table_info({table_name});")
            columns = cur.fetchall()
            for col in columns:
                # col = (cid, name, type, notnull, dflt_value, pk)
                print(f"  {col[1]} ({col[2]}){' [PK]' if col[5] else ''}")
            print()
    except sqlite3.Error as e:
        print("❌ SQLite error:", e)
    finally:
        conn.close()

if __name__ == "__main__":
    check_tables()
