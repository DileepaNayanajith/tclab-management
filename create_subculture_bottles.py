import sqlite3

# Connect to your database (change path if needed)
conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

# Create the subculture_bottles table
cur.execute("""
CREATE TABLE IF NOT EXISTS subculture_bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT UNIQUE NOT NULL,
    plant_code TEXT NOT NULL,
    plant_name TEXT NOT NULL,
    media_code TEXT NOT NULL,
    num_plants INTEGER NOT NULL,
    cycle INTEGER NOT NULL,
    technician TEXT,
    parent_id INTEGER,  -- References the parent bottle if this is a subculture
    status TEXT DEFAULT 'Active',
    origin TEXT DEFAULT 'New', -- 'New' for initial mother bottle, 'Subculture' for derived bottles
    date_created DATE DEFAULT (DATE('now')),
    FOREIGN KEY(parent_id) REFERENCES subculture_bottles(id)
)
""")

conn.commit()
conn.close()
print("✅ Table 'subculture_bottles' created successfully!")
