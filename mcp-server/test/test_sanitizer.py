"""
Unit tests for db/sanitizer.py.
No DB connection required — purely functional.
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import pytest
from db.sanitizer import validate_query, mask_sensitive_fields


def test_block_delete_query():
    """DELETE query must raise ValueError."""
    with pytest.raises(ValueError, match='Only SELECT queries are allowed'):
        validate_query('DELETE FROM users')


def test_block_drop_query():
    """DROP TABLE must raise ValueError."""
    with pytest.raises(ValueError, match='Only SELECT queries are allowed'):
        validate_query('DROP TABLE users')


def test_block_insert_query():
    """INSERT must raise ValueError."""
    with pytest.raises(ValueError, match='Only SELECT queries are allowed'):
        validate_query("INSERT INTO users(email) VALUES('x')")


def test_block_update_query():
    """UPDATE must raise ValueError."""
    with pytest.raises(ValueError, match='Only SELECT queries are allowed'):
        validate_query('UPDATE users SET email=\'x\' WHERE id=1')


def test_allow_select_query():
    """SELECT must not raise."""
    validate_query('SELECT * FROM users')  # no exception


def test_allow_select_with_leading_whitespace():
    """SELECT with leading whitespace must be allowed."""
    validate_query('  SELECT COUNT(*) FROM users')


def test_mask_password_field():
    """'password' field must be replaced with [REDACTED]."""
    result = mask_sensitive_fields([{'password': 'hash123', 'email': 'a@b.com'}])
    assert result[0]['password'] == '[REDACTED]'
    assert result[0]['email'] == 'a@b.com'


def test_mask_token_fields():
    """'access_token' and 'refresh_token' must be redacted."""
    row = {'access_token': 'jwt', 'refresh_token': 'rt', 'id': 1}
    result = mask_sensitive_fields([row])
    assert result[0]['access_token'] == '[REDACTED]'
    assert result[0]['refresh_token'] == '[REDACTED]'
    assert result[0]['id'] == 1


def test_non_sensitive_fields_unchanged():
    """Non-sensitive fields must pass through unchanged."""
    row = {'id': 42, 'email': 'test@test.com', 'name': 'Alice'}
    result = mask_sensitive_fields([row])
    assert result[0] == row
