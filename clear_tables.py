import sqlite3

# Path to your database
db_path = r"F:\My Drive\naturalfoliagelab\tclab.db"  # change to your DB path

conn = sqlite3.connect(db_path)
cur = conn.cursor()

try:
    # Delete all POS data
    cur.execute("DELETE FROM pos_invoice_items")
    cur.execute("DELETE FROM pos_invoices")
    cur.execute("DELETE FROM rooted_plants")

    # Reset rooted plant quantities
    cur.execute("UPDATE subculture_bottles SET num_plants=0")

    # Optional: reset other tables if needed
    # cur.execute("DELETE FROM pos_transactions")

    conn.commit()
    print("All tables cleared successfully!")
except Exception as e:
    conn.rollback()
    print("Error:", e)
finally:
    conn.close()
