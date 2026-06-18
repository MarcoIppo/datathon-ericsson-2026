import os
from db.connection import get_connection


def list_tables() -> list[dict]:
    """Return list of tables with their columns from the database."""
    conn = get_connection()
    try:
        cursor = conn.cursor()
        profile = os.environ.get('DB_PROFILE', 'h2').lower()

        if profile == 'postgres':
            cursor.execute("""
                SELECT c.table_name, c.column_name, c.data_type
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON c.table_name = t.table_name AND c.table_schema = t.table_schema
                WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                ORDER BY c.table_name, c.ordinal_position
            """)
        else:
            cursor.execute("""
                SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS c
                JOIN INFORMATION_SCHEMA.TABLES t ON c.TABLE_NAME = t.TABLE_NAME
                WHERE t.TABLE_SCHEMA = 'PUBLIC' AND t.TABLE_TYPE = 'BASE TABLE'
                ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION
            """)

        rows = cursor.fetchall()
        tables: dict[str, list] = {}
        for row in rows:
            table, col, dtype = str(row[0]), str(row[1]), str(row[2])
            tables.setdefault(table, []).append({'name': col, 'type': dtype})

        return [{'table_name': t, 'columns': cols} for t, cols in tables.items()]
    finally:
        conn.close()
