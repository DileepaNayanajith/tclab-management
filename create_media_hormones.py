import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS media_hormones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    media_code TEXT,
    hormone_name TEXT,
    hormone_mg REAL
)
""")

conn.commit()
conn.close()

print("media_hormones table created successfully!")