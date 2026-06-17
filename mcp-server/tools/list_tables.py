"""
MCP tool: list_tables
Returns all user tables with their column names and types.
Compatible with H2 (schema 'PUBLIC') and PostgreSQL (schema 'public').
"""
import os
from db.connection import get_connection


def list_tables() -> list[dict]:
    """
    Query INFORMATION_SCHEMA to return table names and column definitions.
    Returns a list of { table_name, columns: [{name, type}] }.
    """
    profile = os.environ.get('DB_PROFILE', 'h2').lower()
    schema = 'PUBLIC' if profile == 'h2' else 'public'

    conn = get_connection()
    try:
        cursor = conn.cursor()

        cursor.execute(
            "SELECT table_name FROM information_schema.tables "
            "WHERE table_schema = %s ORDER BY table_name" if profile == 'postgres'
            else "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                 "WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME",
            (schema,)
        )
        tables = [row[0] for row in cursor.fetchall()]

        result = []
        for table in tables:
            cursor.execute(
                "SELECT column_name, data_type FROM information_schema.columns "
                "WHERE table_schema = %s AND table_name = %s ORDER BY ordinal_position"
                if profile == 'postgres'
                else "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                (schema, table)
            )
            columns = [{'name': r[0], 'type': r[1]} for r in cursor.fetchall()]
            result.append({'table_name': table, 'columns': columns})

        return result
    finally:
        conn.close()
