import hashlib

MYSQL_PASSWORD = "grGLPug5dl37"
MYSQL_USER = "kosfaton_hakaton"
MYSQL_DATABASE = "kosfaton_hakaton"
MYSQL_HOST = "kosfaton.beget.tech"
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
