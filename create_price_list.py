import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS price_list (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    variety_name TEXT,
    price REAL,
    last_updated TIMESTAMP
)
""")

conn.commit()
conn.close()

print("price_list table created successfully!")