# check_media_hormones.py
import sqlite3

DB_NAME = "tclab.db"   # <-- change if your database file has another name

def check_hormones():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    try:
        # Example assuming a junction table: media_hormones(media_code, hormone_id)
        cur.execute("""
            SELECT mc.media_code, mc.basal_media, h.hormone_name, h.concentration
            FROM media_compositions mc
            LEFT JOIN media_hormones mh ON mc.media_code = mh.media_code
            LEFT JOIN hormones h ON mh.hormone_id = h.id
            ORDER BY mc.media_code
        """)
        rows = cur.fetchall()

        if not rows:
            print("⚠️ No records found in media_compositions or linked hormones.")
            return

        print("📋 Media Codes with Hormones:")
        for row in rows:
            media_code = row["media_code"]
            basal = row["basal_media"]
            hormone = row["hormone_name"] if row["hormone_name"] else "None"
            conc = row["concentration"] if row["concentration"] else "-"
            print(f"Media Code: {media_code} | Basal: {basal} | Hormone: {hormone} | Conc: {conc}")

    except sqlite3.Error as e:
        print("❌ SQLite error:", e)
    finally:
        conn.close()

if __name__ == "__main__":
    check_hormones()
