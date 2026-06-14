import sqlite3

DB_FILE = "tclab.db"   # change if your DB file name is different

def clear_media_compositions():
    conn = sqlite3.connect(DB_FILE)
    cur = conn.cursor()

    # Delete all rows
    cur.execute("DELETE FROM media_compositions")
    conn.commit()

    print("✅ media_compositions table cleared!")

    # Check if really empty
    cur.execute("SELECT COUNT(*) FROM media_compositions")
    count = cur.fetchone()[0]
    print(f"Rows remaining: {count}")

    conn.close()

if __name__ == "__main__":
    clear_media_compositions()
