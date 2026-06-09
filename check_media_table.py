import sqlite3

def check_media_table():
    conn = sqlite3.connect("tclab.db")  # change db name if different
    cur = conn.cursor()

    try:
        cur.execute("PRAGMA table_info(media)")
        columns = cur.fetchall()

        if not columns:
            print("❌ Table 'media' does not exist!")
        else:
            print("✅ Columns in 'media' table:")
            for col in columns:
                # col[1] = column name, col[2] = type
                print(f"- {col[1]} ({col[2]})")

    except sqlite3.Error as e:
        print("Error:", e)

    finally:
        conn.close()

if __name__ == "__main__":
    check_media_table()
