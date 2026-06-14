import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS media (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    media_code TEXT,
    basal_media TEXT,
    hormones TEXT,
    ph REAL,
    agar REAL,
    date_prepared TEXT,
    technician TEXT
)
""")

conn.commit()
conn.close()

print("media table created successfully!")