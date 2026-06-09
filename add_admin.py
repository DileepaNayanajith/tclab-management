import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute("""
INSERT INTO users (username, password, full_name, role)
VALUES (?, ?, ?, ?)
""", ("admin", "admin+123", "Admin", "admin"))

conn.commit()
conn.close()

print("Admin user added successfully!")