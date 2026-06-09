import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

# Create the plants table
cur.execute("""
CREATE TABLE IF NOT EXISTS plants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    variety TEXT,
    description TEXT
)
""")

conn.commit()
conn.close()
print("Plants table created successfully!")
