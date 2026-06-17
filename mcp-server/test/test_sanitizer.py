"""
Unit tests for db/sanitizer.py — no DB connection required.
Tests cover READ-ONLY enforcement and sensitive field masking.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import pytest
from db.sanitizer import validate_query, mask_sensitive_fields


class TestValidateQuery:
    """validate_query() must block all non-SELECT statements."""

    def test_block_delete_query(self):
        """DELETE should raise ValueError."""
        with pytest.raises(ValueError, match='Only SELECT'):
            validate_query('DELETE FROM users')

    def test_block_drop_query(self):
        """DROP TABLE should raise ValueError."""
        with pytest.raises(ValueError, match='Only SELECT'):
            validate_query('DROP TABLE users')

    def test_block_insert_query(self):
        """INSERT should raise ValueError."""
        with pytest.raises(ValueError, match='Only SELECT'):
            validate_query("INSERT INTO users (email) VALUES ('x@y.com')")

    def test_block_update_query(self):
        """UPDATE should raise ValueError."""
        with pytest.raises(ValueError, match='Only SELECT'):
            validate_query("UPDATE users SET email='a@b.com' WHERE id=1")

    def test_block_truncate_query(self):
        """TRUNCATE should raise ValueError."""
        with pytest.raises(ValueError, match='Only SELECT'):
            validate_query('TRUNCATE TABLE users')

    def test_allow_simple_select(self):
        """Plain SELECT should pass without exception."""
        validate_query('SELECT * FROM users')  # no exception

    def test_allow_select_with_leading_whitespace(self):
        """SELECT with leading whitespace should pass."""
        validate_query('   SELECT COUNT(*) FROM users')

    def test_allow_select_case_insensitive(self):
        """select (lowercase) should pass."""
        validate_query('select id from users')


class TestMaskSensitiveFields:
    """mask_sensitive_fields() must redact known sensitive keys."""

    def test_mask_password_field(self):
        """password field must be replaced with [REDACTED]."""
        rows = [{'id': 1, 'email': 'a@b.com', 'password': '$2a$hash'}]
        result = mask_sensitive_fields(rows)
        assert result[0]['password'] == '[REDACTED]'

    def test_mask_access_token(self):
        """access_token must be redacted."""
        rows = [{'access_token': 'jwt.token.here'}]
        assert mask_sensitive_fields(rows)[0]['access_token'] == '[REDACTED]'

    def test_mask_refresh_token(self):
        """refresh_token must be redacted."""
        rows = [{'refresh_token': 'some-token'}]
        assert mask_sensitive_fields(rows)[0]['refresh_token'] == '[REDACTED]'

    def test_mask_authentication_token(self):
        """authentication_token must be redacted."""
        rows = [{'authentication_token': 'eggup-token'}]
        assert mask_sensitive_fields(rows)[0]['authentication_token'] == '[REDACTED]'

    def test_non_sensitive_fields_unchanged(self):
        """Non-sensitive fields must not be modified."""
        rows = [{'id': 1, 'email': 'a@b.com', 'first_name': 'Mario'}]
        result = mask_sensitive_fields(rows)
        assert result[0] == {'id': 1, 'email': 'a@b.com', 'first_name': 'Mario'}

    def test_empty_rows(self):
        """Empty list should return empty list."""
        assert mask_sensitive_fields([]) == []
