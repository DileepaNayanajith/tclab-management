import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS pos_invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER,
    plant_code TEXT,
    plant_name TEXT,
    quantity REAL,
    unit_price REAL,
    total_price REAL
)
""")

conn.commit()
conn.close()

print("pos_invoice_items table created successfully!")