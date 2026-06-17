"""
DB connection factory for the MCP server.
Reads DB_PROFILE from environment to select H2 (jaydebeapi) or PostgreSQL (psycopg2).
Credentials are never hardcoded — loaded from environment / .env file.
"""
import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env from project root (two levels up from mcp-server/)
_root = Path(__file__).parent.parent.parent
load_dotenv(_root / '.env', override=False)
load_dotenv(_root / 'ericsson_challenge-dev' / '.env', override=False)


def get_connection():
    """
    Return an open DB connection based on DB_PROFILE env var.
    H2  → jaydebeapi JDBC connection to the file-based H2 database.
    postgres → psycopg2 connection to PostgreSQL.
    """
    profile = os.environ.get('DB_PROFILE', 'h2').lower()

    if profile == 'h2':
        import jaydebeapi
        db_path = os.environ.get(
            'H2_DB_PATH',
            str(_root / 'ericsson_challenge-dev' / 'data' / 'datathon_user_db')
        )
        jar = os.environ.get('H2_JAR_PATH', str(Path(__file__).parent.parent / 'h2.jar'))
        url = f'jdbc:h2:file:{db_path};AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE'
        return jaydebeapi.connect(
            'org.h2.Driver', url,
            [os.environ.get('DB_USERNAME', 'admin'), os.environ.get('DB_PASSWORD', '')],
            jar
        )

    if profile == 'postgres':
        import psycopg2
        return psycopg2.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            port=int(os.environ.get('DB_PORT', 5432)),
            dbname=os.environ.get('DB_NAME', 'datathon_db'),
            user=os.environ.get('DB_USERNAME', 'datathon_user'),
            password=os.environ.get('DB_PASSWORD', 'datathon_pass'),
        )

    raise ValueError(f'Unsupported DB_PROFILE: {profile}. Use "h2" or "postgres".')
