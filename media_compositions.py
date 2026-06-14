import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS media_compositions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    media_code TEXT,
    basal_media TEXT,
    hormone_name TEXT,
    hormone_mg REAL
)
""")
conn.commit()
conn.close()
