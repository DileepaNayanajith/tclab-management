import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

# Create the users table
cur.execute("""
 CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        full_name TEXT,
        role TEXT NOT NULL
)
""")

conn.commit()
conn.close()
print("users table created successfully!")
