import sqlite3

# Connect to your database
conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

def check_media_compositions():
    conn = sqlite3.connect("tclab.db")
    cur = conn.cursor()

    print("\n📋 Columns in table 'media_compositions':")
    cur.execute("PRAGMA table_info(media_compositions)")
    for col in cur.fetchall():
        print(" ", col)

    print("\n📊 Rows in table 'media_compositions':")
    cur.execute("SELECT * FROM media_compositions")
    rows = cur.fetchall()

    if not rows:
        print(" (empty)")
    else:
        for row in rows:
            print(" ", row)

    conn.close()

if __name__ == "__main__":
    check_media_compositions()