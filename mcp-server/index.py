import sys
import os

# Ensure mcp-server/ is on the path so db/ and tools/ are importable
sys.path.insert(0, os.path.dirname(__file__))

from mcp.server.fastmcp import FastMCP
from tools.list_tables import list_tables as _list_tables
from tools.query_database import query_database as _query_database

mcp = FastMCP('datathon-db')


@mcp.tool(description='List all tables in the database with their columns and types.')
def list_tables() -> list:
    return _list_tables()


@mcp.tool(description='Execute a read-only SELECT query against the database. Non-SELECT queries are rejected.')
def query_database(sql: str) -> dict:
    return _query_database(sql)


if __name__ == '__main__':
    mcp.run(transport='stdio')
