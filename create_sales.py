import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    variety TEXT,
    subculture_week TEXT,
    qty_sold INTEGER,
    sale_date TEXT
)
""")

conn.commit()
conn.close()

print("sales table created successfully!")