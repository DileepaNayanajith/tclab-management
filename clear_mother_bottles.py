import sqlite3

DB_FILE = "tclab.db"   # change if your DB file name is different

def clear_mother_bottles():
    conn = sqlite3.connect(DB_FILE)
    cur = conn.cursor()

    # Delete all rows
    cur.execute("DELETE FROM mother_bottles")
    conn.commit()

    print("✅ mother_bottles table cleared!")

    # Check if really empty
    cur.execute("SELECT COUNT(*) FROM mother_bottles")
    count = cur.fetchone()[0]
    print(f"Rows remaining: {count}")

    conn.close()

if __name__ == "__main__":
    clear_mother_bottles()
