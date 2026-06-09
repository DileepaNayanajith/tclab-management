import sqlite3
from werkzeug.security import generate_password_hash

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

username = "admin"
password = generate_password_hash("admin+123")  # 🔐 hashed password

cur.execute("""
INSERT INTO users 
(full_name, username, password, role,
 can_register_media, can_subculture, can_discard,
 can_register_mother_plants, can_price_list,
 can_pos_rooted, can_dashboard)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", (
    "System Admin",
    username,
    password,
    "admin",
    1, 1, 1, 1, 1, 1, 1
))

conn.commit()
conn.close()

print("Admin user created successfully!")