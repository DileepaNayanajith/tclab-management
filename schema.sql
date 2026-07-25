CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        full_name TEXT,
        role TEXT NOT NULL
, can_subculture INTEGER DEFAULT 0, can_discard INTEGER DEFAULT 0, can_dashboard INTEGER DEFAULT 0, can_register_media INTEGER DEFAULT 0, can_register_mother_plants INTEGER DEFAULT 0, can_price_list INTEGER DEFAULT 0, can_pos_rooted INTEGER DEFAULT 0);
CREATE TABLE sqlite_sequence(name,seq);
CREATE TABLE mother_bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT UNIQUE NOT NULL,
    plant_code TEXT NOT NULL,
    plant_name TEXT NOT NULL,
    media_code TEXT NOT NULL,
    num_plants INTEGER NOT NULL,
    cycle INTEGER NOT NULL,
    technician TEXT NOT NULL,      
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
, printed INTEGER, status TEXT, culture_week INTEGER, lamina_flow TEXT);
CREATE TABLE subculture_bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT UNIQUE NOT NULL,
    plant_code TEXT NOT NULL,
    plant_name TEXT NOT NULL,
    media_code TEXT NOT NULL,
    num_plants INTEGER NOT NULL,
    cycle INTEGER NOT NULL,
    technician TEXT,
    parent_id INTEGER,  -- References the parent bottle if this is a subculture
    status TEXT DEFAULT 'Active',
    origin TEXT DEFAULT 'New', -- 'New' for initial mother bottle, 'Subculture' for derived bottles
    date_created DATE DEFAULT (DATE('now')), multiply INTEGER DEFAULT 0, rooting INTEGER DEFAULT 0, subculture_week INTEGER, lamina_flow TEXT,
    FOREIGN KEY(parent_id) REFERENCES subculture_bottles(id)
);
CREATE TABLE plants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT,
    name TEXT,
    variety TEXT,
    description TEXT
);
CREATE TABLE media (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    media_code TEXT,
    basal_media TEXT,
    hormones TEXT,
    ph REAL,
    agar REAL,
    date_prepared TEXT,
    technician TEXT
);
CREATE TABLE media_compositions (
    media_code TEXT PRIMARY KEY,
    basal_media TEXT
);
CREATE TABLE media_hormones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    media_code TEXT,
    hormone_name TEXT,
    hormone_mg REAL
);
CREATE TABLE bottles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT,
    media_code TEXT,
    basal_media TEXT,
    hormones TEXT,
    ph TEXT,
    agar TEXT,
    date_prepared TEXT,
    technician TEXT,
    status TEXT,
    cycle INTEGER,
    mother_barcode TEXT,
    plant_code TEXT,
    variety TEXT
);
CREATE TABLE discards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT NOT NULL,
    reason TEXT NOT NULL,
    num_bottles INTEGER NOT NULL,
    technician TEXT,
    date_discarded DATE NOT NULL
, plant_code TEXT, plant_name TEXT, cycle INTEGER, lamina_flow TEXT, subculture_week INTEGER, origin TEXT, parent_id INTEGER, subculture_technician TEXT, type TEXT);
CREATE TABLE taken_out (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subculture_id INTEGER NOT NULL,
    plant_code TEXT NOT NULL,
    plant_name TEXT NOT NULL,
    num_plants INTEGER NOT NULL,
    type TEXT NOT NULL,
    date_taken DATE NOT NULL,
    notes TEXT,
    FOREIGN KEY(subculture_id) REFERENCES subculture_bottles(id)
);
CREATE TABLE sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    variety TEXT,
    subculture_week TEXT,
    qty_sold INTEGER,
    sale_date TEXT
);
CREATE TABLE price_list (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    variety_name TEXT,
    price REAL,
    last_updated TIMESTAMP
);
CREATE TABLE pos_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plant_code TEXT,
    plant_name TEXT,
    quantity REAL,
    unit_price REAL,
    total_price REAL,
    payment_method TEXT,
    printed INTEGER,
    created_at TIMESTAMP
);
CREATE TABLE pos_invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_method TEXT,
    total_amount REAL,
    printed INTEGER,
    created_at TIMESTAMP,
    customer_name TEXT
);
CREATE TABLE pos_invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER,
    plant_code TEXT,
    plant_name TEXT,
    quantity REAL,
    unit_price REAL,
    total_price REAL
);
