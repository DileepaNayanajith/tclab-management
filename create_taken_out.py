import sqlite3

# Connect to your database
conn = sqlite3.connect("tclab.db")
conn.execute("PRAGMA foreign_keys = ON;")  # enable foreign key checks
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS taken_out (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subculture_id INTEGER NOT NULL,
    plant_code TEXT NOT NULL,
    plant_name TEXT NOT NULL,
    num_plants INTEGER NOT NULL,
    type TEXT NOT NULL,
    date_taken DATE NOT NULL,
    notes TEXT,
    FOREIGN KEY(subculture_id) REFERENCES subculture_bottles(id)
)
""")

conn.commit()
conn.close()
print("✅ Table 'taken_out' created successfully!")
