import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS plants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT,
    name TEXT,
    variety TEXT,
    description TEXT
)
""")

conn.commit()
conn.close()

print("plants table created successfully!")