import hashlib

MYSQL_PASSWORD = ""
MYSQL_USER = ""
MYSQL_DATABASE = ""
MYSQL_HOST = ""
DEBUG = False
BOT_TOKEN = ""
BOT_TOKEN_SECRET = hashlib.sha256(BOT_TOKEN.encode()).digest()
REQUIRED_USER_TELEGRAM_VERIFICATION_ARGUMENTS = set(
    [
        "status",
        "user-id",
        "user-first-name",
        "user-last-name",
        "user-name",
        "language-code",
        "auth-date",
        "hash"
    ]
)
REQUIRED_USER_INFO_ARGUMENTS = set(
    [
        "tg-id"
    ]
)
