"""
MCP Server — Datathon DB (UC-MCP-004)
Exposes list_tables and query_database tools via stdio transport.

Governance: READ-ONLY, human-in-the-loop required for every query.
See .kiro/specs/UC-MCP-004/guardrails.md for full policy.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from mcp.server.fastmcp import FastMCP
from tools.list_tables import list_tables as _list_tables
from tools.query_database import query_database as _query_database

mcp = FastMCP('datathon-db')


@mcp.tool()
def list_tables() -> list:
    """List all tables in the database with their column names and types."""
    return _list_tables()


@mcp.tool()
def query_database(sql: str) -> dict:
    """
    Execute a READ-ONLY SELECT query on the database.
    INSERT, UPDATE, DELETE, DROP and other write operations are blocked.
    Sensitive fields (password, tokens) are masked in the response.
    """
    return _query_database(sql)


if __name__ == '__main__':
    mcp.run(transport='stdio')
