from flask import Flask, render_template, request, redirect, url_for, session, flash, send_file,jsonify
app = Flask(__name__)
import sqlite3
import os
from flask import send_file
import barcode
from barcode.writer import ImageWriter
from io import BytesIO
import random
import html
import barcode as bc
import uuid
from datetime import date, datetime
from werkzeug.security import check_password_hash
import traceback

app.secret_key = "19650316sumanasiri"

@app.before_request
def refresh_session_permissions():
    if "user_id" not in session:
        return

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT 
            role,
            COALESCE(can_subculture, 0),
            COALESCE(can_discard, 0),
            COALESCE(can_dashboard, 0),
            COALESCE(can_register_media, 0),
            COALESCE(can_register_mother_plants, 0),
            COALESCE(can_price_list, 0),
            COALESCE(can_pos_rooted, 0)
        FROM users
        WHERE id = ?
    """, (session["user_id"],))

    user = cur.fetchone()
    conn.close()

    if user:
        session["role"] = user[0]
        session["can_subculture"] = int(user[1])
        session["can_discard"] = int(user[2])
        session["can_dashboard"] = int(user[3])
        session["can_register_media"] = int(user[4])
        session["can_register_mother_plants"] = int(user[5])
        session["can_price_list"] = int(user[6])
        session["can_pos_rooted"] = int(user[7])


DB_NAME = "tclab.db"

def get_db_connection():
    db_path = os.path.join(os.path.dirname(__file__), DB_NAME)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn



from werkzeug.security import check_password_hash
import sqlite3

@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        username = request.form.get("username")
        password = request.form.get("password")

        conn = get_db_connection()
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()

        cur.execute("""
    SELECT 
        id,
        username,
        password,
        role,
        COALESCE(can_subculture, 0) AS can_subculture,
        COALESCE(can_discard, 0) AS can_discard,
        COALESCE(can_dashboard, 0) AS can_dashboard,
        COALESCE(can_register_media, 0) AS can_register_media,
        COALESCE(can_register_mother_plants, 0) AS can_register_mother_plants,
        COALESCE(can_price_list, 0) AS can_price_list,
        COALESCE(can_pos_rooted, 0) AS can_pos_rooted
    FROM users
    WHERE username = ?
""", (username,))

        user = cur.fetchone()
        conn.close()

        # ❌ USER NOT FOUND
        if user is None:
            flash("Invalid username or password", "danger")
            return redirect(url_for("login"))

        try:
            # ✅ PASSWORD CHECK (supports hashed OR plain password)
            password_ok = False

            # plain text check (for your current DB)
            #if user["password"] == password:
            if user and user["password"] == password:
                password_ok = True

            # hashed password check (future-safe)
            else:
                try:
                    password_ok = check_password_hash(user["password"], password)
                except:
                    password_ok = False
            print("Username:", username)
            print("User found:", user is not None)
            print("Password OK:", password_ok)
            if password_ok:

                session.clear()

                session["user_id"] = user["id"]
                session["username"] = user["username"]
                session["role"] = user["role"]

                # SAFE CONVERSIONS
                session["can_subculture"] = int(user["can_subculture"] or 0)
                session["can_discard"] = int(user["can_discard"] or 0)
                session["can_dashboard"] = int(user["can_dashboard"] or 0)
                session["can_register_media"] = int(user["can_register_media"] or 0)
                session["can_register_mother_plants"] = int(user["can_register_mother_plants"] or 0)
                session["can_price_list"] = int(user["can_price_list"] or 0)
                session["can_pos_rooted"] = int(user["can_pos_rooted"] or 0)

                return redirect(url_for("dashboard"))

            else:
                flash("Invalid username or password", "danger")
                return redirect(url_for("login"))

        #except Exception as e:
                #print("LOGIN CRASH ERROR:")
                #print(traceback.format_exc())

                #flash("Internal error occurred. Check logs.", "danger")
                #return redirect(url_for("login"))
            
        except Exception as e:
                import traceback
                print("🔥 LOGIN CRASH FULL TRACE:")
                print(traceback.format_exc())
                print("USER DATA:", user)
                return f"Login error: {e}", 500
             

    return render_template("login.html")


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))

@app.route("/dashboard")
def dashboard():
    if "username" not in session:
        return redirect(url_for("login"))
    
    print("SESSION:",
        session.get("username"),
        session.get("role"),
        session.get("can_subculture"),
        session.get("can_discard"),
        session.get("can_dashboard"))

      # -------------------------
    # ACCESS CONTROL (IMPORTANT FIX)
    # -------------------------
    is_admin = session.get("role") == "admin"

    has_any_access = (
        is_admin
        or session.get("can_dashboard") == 1
        or session.get("can_subculture") == 1
        or session.get("can_discard") == 1
        or session.get("can_register_media") == 1
        or session.get("can_pos_rooted") == 1
    )

    if not has_any_access:
        return "Access Denied"

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # -----------------------------
    # Mother bottles (multiply stage)
    # -----------------------------
    mother_totals = cur.execute("""
         SELECT   
            plant_code,
            plant_name,
            COALESCE(SUM(num_plants),0) AS mother_total
        FROM mother_bottles
        WHERE status='Active'
          AND id NOT IN (SELECT parent_id FROM subculture_bottles)
        GROUP BY plant_code, plant_name
    """).fetchall()
    
    # -----------------------------
    # Subculture bottles (multiply + rooting)
    # -----------------------------
    subculture_totals = cur.execute("""
        SELECT 
            plant_code,
            plant_name,
            COALESCE(SUM(CASE WHEN multiply = 1 THEN num_plants ELSE 0 END),0) AS multiply_total,
            COALESCE(SUM(CASE WHEN rooting = 1 THEN num_plants ELSE 0 END),0)  AS rooting_total
        FROM subculture_bottles
        WHERE status='Active'
        GROUP BY plant_code, plant_name
    """).fetchall()

    # -----------------------------
    # Discards (subtract)
    # ----------------------------- 

    discard_totals = cur.execute("""
    SELECT 
        s.plant_code,
        s.plant_name,
        COALESCE(SUM(CASE 
            WHEN d.reason IN ('mother','multiply') THEN d.num_bottles 
            ELSE 0 END),0) AS multiply_discard,
        COALESCE(SUM(CASE 
            WHEN d.reason='rooting' THEN d.num_bottles 
            ELSE 0 END),0) AS rooting_discard
    FROM discards d
    LEFT JOIN subculture_bottles s 
        ON s.barcode = d.barcode
    GROUP BY s.plant_code, s.plant_name
""").fetchall()
    


    # -----------------------------
    # Combine totals
    # -----------------------------
    totals = {}

    # Mother bottles
    for row in mother_totals:
        key = row["plant_code"]
        totals.setdefault(key, {"plant_name": row["plant_name"], "multiply":0, "rooting":0})
        totals[key]["multiply"] += row["mother_total"]

    # Subculture bottles
    for row in subculture_totals:
        key = row["plant_code"]
        totals.setdefault(key, {"plant_name": row["plant_name"], "multiply":0, "rooting":0})
        totals[key]["multiply"] += row["multiply_total"]
        totals[key]["rooting"]  += row["rooting_total"]

    # Discards
    for row in discard_totals:
        key = row["plant_code"]
        totals.setdefault(key, {"plant_name": row["plant_name"], "multiply":0, "rooting":0})
        totals[key]["multiply"] = max(totals[key]["multiply"] - row["multiply_discard"], 0)
        totals[key]["rooting"]  = max(totals[key]["rooting"] - row["rooting_discard"], 0)

    # -----------------------------
    # Prepare for dashboard
    # -----------------------------
    plant_totals = [
        {
            "plant_code": code,
            "plant_name": data["plant_name"],
            "multiply": data["multiply"],
            "rooting": data["rooting"],
            "grand_total": data["multiply"] + data["rooting"]
        }
        for code, data in totals.items()
        if data["multiply"] != 0 or data["rooting"] != 0
    ]

    # -----------------------------
    # Bottles older than 6 weeks
    # -----------------------------
    old_bottles = cur.execute("""
        SELECT 
            s.cycle AS subculture_week,
            s.plant_code,
            s.plant_name AS variety,
            COUNT(*) AS bottle_count,
             SUM(s.num_plants) AS total_plants,
            CAST((julianday('now') - julianday(s.date_created)) / 7 AS INTEGER) || ' weeks' AS age,
            CAST((julianday('now') - julianday(s.date_created)) / 7 AS INTEGER) AS week_number
        FROM subculture_bottles s
        LEFT JOIN subculture_bottles d
            ON d.parent_id = s.id AND d.status='Active'
        WHERE ((julianday('now') - julianday(s.date_created)) / 7) > 6
            AND s.status='Active'
            AND s.multiply = 1
            AND d.id IS NULL
         GROUP BY s.cycle, s.plant_code, s.plant_name, age
        ORDER BY (julianday('now') - julianday(s.date_created)) DESC
    """).fetchall()

    conn.close()

    return render_template(
        "dashboard.html",
        plant_totals=plant_totals,
        old_bottles=old_bottles
    )

#-------------Add  users--------------------
from werkzeug.security import generate_password_hash
import sqlite3

@app.route("/create_users", methods=["GET", "POST"])
def create_users():
    # ✅ Must be logged in
    if "user_id" not in session:
        return redirect(url_for("login"))

    # ✅ Only admin can access
    if session.get("role") != "admin":
        return "Access Denied"

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    if request.method == "POST":
        full_name = request.form.get("full_name")
        username = request.form.get("username")
        raw_password = request.form.get("password")
        role = request.form.get("role")

        # ✅ HASH PASSWORD (IMPORTANT)
        password = generate_password_hash(raw_password)

        # ✅ PERMISSIONS (checkbox → 1/0)
        can_register_media = 1 if request.form.get("can_register_media") else 0
        can_subculture = 1 if request.form.get("can_subculture") else 0
        can_discard = 1 if request.form.get("can_discard") else 0
        can_register_mother_plants = 1 if request.form.get("can_register_mother_plants") else 0
        can_price_list = 1 if request.form.get("can_price_list") else 0
        can_pos_rooted = 1 if request.form.get("can_pos_rooted") else 0
        can_dashboard = 1 if request.form.get("can_dashboard") else 0

        try:
            cur.execute("""
                INSERT INTO users 
                (full_name, username, password, role,
                 can_register_media, can_subculture, can_discard,
                 can_register_mother_plants, can_price_list,
                 can_pos_rooted, can_dashboard)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                full_name,
                username,
                password,   # ✅ hashed password
                role,
                can_register_media,
                can_subculture,
                can_discard,
                can_register_mother_plants,
                can_price_list,
                can_pos_rooted,
                can_dashboard
            ))

            conn.commit()
            flash("User created successfully!", "success")

        except sqlite3.IntegrityError:
            flash(f"Username '{username}' already exists!", "danger")

    # ✅ Show users list
    cur.execute("""
        SELECT id, username, role,
               can_register_media, can_subculture, can_discard,
               can_register_mother_plants, can_price_list,
               can_pos_rooted, can_dashboard
        FROM users
    """)
    users = cur.fetchall()

    conn.close()

    return render_template("create_users.html", users=users)

#-------------Edit users--------------------

from werkzeug.security import generate_password_hash
import sqlite3

@app.route("/edit_user/<int:user_id>", methods=["GET", "POST"])
def edit_user(user_id):

    if "user_id" not in session:
        return redirect(url_for("login"))

    if session.get("role") != "admin":
        return "Access Denied"

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # ✅ GET USER
    cur.execute("SELECT * FROM users WHERE id = ?", (user_id,))
    user = cur.fetchone()

    if not user:
        conn.close()
        return "User not found"

    # =========================
    # POST (UPDATE USER)
    # =========================
    if request.method == "POST":

        full_name = request.form.get("full_name")
        username = request.form.get("username")
        role = request.form.get("role")

        # ✅ PASSWORD HANDLING
        password = request.form.get("password")

        can_register_media = 1 if request.form.get("can_register_media") else 0
        can_subculture = 1 if request.form.get("can_subculture") else 0
        can_discard = 1 if request.form.get("can_discard") else 0
        can_register_mother_plants = 1 if request.form.get("can_register_mother_plants") else 0
        can_price_list = 1 if request.form.get("can_price_list") else 0
        can_pos_rooted = 1 if request.form.get("can_pos_rooted") else 0
        can_dashboard = 1 if request.form.get("can_dashboard") else 0

        # =========================
        # CASE 1: PASSWORD UPDATED
        # =========================
        if password:
            hashed_password = generate_password_hash(password)

            cur.execute("""
                UPDATE users
                SET full_name = ?,
                    username = ?,
                    password = ?,
                    role = ?,
                    can_register_media = ?,
                    can_subculture = ?,
                    can_discard = ?,
                    can_register_mother_plants = ?,
                    can_price_list = ?,
                    can_pos_rooted = ?,
                    can_dashboard = ?
                WHERE id = ?
            """, (
                full_name,
                username,
                hashed_password,
                role,
                can_register_media,
                can_subculture,
                can_discard,
                can_register_mother_plants,
                can_price_list,
                can_pos_rooted,
                can_dashboard,
                user_id
            ))

        # =========================
        # CASE 2: NO PASSWORD CHANGE
        # =========================
        else:
            cur.execute("""
                UPDATE users
                SET full_name = ?,
                    username = ?,
                    role = ?,
                    can_register_media = ?,
                    can_subculture = ?,
                    can_discard = ?,
                    can_register_mother_plants = ?,
                    can_price_list = ?,
                    can_pos_rooted = ?,
                    can_dashboard = ?
                WHERE id = ?
            """, (
                full_name,
                username,
                role,
                can_register_media,
                can_subculture,
                can_discard,
                can_register_mother_plants,
                can_price_list,
                can_pos_rooted,
                can_dashboard,
                user_id
            ))

        conn.commit()
        conn.close()

        flash("User updated successfully!", "success")
        return redirect(url_for("create_users"))

    conn.close()

    return render_template("edit_user.html", user=user)


# ---------------- check mother bottle----------------
@app.route("/check_mother/<barcode>")
def check_mother(barcode):
    conn = get_db_connection()
    cur = conn.cursor()

    bottle = cur.execute("""
        SELECT status FROM mother_bottles WHERE barcode = ?
    """, (barcode,)).fetchone()

    conn.close()

    if not bottle:
        return {"status": "not_found"}  # not in mother_bottles
    return {"status": bottle["status"]}
# ----------------  check mother bottle end-- ----------------

# ---------------- Register New Plant ----------------
@app.route("/register_plant", methods=["GET", "POST"])
def register_plant():
    if "username" not in session:
        return redirect(url_for("login"))

    if request.method == "POST":
        code = request.form.get("code").strip()
        name = request.form.get("name").strip()
        variety = request.form.get("variety", "").strip()
        description = request.form.get("description", "").strip()

        conn = get_db_connection()
        cur = conn.cursor()

        # Check if plant code already exists
        cur.execute("SELECT id FROM plants WHERE code = ?", (code,))
        existing = cur.fetchone()

        if existing:
            conn.close()
            flash(f"Plant with code {code} already exists!", "error")
            return redirect(url_for("register_plant"))

        # Insert new plant
        cur.execute("""
            INSERT INTO plants (code, name, variety, description)
            VALUES (?, ?, ?, ?)
        """, (code, name, variety, description))
        conn.commit()
        conn.close()

        #flash(f"✅ Plant {name} ({code}) registered successfully!", "success")
        return redirect(url_for("register_plant"))

    # If GET request → render form
    return render_template("register_plant.html")
# ---------------- Register New Plant End ----------------

# ------------------- View Plants -------------------
@app.route("/view_plants")
def view_plants():
    if "username" not in session:
        return redirect(url_for("login"))

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM plants")
    plants = cur.fetchall()
    conn.close()
    return render_template("view_plants.html", plants=plants)

# ------------------- Update Plant -------------------
@app.route("/update_plant/<int:plant_id>", methods=["GET", "POST"])
def update_plant(plant_id):
    if "username" not in session:
        return redirect(url_for("login"))

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM plants WHERE id = ?", (plant_id,))
    plant = cur.fetchone()

    if not plant:
        conn.close()
        flash("Plant not found!", "danger")
        return redirect(url_for("view_plants"))

    if request.method == "POST":
        code = request.form["code"].strip()
        name = request.form["name"].strip()
        variety = request.form["variety"].strip()
        description = request.form["description"].strip()

        cur.execute("""
            UPDATE plants
            SET code = ?, name = ?, variety = ?, description = ?
            WHERE id = ?
        """, (code, name, variety, description, plant_id))

        conn.commit()
        conn.close()
        #flash("Plant updated successfully!", "success")
        return redirect(url_for("view_plants"))

    conn.close()
    return render_template("update_plant.html", plant=plant)

# ------------------- Delete Plant -------------------
@app.route("/delete_plant/<int:plant_id>")
def delete_plant(plant_id):
    if "username" not in session:
        return redirect(url_for("login"))

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM plants WHERE id = ?", (plant_id,))
    conn.commit()
    conn.close()    
    return redirect(url_for("view_plants"))

# ------------------- Run Server -------------------

# ------------------- Register mother bottles -------------------


DB_PATH = "tclab.db"

# --- Database connection ---
def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

#-----Register Mother Bottle------------------
@app.route("/register_mother_bottle", methods=["GET", "POST"])
def register_mother_bottle():
    conn = get_db_connection()
    cur = conn.cursor()

    # Get dropdown data
    cur.execute("SELECT * FROM plants")
    plants = cur.fetchall()

    cur.execute("SELECT media_code FROM media_compositions")
    media_list = cur.fetchall()

    if request.method == "POST":
        plant_code = request.form["plant_code"]
        plant_name = request.form["plant_name"]
        media_code = request.form["media_code"]
        num_plants = request.form["num_plants"]
        cycle = request.form["cycle"]
        #technician = request.form["technician"].strip()
        technician = session.get("username")
        culture_week = request.form.get("culture_week")
        lamina_flow = request.form.get("lamina_flow")

        barcode_value = f"{plant_code[:3].upper()}{cycle}{random.randint(100,999)}"

        cur.execute("""
            INSERT INTO mother_bottles
            (barcode, plant_code, plant_name, media_code, num_plants, cycle, technician, culture_week, lamina_flow, printed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """, (barcode_value, plant_code, plant_name, media_code, num_plants, cycle, technician, culture_week, lamina_flow))


        conn.commit()
        bottle_id = cur.lastrowid
        conn.close()

        # Redirect to print page
        return redirect(url_for("print_printing_mother_bottle", bottle_id=bottle_id))

    # For GET
    cur.execute("SELECT * FROM mother_bottles ORDER BY id DESC")
    mother_bottles = cur.fetchall()
    conn.close()

    # Provide today's date and week number to the form
    import datetime
    today = datetime.date.today()
    current_week = today.isocalendar()[1]

    return render_template(
        "register_mother_bottle.html",
        plants=plants,
        media_list=media_list,
        mother_bottles=mother_bottles,
        today_date=today.strftime("%Y-%m-%d"),
        current_week=current_week
    )

# --- Generate Barcode Image ---
#@app.route("/generate_barcode/<barcode_value>")
#def generate_barcode(barcode_value):
    #CODE128 = barcode.get_barcode_class("code128")
    #buffer = BytesIO()
    #CODE128(barcode_value, writer=ImageWriter()).write(buffer)
    #buffer.seek(0)
    #return send_file(buffer, mimetype="image/png")

@app.route("/generate_barcode/<barcode_value>")
def generate_barcode(barcode_value):
    CODE128 = barcode.get_barcode_class("code128")

    buffer = BytesIO()

    writer_options = {
        "module_width": 0.25,   # thickness of bars
        "module_height": 7,    # height of barcode (important for 10mm label)
        #"font_size": 5,        # small text
        #"text_distance": 2,    # space between barcode & text
        "quiet_zone": 1,        # margin around barcode
        "write_text": True 

    }

    CODE128(barcode_value, writer=ImageWriter()).write(
        buffer, options=writer_options
    )

    buffer.seek(0)

    return send_file(buffer, mimetype="image/png")


# --- View Mother Bottles for Printing ---
@app.route("/view_printing_mother_bottles")
def view_printing_mother_bottles():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM mother_bottles ORDER BY id DESC")
    mother_bottles = cur.fetchall()
    conn.close()
    return render_template("view_printing_mother_bottles.html", mother_bottles=mother_bottles)

# --- Print & Mark as Printed ---
@app.route("/print_printing_mother_bottle/<int:bottle_id>")
def print_printing_mother_bottle(bottle_id):
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("SELECT * FROM mother_bottles WHERE id = ?", (bottle_id,))
    bottle = cur.fetchone()
    if not bottle:
        conn.close()
        flash("Bottle not found!")
        return redirect(url_for("view_printing_mother_bottles"))

    # Mark as printed
    cur.execute("UPDATE mother_bottles SET printed = 1 WHERE id = ?", (bottle_id,))
    conn.commit()
    conn.close()

    return render_template("print_printing_mother_bottle.html", bottle=bottle)


# ------------------- Register mother bottle end-------------------






# ---------------- Register Media ----------------
@app.route('/register_media', methods=['GET', 'POST'])
def register_media():
    conn = get_db_connection()
    cur = conn.cursor()

    # Fetch media list with hormones
    cur.execute("""
        SELECT m.media_code, m.basal_media, 
               GROUP_CONCAT(h.hormone_name || ' ' || h.hormone_mg, ', ') AS hormones
        FROM media_compositions m
        LEFT JOIN media_hormones h ON m.media_code = h.media_code
        GROUP BY m.media_code, m.basal_media
    """)
    media_list = cur.fetchall()

    if request.method == 'POST':
        media_code = request.form['media_code']
        date_prepared = request.form.get('date_prepared')  # editable system date
        technician = request.form['technician']
        num_bottles = int(request.form['num_bottles'])

        date_obj = datetime.strptime(date_prepared, "%Y-%m-%d")
        week_number = date_obj.isocalendar()[1]

        new_barcodes = []

        #for _ in range(num_bottles):
            #barcode_value = f"{media_code}-W{week_number}-{uuid.uuid4().hex[:6]}"
        
        for i in range(num_bottles):
            bottle_no = str(i + 1).zfill(2)
            barcode_value = f"{media_code}-{week_number}-{bottle_no}"

            try:
                cur.execute("""
                    INSERT INTO bottles (barcode, media_code, date_prepared, technician, status)
                    VALUES (?, ?, ?, ?, ?)
                """, (barcode_value, media_code, date_prepared, technician, "Media Prepared"))
                new_barcodes.append(barcode_value)
            except sqlite3.IntegrityError:
                flash(f"⚠ Barcode already exists, skipping: {barcode_value}", "error")
                continue

        conn.commit()
        conn.close()

        if new_barcodes:
            session['new_barcodes'] = new_barcodes  # store in session
            return redirect(url_for('print_media_barcodes'))

        flash("No bottles saved. Please try again.", "error")
        return redirect(url_for('register_media'))

    conn.close()
    return render_template('register_media.html', media_list=media_list, today_date=date.today())


# ---------------- Print Media Barcodes ----------------
@app.route('/print_media_barcodes')
def print_media_barcodes():
    barcodes = session.pop('new_barcodes', [])
    if not barcodes:
        flash("No barcodes to print.", "error")
        return redirect(url_for('register_media'))
    return render_template("print_media_bottles_barcodes.html", barcodes=barcodes)





# ------------------- Barcode media Generator -------------------
#@app.route('/generate_media_barcode/<barcode_value>')
#def generate_media_barcode(barcode_value):
    #from io import BytesIO
    #import barcode
    #from barcode.writer import ImageWriter
    #from flask import send_file

    #code128 = barcode.get('code128', barcode_value, writer=ImageWriter())
    #buffer = BytesIO()
    #code128.write(buffer, {"write_text": False, "module_height": 15, "font_size": 0})
    #buffer.seek(0)
    #return send_file(buffer, mimetype="image/png")


@app.route('/generate_media_barcode/<barcode_value>')
def generate_media_barcode(barcode_value):
    from io import BytesIO
    import barcode
    from barcode.writer import ImageWriter
    from flask import send_file

    code128 = barcode.get('code128', barcode_value, writer=ImageWriter())
    buffer = BytesIO()

    code128.write(buffer, {
        "write_text": True,
        "module_width": 0.3,
        "module_height": 6,
        "font_size" : 8,
        "text_distance" : 4,
        "quiet_zone": 0.2
    })

    buffer.seek(0)
    return send_file(buffer, mimetype="image/png")




# -------------------reprint Barcode if technician did not print  -------------------
@app.route('/reprint_media_barcodes/<int:media_id>')
def reprint_media_barcodes(media_id):
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT barcode 
        FROM bottles 
        WHERE id = ?
    """, (media_id,))
    bottle = cur.fetchone()
    conn.close()

    if not bottle:
        flash("❌ Bottle not found!", "error")
        return redirect(url_for('view_media'))

    return render_template("print_media_bottles_barcodes.html", barcodes=[bottle["barcode"]])




# ---------------- View Bottles ----------------
@app.route("/view_media")
def view_bottles():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT id, media_code, basal_media, hormones, ph, agar, date_prepared, technician, barcode
        FROM bottles
        ORDER BY id DESC
    """)
    bottles = cur.fetchall()
    conn.close()
    return render_template("view_media.html", bottles=bottles)
# ---------------- View Bottles ----------------


# ---------------- Register media composition ----------------
@app.route("/register_media_compositions", methods=["GET", "POST"])
def register_media_compositions():
    conn = get_db_connection()
    cur = conn.cursor()
    popup_message = None  # for JS alert

    if request.method == "POST":
        media_code = request.form["media_code"].strip()
        basal_media = request.form["basal_media"]

        # Check if media code already exists
        cur.execute("SELECT 1 FROM media_compositions WHERE media_code=?", (media_code,))
        if cur.fetchone():
            conn.close()
            popup_message = f"Media code '{media_code}' already exists!"
            return render_template(
                "register_media_compositions.html",
                media_records=get_media_records(),
                popup_message=popup_message
            )

        # Insert into media_compositions (main table)
        cur.execute("""
            INSERT INTO media_compositions (media_code, basal_media)
            VALUES (?, ?)
        """, (media_code, basal_media))

        # Fixed hormones (2 slots)
        for i in range(1, 3):
            hormone = request.form.get(f"hormone{i}") or "None"
            hormone_mg = request.form.get(f"hormone{i}_mg") or 0
            cur.execute("""
                INSERT INTO media_hormones (media_code, hormone_name, hormone_mg)
                VALUES (?, ?, ?)
            """, (media_code, hormone, hormone_mg))

        # Extra hormones
        extra_hormones = request.form.getlist("extra_hormone_name[]")
        extra_mg = request.form.getlist("extra_hormone_mg[]")
        for h, mg in zip(extra_hormones, extra_mg):
            if not h:
                h = "None"
            if not mg:
                mg = 0
            cur.execute("""
                INSERT INTO media_hormones (media_code, hormone_name, hormone_mg)
                VALUES (?, ?, ?)
            """, (media_code, h, mg))

        conn.commit()
        conn.close()
        popup_message = "Media composition saved successfully!"
        return render_template(
            "register_media_compositions.html",
            media_records=get_media_records(),
            popup_message=popup_message
        )

    # GET request
    conn.close()
    return render_template(
        "register_media_compositions.html",
        media_records=get_media_records()
    )


# helper function to fetch media + hormones
def get_media_records():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT mc.media_code,
               mc.basal_media,
               GROUP_CONCAT(mh.hormone_name || ' ' || mh.hormone_mg || 'mg', ', ') AS hormones
        FROM media_compositions mc
        LEFT JOIN media_hormones mh ON mc.media_code = mh.media_code
        GROUP BY mc.media_code, mc.basal_media
        ORDER BY mc.media_code
    """)
    rows = cur.fetchall()
    conn.close()
    return rows
# ---------------- Register media composition end ----------------


# ----------------Subculture entry ----------------
@app.route('/subculture_entry', methods=['GET', 'POST'])
def subculture_entry():
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # Fetch available media bottles
    cur.execute("""
        SELECT media_code, COUNT(*) AS available_bottles
        FROM bottles
        WHERE status='Media Prepared'
        GROUP BY media_code
    """)
    media_available = cur.fetchall()

    if request.method == 'POST':
        mother_barcode = request.form['mother_barcode'].strip()
        media_code = request.form['media_code']
        num_daughter = int(request.form['num_daughter'])
        num_plants = int(request.form['num_plants'])   # plants per daughter
        technician = request.form['technician'].strip()
        subculture_date = request.form.get('subculture_date') or str(date.today())
        subculture_week = request.form.get('subculture_week')
        lamina_flow = request.form.get('lamina_flow')
        session["lamina_flow"] = lamina_flow
        selected_type = request.form.get('type')

        if not selected_type:
            flash("Please select Multiply or Rooting.", "error")
            conn.close()
            return redirect(url_for('subculture_entry'))

        multiply = 1 if selected_type == "multiply" else 0
        rooting = 1 if selected_type == "rooting" else 0

       # --- Fetch mother/daughter info (check both tables) ---
        cur.execute("SELECT *, 'mother_bottles' as table_name FROM mother_bottles WHERE barcode=?", (mother_barcode,))
        mother_info = cur.fetchone()

        if not mother_info:
            cur.execute("SELECT *, 'subculture_bottles' as table_name FROM subculture_bottles WHERE barcode=?", (mother_barcode,))
            mother_info = cur.fetchone()

        # Check if bottle exists
        if not mother_info:
            flash("Mother/Daughter bottle not found!", "error")
            conn.close()
            return redirect(url_for('subculture_entry'))

        # 🔹 Check discard status for both mother and daughter bottles
        if mother_info['status'] == "Discarded":
            flash("This bottle is discarded and cannot be used!", "error")
            conn.close()
            return redirect(url_for('subculture_entry'))

        # ✅ Safe values
        table_name = mother_info['table_name']
        plant_code = mother_info['plant_code']
        plant_name = mother_info['plant_name']
        last_cycle = int(mother_info['cycle'] or 0)
        new_cycle = last_cycle + 1

        # --- Check if this parent has been used before ---
        cur.execute("""
            SELECT COUNT(*) as used_count
            FROM subculture_bottles
            WHERE parent_id=? 
        """, (mother_info['id'],))
        already_used_ever = cur.fetchone()['used_count'] > 0

        # Plants to deduct only if never used before
        plants_to_deduct = 0
        if not already_used_ever:
            plants_to_deduct = mother_info['num_plants'] or 0
            cur.execute(f"""
                UPDATE {table_name}
                SET num_plants = 0
                WHERE id = ?
            """, (mother_info['id'],))

        # Mark the mother bottle as used
        cur.execute(f"UPDATE {table_name} SET status='Used' WHERE id=?", (mother_info['id'],))

        # Determine starting index for daughter numbering
        cur.execute("""
            SELECT MAX(CAST(SUBSTR(barcode, -3) AS INTEGER)) AS last_index
            FROM subculture_bottles
            WHERE parent_id=? AND cycle=? AND subculture_week=?
        """, (mother_info['id'], new_cycle, subculture_week))
        last_index_row = cur.fetchone()
        start_index = (last_index_row['last_index'] or 0) + 1

        new_barcodes = []

        # Generate daughter bottles
        for n in range(num_daughter):
            i = start_index + n
            week_part = str(subculture_week).replace("W", "").strip()
            barcode_value = f"{plant_code}-{week_part}-{str(i).zfill(3)}"

            # Ensure uniqueness
            while cur.execute("SELECT 1 FROM subculture_bottles WHERE barcode=?", (barcode_value,)).fetchone():
                i += 1
                barcode_value = f"{plant_code}-{week_part}-{str(i).zfill(3)}"

            cur.execute("""
                INSERT INTO subculture_bottles
                (barcode, plant_code, plant_name, media_code, num_plants, cycle,
                 technician, parent_id, status, origin, date_created,
                 multiply, rooting, subculture_week, lamina_flow)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                barcode_value, plant_code, plant_name, media_code, num_plants,
                new_cycle, technician, mother_info['id'],
                "Active", "Subculture", subculture_date,
                multiply, rooting, subculture_week, lamina_flow
            ))

            new_barcodes.append(barcode_value)

            # Deduct ONE media bottle per daughter
            cur.execute("""
                SELECT id FROM bottles
                WHERE media_code=? AND status='Media Prepared'
                LIMIT 1
            """, (media_code,))
            media_bottle = cur.fetchone()
            if media_bottle:
                cur.execute("UPDATE bottles SET status='Used' WHERE id=?", (media_bottle['id'],))

        # Update cycle in mother
        cur.execute(f"UPDATE {table_name} SET cycle=? WHERE id=?", (new_cycle, mother_info['id']))

        conn.commit()
        conn.close()

        return render_template("print_subculture_barcodes.html", barcodes=new_barcodes, deducted=plants_to_deduct)

    conn.close()
    return render_template('subculture_entry.html',
                            media_available=media_available, 
                            today_date=date.today(),
                            saved_lamina=session.get("lamina_flow")
                           )
# ----------------Subculture entry ends ----------------


from flask import jsonify

@app.route("/get_mother_info/<barcode>")
def get_mother_info(barcode):
    conn = sqlite3.connect("tclab.db")
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    barcode = barcode.strip()
    table_name = "mother_bottles"

    # Step 1: Look in mother_bottles
    cur.execute("""
        SELECT id, barcode, cycle, plant_code, plant_name, media_code, technician, status
        FROM mother_bottles
        WHERE barcode = ? COLLATE NOCASE
        LIMIT 1
    """, (barcode,))
    mother = cur.fetchone()

    # Step 2: If not found, look in subculture_bottles (daughter can become mother)
    if not mother:
        cur.execute("""
            SELECT id, barcode, cycle, plant_code, plant_name, media_code, technician, status
            FROM subculture_bottles
            WHERE barcode = ? COLLATE NOCASE
            LIMIT 1
        """, (barcode,))
        mother = cur.fetchone()
        table_name = "subculture_bottles"

    if mother:
        # ✅ Check if this bottle has already been used for subculture
        cur.execute("SELECT 1 FROM subculture_bottles WHERE parent_id=? LIMIT 1", (mother['id'],))
        already_subcultured = bool(cur.fetchone())

        response = {
            "id": mother["id"],
            "barcode": mother["barcode"],
            "cycle": mother["cycle"],
            "plant_code": mother["plant_code"],
            "plant_name": mother["plant_name"],
            "media_code": mother["media_code"],
            "technician": mother["technician"],
            "status": mother["status"],   # 👈 Added here
            "already_subcultured": already_subcultured,
            "table": table_name
        }

        conn.close()
        return jsonify(response)

    conn.close()
    return jsonify({"error": "Mother bottle not found"})

@app.route('/print_subculture_barcode/<int:bottle_id>')
def print_subculture_barcode(bottle_id):
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("SELECT barcode FROM subculture_bottles WHERE id=?", (bottle_id,))
    bottle = cur.fetchone()
    conn.close()

    if not bottle:
        flash("Bottle not found!", "error")
        return redirect(url_for('view_subculture'))

    # Pass as a list so the template loop works
    return render_template("print_subculture_barcodes.html", barcodes=[bottle["barcode"]])


# ------View subculture bottles---------------------------------
@app.route('/view_subculture')
def view_subculture():
    LAMINA_MAP = {
        "LF1": "A", "LF2": "B", "LF3": "C",
        "LF4": "D", "LF5": "E", "LF6": "F"
    }

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    cur.execute("""
        SELECT 
            s.id,
            s.barcode AS daughter_barcode,
            m.barcode AS mother_barcode,
            s.plant_code,
            s.plant_name,
            s.media_code,
            s.num_plants,
            s.cycle,
            s.technician,
            s.date_created,
            s.subculture_week,   
            s.lamina_flow,
            s.multiply,
            s.rooting
        FROM subculture_bottles s
        LEFT JOIN mother_bottles m ON s.parent_id = m.id
        ORDER BY s.date_created DESC
    """)

    subcultures = cur.fetchall()
    conn.close()

    subculture_list = []
    for s in subcultures:
        multiply_count = s["num_plants"] if s["multiply"] == 1 else 0
        rooting_count  = s["num_plants"] if s["rooting"]  == 1 else 0

        subculture_list.append({
            "id": s["id"],
            "daughter_barcode": s["daughter_barcode"],
            "mother_barcode": s["mother_barcode"],
            "plant_code": s["plant_code"],
            "plant_name": s["plant_name"],
            "media_code": s["media_code"],
            "multiply_plants": multiply_count,
            "rooting_plants": rooting_count,
            "cycle": s["cycle"],
            "technician": s["technician"],
            "date_created": s["date_created"],
            "subculture_week": s["subculture_week"],
            "lamina_flow": LAMINA_MAP.get(s["lamina_flow"], s["lamina_flow"])
        })

    return render_template('view_subculture.html', subcultures=subculture_list)
# ------View subculture bottles end---------------------------------



@app.route('/discard_entry', methods=['GET', 'POST'])
def discard_entry():
    LAMINA_MAP = {"LF1": "A", "LF2": "B", "LF3": "C", "LF4": "D", "LF5": "E", "LF6": "F"}

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    if request.method == 'POST':
        barcode = request.form['barcode'].strip()
        reason = request.form['reason']
        technician = request.form['technician']
        date_discarded = date.today()

        #Optional form values (auto-filled from front-end)
        form_lamina = request.form.get('lamina_flow', '').strip()
        form_week = request.form.get('subculture_week', '').strip()
        form_week = int(form_week) if form_week.isdigit() else 0

        # --- Check if barcode already discarded ---
        cur.execute("SELECT * FROM discards WHERE barcode=?", (barcode,))
        if cur.fetchone():
            flash(f"⚠ This bottle ({barcode}) is already discarded!", "error")
            conn.close()
            return redirect(url_for('discard_entry'))

        # --- Check both tables for existing discard status ---
        for table in ["subculture_bottles", "mother_bottles"]:
            cur.execute(f"SELECT status FROM {table} WHERE barcode=?", (barcode,))
            row = cur.fetchone()
            if row and row["status"] == "Discarded":
                flash(f"⚠ This bottle ({barcode}) is already discarded!", "error")
                conn.close()
                return redirect(url_for('discard_entry'))

        # --- Try subculture_bottles first ---
        cur.execute("SELECT * FROM subculture_bottles WHERE barcode=?", (barcode,))
        sub = cur.fetchone()

        if sub:
            plant_code = sub["plant_code"]
            plant_name = sub["plant_name"]
            cycle = sub["cycle"]
            lamina_flow = (
            LAMINA_MAP.get(sub["lamina_flow"], sub["lamina_flow"])
                if sub["lamina_flow"] else form_lamina
                )

            subculture_technician = sub["technician"] or technician
            subculture_week = sub["subculture_week"] if sub["subculture_week"] else form_week
            parent_id = sub["id"]
            origin = sub["parent_id"]

            num_plants = sub["num_plants"]
            discard_type = "multiply" if sub["multiply"] == 1 else "rooting"

            # Update subculture bottle status
            cur.execute("UPDATE subculture_bottles SET status=? WHERE barcode=?", ("Discarded", barcode))

        else:
            # --- Try mother_bottles ---
            cur.execute("SELECT * FROM mother_bottles WHERE barcode=?", (barcode,))
            mother = cur.fetchone()
            if mother:
                plant_code = mother["plant_code"]
                plant_name = mother["plant_name"]
                cycle = mother["cycle"]
                lamina_flow = LAMINA_MAP.get(mother["lamina_flow"], mother["lamina_flow"]) or form_lamina
                subculture_technician = mother["technician"]
                subculture_week = mother["culture_week"] if "culture_week" in mother.keys() else form_week
                parent_id = mother["id"]
                origin = None

                num_plants = mother["num_plants"]
                discard_type = "mother"

                # Update mother bottle status
                cur.execute("UPDATE mother_bottles SET status=? WHERE barcode=?", ("Discarded", barcode))
            else:
                flash(f"⚠ Bottle {barcode} not found!", "error")
                conn.close()
                return redirect(url_for('discard_entry'))

        # --- Insert discard record ---
        try:
            cur.execute("""
                INSERT INTO discards ( 
                barcode, reason, num_bottles, technician, date_discarded,
                plant_code, plant_name, cycle, lamina_flow, subculture_technician,
             subculture_week, origin, parent_id, type
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                barcode, reason, num_plants, technician, date_discarded,
                plant_code, plant_name, cycle, lamina_flow, subculture_technician,
                subculture_week, origin, parent_id, discard_type
            ))

            conn.commit()
            flash(f"✅ Bottle {barcode} discarded successfully.", "success")

        except sqlite3.Error as e:
            flash(f"Error: {e}", "error")

        finally:
            conn.close()

        return redirect(url_for('discard_entry'))

    conn.close()
    return render_template("discard_entry.html")


@app.route('/get_discard_info/<barcode>')
def get_discard_info(barcode):
    LAMINA_MAP = {
        "LF1": "A", "LF2": "B", "LF3": "C",
        "LF4": "D", "LF5": "E", "LF6": "F"
    }

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # Check if barcode is already discarded
    cur.execute("SELECT * FROM discards WHERE barcode=?", (barcode,))
    discarded = cur.fetchone()
    if discarded:
        conn.close()
        return {"error": f"This bottle ({barcode}) is already discarded!"}

    # Try to fetch from subculture bottles
    cur.execute("SELECT * FROM subculture_bottles WHERE barcode=?", (barcode,))
    sub = cur.fetchone()
    if sub:
        conn.close()
        return {
            "plant_code": sub["plant_code"],
            "plant_name": sub["plant_name"],
            "cycle": sub["cycle"],
            "subculture_technician": sub["technician"],
            "lamina_flow": LAMINA_MAP.get(sub["lamina_flow"], sub["lamina_flow"]),
            "week_number": sub["subculture_week"]
        }

    # Otherwise fetch from mother bottles
    cur.execute("SELECT * FROM mother_bottles WHERE barcode=?", (barcode,))
    mother = cur.fetchone()
    conn.close()
    if not mother:
        return {"error": "Bottle not found"}

    # ✅ Fetch lamina_flow and culture_week from mother_bottles
    lamina_flow = LAMINA_MAP.get(mother["lamina_flow"], mother["lamina_flow"])
    week_number = mother["culture_week"] if mother["culture_week"] else 0

    return {
        "plant_code": mother["plant_code"],
        "plant_name": mother["plant_name"],
        "cycle": mother["cycle"],
        "subculture_technician": mother["technician"],
        "lamina_flow": lamina_flow,
        "week_number": week_number
    }

@app.route('/view_discards')
def view_discards():
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("SELECT * FROM discards ORDER BY date_discarded DESC")
    discards = [dict(row) for row in cur.fetchall()]
    conn.close()
    return render_template("view_discards.html", discards=discards)

  
@app.route("/view_rooted_plants")
def view_rooted_plants():
    if "username" not in session:
        return redirect(url_for("login"))

    conn = get_db_connection()
    cur = conn.cursor()

    rooted_plants = cur.execute("""
        SELECT 
            s.plant_code,
            s.plant_name AS variety,
            s.subculture_week,
            CAST((julianday('now') - julianday(s.date_created)) / 7 AS INTEGER) || ' weeks' AS age,
            CAST((julianday('now') - julianday(s.date_created)) / 7 AS INTEGER) AS week_number,
            SUM(s.num_plants) AS total_plants   -- ✅ sum plants per variety & week
        FROM subculture_bottles s
        WHERE s.status='Active'
          AND s.rooting = 1   -- ✅ only rooted stage bottles
        GROUP BY 
            s.plant_code, 
            s.plant_name, 
            s.subculture_week, 
            week_number
        ORDER BY week_number DESC;   -- oldest first
    """).fetchall()

    conn.close()

    return render_template("view_rooted_plants.html", rooted_plants=rooted_plants)


@app.route("/price_list", methods=["GET", "POST"])
def price_list():
    conn = get_db_connection()
    cur = conn.cursor()

    # Fetch all plants for dropdown
    plants = cur.execute("SELECT code, name FROM plants").fetchall()

    if request.method == "POST":
        plant_code = request.form.get("plant_code")
        variety_name = request.form.get("variety_name")
        price = request.form.get("price")
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        # Check if entry exists
        existing = cur.execute("SELECT id FROM price_list WHERE plant_code=?", (plant_code,)).fetchone()
        if existing:
            # Update
            cur.execute("""
                UPDATE price_list
                SET price=?, last_updated=?
                WHERE plant_code=?
            """, (price, now, plant_code))
        else:
            # Insert
            cur.execute("""
                INSERT INTO price_list (plant_code, variety_name, price, last_updated)
                VALUES (?, ?, ?, ?)
            """, (plant_code, variety_name, price, now))

        conn.commit()
        return redirect(url_for("price_list"))

    # Fetch current prices
    prices = cur.execute("SELECT * FROM price_list ORDER BY plant_code").fetchall()
    conn.close()
    return render_template("price_list.html", plants=plants, prices=prices)



# ---------------- POS – Sell Rooted Plants ----------------
@app.route("/pos_rooted", methods=["GET","POST"])
def pos_rooted():
    if "username" not in session:
        return redirect(url_for("login"))

    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    if request.method == "POST":
        data = request.get_json()
        items = data.get("items", [])
        payment_method = data.get("payment_method", "")
        save_only = data.get("save_only", False)

        if not items:
            return jsonify({"status":"error","message":"No items to save"}),400

        # ------------------- Validate quantities -------------------
        for item in items:
            plant_code = item['plant_code']
            
            # Ensure qty is integer and positive
            try:
                qty = int(float(item.get('qty', 0)))
            except:
                qty = 0

            # Fetch current available stock (only positive stock)
            cur.execute("""
                SELECT COALESCE(SUM(num_plants),0) AS available
                FROM subculture_bottles
                WHERE plant_code=? AND status='Active' AND rooting=1 AND num_plants > 0
            """, (plant_code,))
            available = cur.fetchone()["available"]

            if qty <= 0:
                return jsonify({
                    "status": "error",
                    "message": f"Quantity must be greater than 0 for {plant_code}."
                }), 400

            if qty > available:
                return jsonify({
                    "status": "error",
                    "message": f"Invalid quantity for {plant_code}. Available: {available}"
                }), 400

        # ------------------- Calculate total amount -------------------
        total_amount = sum(item['qty']*item['unit_price']*(1-item['discount']/100) for item in items)

        # ------------------- Insert invoice -------------------
        cur.execute("INSERT INTO pos_invoices (payment_method, total_amount) VALUES (?, ?)", 
                    (payment_method, total_amount))
        invoice_id = cur.lastrowid

        # ------------------- Insert items & update stock -------------------
        for item in items:
            total_price = item['qty']*item['unit_price']*(1-item['discount']/100)
            cur.execute("""
                INSERT INTO pos_invoice_items (invoice_id, plant_code, plant_name, quantity, unit_price, total_price)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (invoice_id, item['plant_code'], item['variety'], item['qty'], item['unit_price'], total_price))

            # Deduct stock (FIFO: reduce from first available bottles)
            remaining = item['qty']
            while remaining > 0:
                cur.execute("""
                    SELECT id, num_plants FROM subculture_bottles
                    WHERE plant_code=? AND status='Active' AND rooting=1 AND num_plants>0
                    ORDER BY date_created ASC
                    LIMIT 1
                """, (item['plant_code'],))
                bottle = cur.fetchone()
                if not bottle:
                    break

                deduct = min(remaining, bottle['num_plants'])
                cur.execute("UPDATE subculture_bottles SET num_plants=num_plants-? WHERE id=?",
                            (deduct, bottle['id']))
                remaining -= deduct

        conn.commit()
        conn.close()

        msg = "Invoice saved!"
        if not save_only:
            msg = "Invoice saved and ready to print!"

        return jsonify({"status":"success","message":msg,"invoice_id":invoice_id})

    # ---------------- GET request: show rooted plants ----------------
    rooted_plants = cur.execute("""
        SELECT plant_code, plant_name AS variety, SUM(num_plants) AS total_available
        FROM subculture_bottles
        WHERE status='Active' AND rooting=1 AND num_plants > 0
        GROUP BY plant_code, plant_name
    """).fetchall()

    # Fetch price list and create dict
    prices = cur.execute("SELECT plant_code, price FROM price_list").fetchall()
    price_dict = {row['plant_code']: row['price'] for row in prices}

    conn.close()
    return render_template("pos_rooted.html", rooted_plants=rooted_plants, price_dict=price_dict)

# ---------------- Scan Rooted Bottle ----------------
@app.route("/scan_rooted_bottle/<barcode>")
def scan_rooted_bottle(barcode):
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    cur.execute("""
        SELECT plant_code, plant_name, num_plants, status, rooting
        FROM subculture_bottles
        WHERE barcode = ?
    """, (barcode,))
    bottle = cur.fetchone()
    conn.close()

    if not bottle:
        return jsonify({"status": "error", "message": "Barcode not found!"})

    if bottle["status"] != "Active" or bottle["rooting"] != 1:
        return jsonify({"status": "error", "message": "Bottle not valid for sale!"})

    return jsonify({
        "status": "success",
        "plant_code": bottle["plant_code"],
        "plant_name": bottle["plant_name"],
        "num_plants": bottle["num_plants"]
    })




# ---------------- POS Transactions ----------------
@app.route("/pos_transactions")
def pos_transactions():
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    cur.execute("""
        SELECT i.id, ii.plant_code, ii.plant_name, ii.quantity, ii.unit_price, ii.total_price,
               i.payment_method, i.printed, i.created_at
        FROM pos_invoices i
        JOIN pos_invoice_items ii ON i.id=ii.invoice_id
        ORDER BY i.created_at DESC
    """)
    transactions = cur.fetchall()
    conn.close()

    return render_template("pos_transactions.html", transactions=transactions)


# ---------------- Print Invoice ----------------
@app.route("/print_invoice/<int:invoice_id>")
def print_invoice(invoice_id):
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    invoice = cur.execute("SELECT * FROM pos_invoices WHERE id=?", (invoice_id,)).fetchone()
    items = cur.execute("SELECT * FROM pos_invoice_items WHERE invoice_id=?", (invoice_id,)).fetchall()

    cur.execute("UPDATE pos_invoices SET printed=1 WHERE id=?", (invoice_id,))
    conn.commit()
    conn.close()

    return render_template("print_invoice.html", invoice=invoice, items=items)

# ---------------- View All Invoices ----------------
@app.route('/invoices')
def invoices():
    conn = get_db_connection()
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    cur.execute("""
        SELECT id, customer_name, total_amount, payment_method, created_at
        FROM invoices
        ORDER BY id DESC
    """)
    invoices = cur.fetchall()
    conn.close()

    return render_template('view_invoices.html', invoices=invoices)

@app.route('/view_invoices')
def view_invoices():
    conn = get_db_connection()
    cur = conn.cursor()
    invoices = cur.execute("""
        SELECT 
            id,
            payment_method,
            total_amount,
            created_at
        FROM pos_invoices
        ORDER BY created_at DESC
    """).fetchall()
    conn.close()
    return render_template('view_invoices.html', invoices=invoices)

@app.route("/discard_stats_dashboard", methods=["GET", "POST"])
def discard_stats_dashboard():
    conn = get_db_connection()
    cur = conn.cursor()

     #Default filter values
    start_date = request.form.get("start_date", "")
    end_date = request.form.get("end_date", "")
    technician_filter = request.form.get("technician", "")
    lamina_filter = request.form.get("lamina_flow", "")

     #Build WHERE conditions
    conditions = []
    params = []

    if start_date:
        conditions.append("date_discarded >= ?")
        params.append(start_date)
    if end_date:
        conditions.append("technician = ?")
        params.append(technician_filter)
    if lamina_filter:
        conditions.append("lamina_flow = ?")
        params.append(lamina_filter)

    where_clause = "WHERE " + " AND ".join(conditions) if conditions else ""

    # Get total discarded by grouping
    cur.execute(f"""
        SELECT
            plant_name,
            SUM(num_bottles) as discarded
        FROM discards 
        {where_clause}
        GROUP BY plant_name
        Y discarded DESC
    """, params)
    discarded_data = cur.fetchall()

     #Get total subcultured for the same filters
    total_query = "SELECT SUM(num_plants) as total FROM subculture_bottles"
    total_params = []

    sub_conditions = []
    if start_date:
        sub_conditions.append("date_created >= ?")
        total_params.append(start_date)
    if end_date:
        sub_conditions.append("date_created <= ?")
        total_params.append(end_date)
    if technician_filter:
        sub_conditions.append("technician = ?")
        total_params.append(technician_filter)
    if lamina_filter:
        sub_conditions.append("lamina_flow = ?")
        total_params.append(lamina_filter)
    sub_where_clause = " WHERE " + " AND ".join(sub_conditions) if sub_conditions else ""
    cur.execute(total_query + sub_where_clause, total_params)
    total_row = cur.fetchone()
    total_subcultured = total_row["total"] or 0

    # Calculate percentage for each plant
    results = []
    for row in discarded_data:
        percent = (row["discarded"] / total_subcultured * 100) if total_subcultured else 0
        results.append({
            "plant_name": row["plant_name"],
            "discarded": row["discarded"],
            "total_subcultured": total_subcultured,
            "discard_percent": round(percent, 2)
        })

    # Get distinct technicians and lamina flows for filter dropdowns
    cur.execute("SELECT DISTINCT technician FROM subculture_bottles")
    technicians = [r["technician"] for r in cur.fetchall()]

    cur.execute("SELECT DISTINCT lamina_flow FROM subculture_bottles")
    lamina_flows = [r["lamina_flow"] for r in cur.fetchall()]

    conn.close()

    return render_template(
        results=results,
        technicians=technicians,
        lamina_flows=lamina_flows,
        start_date=start_date,
        end_date=end_date,
        technician_filter=technician_filter,
        lamina_filter=lamina_filter
    )

@app.route('/discard_stats')
def discard_stats():
    conn = get_db_connection()
    conn.row_factory = lambda cursor, row: row
    cur = conn.cursor()

    # Total discards
    cur.execute("SELECT SUM(num_bottles) FROM discards")
    total_discards = cur.fetchone()[0] or 1  # avoid division by zero

    # --- By Plant ---
    by_plant_raw = cur.fetchall()
    by_plant = [{"label": p[0], "count": p[1], "percent": round(p[1]/total_discards*100,2)} for p in by_plant_raw]

    # --- By Cycle ---
    cur.execute("SELECT cycle, SUM(num_bottles) FROM discards GROUP BY cycle")
    by_cycle_raw = cur.fetchall()
    by_cycle = [{"label": c[0], "count": c[1], "percent": round(c[1]/total_discards*100,2)} for c in by_cycle_raw]

    # --- By Technician ---
    cur.execute("SELECT subculture_technician, SUM(num_bottles) FROM discards GROUP BY subculture_technician")
    by_technician_raw = cur.fetchall()
    by_technician = [{"label": t[0], "count": t[1], "percent": round(t[1]/total_discards*100,2)} for t in by_technician_raw]

    # --- By Lamina Flow ---
    cur.execute("SELECT lamina_flow, SUM(num_bottles) FROM discards GROUP BY lamina_flow")
    by_lamina_raw = cur.fetchall()
    by_lamina = [{"label": l[0], "count": l[1], "percent": round(l[1]/total_discards*100,2)} for l in by_lamina_raw]

    # --- By Subculture Week ---
    by_week_raw = cur.fetchall()
    by_week = [{"label": w[0], "count": w[1], "percent": round(w[1]/total_discards*100,2)} for w in by_week_raw]

    return render_template(
        'discard_stats.html',
        by_plant=by_plant,
        by_cycle=by_cycle,
        by_technician=by_technician,
        by_lamina=by_lamina,
        by_week=by_week
    )



# ----------------- Route to edit media compositions ----------
@app.route('/edit_media/<media_code>', methods=['GET', 'POST'])
def edit_media(media_code):
    conn = get_db_connection()
    
    media_row = conn.execute("SELECT * FROM media_compositions WHERE media_code=?", (media_code,)).fetchone()
    if not media_row:
        conn.close()
        flash("Media not found!", "danger")
        return redirect(url_for('register_media_compositions'))

    media = dict(media_row)

    # Get all hormones for this media_code
    hormones = conn.execute("SELECT * FROM media_hormones WHERE media_code=?", (media_code,)).fetchall()
    hormones = [dict(h) for h in hormones]

    # ------------------- ADD THIS FIX -------------------
    # Normalize hormone names to remove extra spaces or hidden chars
    for h in hormones:
        if h['hormone_name']:
            h['hormone_name'] = h['hormone_name'].strip()
    # ------------------------------------------------------

    hormone_list = [
        "BAP","NAA","2,4-D","KIN","IBA","IAA","TDZ","Picloram",
        "2IP","GA3","Adenine sulfate","Coconut water","Ascorbic A."
    ]

    if request.method == 'POST':
        basal_media = request.form['basal_media']

        # Clear old hormones
        conn.execute("DELETE FROM media_hormones WHERE media_code=?", (media_code,))

        # Insert all hormone fields dynamically
        index = 1
        while True:
            hormone_name = request.form.get(f'hormone{index}')
            hormone_mg = request.form.get(f'hormone{index}_mg', 0)
            if hormone_name is None:
                break
            if hormone_name.strip():  # Only save if a hormone is selected
                try:
                    mg_value = float(hormone_mg)
                except:
                    mg_value = 0
                conn.execute(
                    "INSERT INTO media_hormones (media_code, hormone_name, hormone_mg) VALUES (?,?,?)",
                    (media_code, hormone_name.strip(), mg_value)  # also strip before saving
                )
            index += 1

        conn.execute("UPDATE media_compositions SET basal_media=? WHERE media_code=?", (basal_media, media_code))
        conn.commit()
        conn.close()
        #flash(f"Media {media_code} updated successfully!", "success")
        return redirect(url_for('register_media_compositions'))

    conn.close()
    return render_template('edit_media_compositions.html', media=media, hormones=hormones, hormone_list=hormone_list)

@app.route('/delete_media/<media_code>')
def delete_media(media_code):
    conn = get_db_connection()

    # Delete related hormones first
    conn.execute("DELETE FROM media_hormones WHERE media_code=?", (media_code,))
    # Delete the media composition
    conn.execute("DELETE FROM media_compositions WHERE media_code=?", (media_code,))
    
    conn.commit()
    conn.close()
    
    #flash(f"Media {media_code} deleted successfully!", "success")
    return redirect(url_for('register_media_compositions'))


@app.route("/")
def home():
    return redirect(url_for("login"))





#if __name__ == "__main__":
    #app.run(debug=True, host="127.0.0.1", port=5000)
if __name__ == "__main__":
    app.run(debug=True)