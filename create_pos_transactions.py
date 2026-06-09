import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS pos_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    plant_name TEXT,
    quantity REAL,
    unit_price REAL,
    total_price REAL,
    payment_method TEXT,
    printed INTEGER,
    created_at TIMESTAMP
)
""")

conn.commit()
conn.close()

print("pos_transactions table created successfully!")