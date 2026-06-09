import sqlite3

conn = sqlite3.connect('tclab.db')
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS mother_bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT,
    plant_code TEXT,
    plant_name TEXT,
    media_code TEXT,
    num_plants INTEGER,
    cycle INTEGER,
    technician TEXT,
    date_created TIMESTAMP,
    printed INTEGER,
    status TEXT,
    culture_week INTEGER,
    lamina_flow TEXT
)
""")

conn.commit()
conn.close()

print("Mother bottles table created successfully!")