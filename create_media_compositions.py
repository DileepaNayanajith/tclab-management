import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS media_compositions (
    media_code TEXT PRIMARY KEY,
    basal_media TEXT
)
""")

conn.commit()
conn.close()

print("media_compositions table created successfully!")