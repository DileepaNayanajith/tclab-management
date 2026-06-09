import sqlite3

DB_PATH = "tclab.db"  # change if your DB has a different name

def main():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row

    print("\n=== All rows in subculture_bottles ===\n")
    rows = conn.execute("SELECT * FROM subculture_bottles").fetchall()
    for row in rows:
        print(dict(row))

    print("\n=== Available Multiply Plants ===\n")
    multiply = conn.execute("""
        SELECT plant_code, plant_name, COALESCE(SUM(multiply),0) as count
        FROM subculture_bottles
        WHERE status='active'
        GROUP BY plant_code, plant_name
        HAVING count > 0
    """).fetchall()
    for row in multiply:
        print(dict(row))

    print("\n=== Available Rooting Plants ===\n")
    rooting = conn.execute("""
        SELECT plant_code, plant_name, COALESCE(SUM(rooting),0) as count
        FROM subculture_bottles
        WHERE status='active'
        GROUP BY plant_code, plant_name
        HAVING count > 0
    """).fetchall()
    for row in rooting:
        print(dict(row))

    print("\n=== Contamination Summary ===\n")
    total = conn.execute("SELECT COALESCE(SUM(multiply+rooting),0) as total FROM subculture_bottles WHERE status='active'").fetchone()["total"]
    contaminated = conn.execute("SELECT COALESCE(SUM(multiply+rooting),0) as contaminated FROM subculture_bottles WHERE status='contaminated'").fetchone()["contaminated"]
    contamination_percent = round((contaminated / total * 100),2) if total else 0
    print(f"Total active plants: {total}")
    print(f"Contaminated plants: {contaminated}")
    print(f"Contamination %: {contamination_percent}%")

    print("\n=== Bottles older than 6 weeks ===\n")
    old_bottles = conn.execute("""
        SELECT barcode, plant_code, plant_name as variety,
               ROUND((julianday('now') - julianday(date_created))/7,1) as weeks
        FROM subculture_bottles
        WHERE ((julianday('now') - julianday(date_created))/7) > 6
          AND status='active'
        ORDER BY weeks DESC
    """).fetchall()
    for row in old_bottles:
        print(dict(row))

    conn.close()

if __name__ == "__main__":
    main()
