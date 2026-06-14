import sqlite3

# Connect to your database (change path if needed)
conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

# Create the discard table
cur.execute("""
CREATE TABLE IF NOT EXISTS discards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT NOT NULL,
    reason TEXT NOT NULL,
    num_bottles INTEGER NOT NULL,
    technician TEXT,
    date_discarded DATE NOT NULL
);

""")

conn.commit()
conn.close()
print("✅ Table 'discard table' created successfully!")
