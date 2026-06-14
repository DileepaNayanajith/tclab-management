import sqlite3

conn = sqlite3.connect("tclab.db")  # make sure filename is correct
cur = conn.cursor()

cur.execute("DELETE FROM users")

conn.commit()
conn.close()

print("Users table cleared successfully!")