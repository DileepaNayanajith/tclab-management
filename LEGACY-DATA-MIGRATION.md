# Legacy data migration

Source: `naturalfoliagelab 4.zip` → `tclab.db` (SQLite)

## Audited records

| Legacy table | Rows | Migration target |
|---|---:|---|
| plants | 2 | `Plant` |
| media_compositions | 2 | `MediaComposition` |
| media_hormones | 4 | Combined into the media `hormones` field |
| mother_bottles | 1 | `MotherBottle` |
| subculture_bottles | 9 | `Subculture` |
| price_list | 2 | Deferred until the Price List feature is implemented |
| users | 3 | Not imported; passwords and legacy access rules are not copied |
| discards, taken_out, sales, invoices, transactions | 0 | Nothing to import |

## Mapping decisions

- Legacy `Active`, `Used`, and `Discarded` values map to the Spring enum values
  `ACTIVE`, `USED`, and `DISCARDED`.
- Legacy dates are retained so six-week dashboard warnings remain accurate.
- `rooting = 1` maps to the `rooting` boolean; other active cultures are counted as
  multiplication cultures.
- The legacy mother bottle has zero remaining plants because its status is `Used`.
  Historical zero quantities are preserved rather than changed.
- Importing is idempotent: barcodes/codes already present in the target database are
  skipped, so restarting the application does not duplicate records.
- The uploaded SQLite file and credentials are not committed to Git.
