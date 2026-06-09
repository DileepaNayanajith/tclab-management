import sqlite3

DB_FILE = "tclab.db"   # change if your DB file name is different

def clear_bottle_table():
    conn = sqlite3.connect(DB_FILE)
    cur = conn.cursor()

    # Delete all rows
    cur.execute("DELETE FROM bottles")
    conn.commit()

    print("✅ bottle table cleared!")

    # Check if really empty
    cur.execute("SELECT COUNT(*) FROM bottles")
    count = cur.fetchone()[0]
    print(f"Rows remaining: {count}")

    conn.close()

if __name__ == "__main__":
    clear_bottle_table()
