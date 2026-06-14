import sqlite3

DB_FILE = "tclab.db"  # adjust path if needed

def get_db_connection():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def check_media_tables():
    conn = get_db_connection()
    cur = conn.cursor()

    print("=== Media Compositions ===")
    cur.execute("SELECT * FROM media ORDER BY media_code")
    rows = cur.fetchall()
    if rows:
        for row in rows:
            print(dict(row))
    else:
        print("No data found in media table.")

    print("\n=== Media Hormones ===")
    cur.execute("SELECT * FROM media_hormones ORDER BY media_code")
    rows = cur.fetchall()
    if rows:
        for row in rows:
            print(dict(row))
    else:
        print("No data found in media_hormones table.")

    conn.close()

if __name__ == "__main__":
    check_media_tables()
