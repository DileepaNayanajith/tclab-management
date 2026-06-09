import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT,
    media_code TEXT,
    basal_media TEXT,
    hormones TEXT,
    ph TEXT,
    agar TEXT,
    date_prepared TEXT,
    technician TEXT,
    status TEXT,
    cycle INTEGER,
    mother_barcode TEXT,
    plant_code TEXT,
    variety TEXT
)
""")

conn.commit()
conn.close()

print("bottles table created successfully!")