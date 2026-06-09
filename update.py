import sqlite3

conn = sqlite3.connect("tclab.db")
cur = conn.cursor()

cur.execute ("ALTER TABLE users ADD COLUMN can_register_media INTEGER DEFAULT 0;")
cur.execute ("ALTER TABLE users ADD COLUMN can_register_mother_plants INTEGER DEFAULT 0;")
cur.execute ("ALTER TABLE users ADD COLUMN can_price_list INTEGER DEFAULT 0;")
cur.execute ("ALTER TABLE users ADD COLUMN can_pos_rooted INTEGER DEFAULT 0;")

conn.commit()
conn.close()
print("Users table upgraded successfully")