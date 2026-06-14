import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS pos_invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_method TEXT,
    total_amount REAL,
    printed INTEGER,
    created_at TIMESTAMP,
    customer_name TEXT
)
""")

conn.commit()
conn.close()

print("pos_invoices table created successfully!")