import os
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv

load_dotenv()


def get_connection():
    """Return a DB connection based on DB_PROFILE env var (h2 | postgres)."""
    profile = os.environ.get('DB_PROFILE', 'h2').lower()
    if profile == 'postgres':
        return psycopg2.connect(
            host=os.environ['DB_HOST'],
            port=int(os.environ.get('DB_PORT', 5432)),
            dbname=os.environ['DB_NAME'],
            user=os.environ['DB_USERNAME'],
            password=os.environ['DB_PASSWORD'],
        )
    # H2 via JDBC
    import jpype
    import jaydebeapi
    jvm_path = os.environ.get('JAVA_JVM_PATH', '/usr/lib/jvm/java-17-openjdk-amd64/lib/server/libjvm.so')
    jar = os.environ.get('H2_JAR_PATH', os.path.join(os.path.dirname(__file__), '..', 'h2.jar'))
    if not jpype.isJVMStarted():
        jpype.startJVM(jvm_path, classpath=[jar])
    db_path = os.environ.get('H2_DB_PATH', '../ericsson_challenge-dev/data/datathon_user_db')
    url = f'jdbc:h2:file:{db_path};AUTO_SERVER=TRUE'
    return jaydebeapi.connect(
        'org.h2.Driver', url,
        [os.environ.get('DB_USERNAME', 'admin'), os.environ.get('DB_PASSWORD', '')],
    )
