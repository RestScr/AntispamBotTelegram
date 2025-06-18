import asyncio
import json
import uuid
import logging
import subprocess
from collections import defaultdict
from typing import Dict, List, Tuple
from pathlib import Path
import sys

import requests
import urllib3
from telethon import TelegramClient
from telethon.tl.functions.messages import GetHistoryRequest
from requests.adapters import HTTPAdapter
import ssl


# ── Логирование ───────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler()]
)
log = logging.getLogger(__name__)
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ── Telegram creds ────────────────────────────────────────
BOT_SESSION = ""
API_ID = 0
API_HASH = ""
client = TelegramClient(BOT_SESSION, API_ID, API_HASH)

# ── GigaChat creds ────────────────────────────────────────
GIGA_CLIENT_ID = ""
GIGA_BASIC = (
    ""
)
SCOPE = ""
AUTH_URL = ""
CHAT_URL = ""

# ── HTTPS адаптер для TLS ────────────────────────────────
class TLSAdapter(HTTPAdapter):
    def init_poolmanager(self, *args, **kwargs):
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        ctx.set_ciphers("DEFAULT@SECLEVEL=1")
        kwargs["ssl_context"] = ctx
        return super().init_poolmanager(*args, **kwargs)

session = requests.Session()
session.mount("https://", TLSAdapter())

# ── Запуск третьего бота после завершения ────────────────
PROJECT_DIR = Path(__file__).resolve().parent
THIRD_PATH = PROJECT_DIR / "third_bot.py"  # ← имя третьего скрипта
CMD_THIRD = [sys.executable, str(THIRD_PATH)]
print("THIRD_PATH =", THIRD_PATH, THIRD_PATH.exists())


def launch_third():
    try:
        subprocess.Popen(CMD_THIRD)
        log.info("⭢ third_bot запущен: %s", " ".join(CMD_THIRD))
    except Exception as e:
        log.error("Не смог запустить third_bot: %s", e)

# ── GigaChat auth and query ──────────────────────────────
def get_token() -> str:
    log.info("Запрос токена GigaChat…")
    r = session.post(
        AUTH_URL,
        headers={
            "Authorization": f"Basic {GIGA_BASIC}",
            "Content-Type": "application/x-www-form-urlencoded",
            "RqUID": str(uuid.uuid4())
        },
        data={"grant_type": "client_credentials", "scope": SCOPE},
        timeout=15,
        verify=False
    )
    r.raise_for_status()
    log.info("Токен получен.")
    return r.json()["access_token"]

def ask_gigachat(prompt: str, context: str, token: str) -> str:
    log.info("Формирую запрос к GigaChat…")
    log.debug("PROMPT:\n%s", prompt)
    log.debug("CONTEXT:\n%s", context)

    body = {
        "model": "GigaChat",
        "messages": [
            {"role": "system", "content": context},
            {"role": "user", "content": prompt}
        ],
        "max_tokens": 2048,
        "temperature": 0.2
    }

    try:
        r = session.post(
            CHAT_URL,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "X-Client-ID": GIGA_CLIENT_ID,
                "RqUID": str(uuid.uuid4())
            },
            json=body,
            timeout=30,
            verify=False
        )
        r.raise_for_status()
        log.info("Ответ от GigaChat получен.")
        log.debug("Сырой ответ GigaChat:\n%s", r.text)
        return r.json()["choices"][0]["message"]["content"]
    except requests.exceptions.SSLError as e:
        log.error("SSL-ошибка: %s", e)
        return '[{"user":"@example","reason":"SPAM","date":"2025-01-01T00:00"}]'
    except Exception as e:
        log.error("Ошибка запроса: %s", e)
        return '[{"user":"@example","reason":"SPAM","date":"2025-01-01T00:00"}]'

# ── Работа с локальными данными ──────────────────────────
def load_participants(path: str = "channel_users.json") -> Dict[int, str]:
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return {int(u["id"]): f'@{u["username"]}' if u.get("username") else f'id{u["id"]}' for u in data}
    except Exception as e:
        log.error("Не удалось прочитать %s: %s", path, e)
        return {}

async def fetch_last_msgs(entity, per_user=10) -> Dict[int, List[Tuple[str, str]]]:
    user_msgs: Dict[int, List[Tuple[str, str]]] = defaultdict(list)
    offset = 0
    limit = 100
    while True:
        hist = await client(GetHistoryRequest(entity, offset, None, 0, limit, 0, 0, 0))
        if not hist.messages:
            break
        for m in hist.messages:
            if m.from_id and m.message:
                uid = getattr(m.from_id, "user_id", m.from_id)
                if len(user_msgs[uid]) < per_user:
                    user_msgs[uid].append((m.message, m.date.isoformat()))
        offset = hist.messages[-1].id
    log.info("Собрано сообщений от %d пользователей.", len(user_msgs))
    return user_msgs

# ── Основной запуск ───────────────────────────────────────
async def main():
    await client.start()
    log.info("Клиент Telegram запущен.")

    entity = await client.get_entity("https://t.me/+l4RmCWI81to3N2Ey")
    uname_map = load_participants()
    if not uname_map:
        log.error("Участники не загружены, выходим.")
        return

    msgs = await fetch_last_msgs(entity)

    payload = [
        {"user": uname_map.get(uid, f"id{uid}"), "messages": [m for m, _ in lst]}
        for uid, lst in msgs.items()
    ]
    prompt = json.dumps(payload, ensure_ascii=False, indent=2)

    context = (
        "Ты система модерации. На входе JSON-массив объектов вида "
        "{user, messages:[…]}. Для каждого пользователя проанализируй сообщения "
        "и username и верни список JSON-объектов вида "
        '{"user":"@name","reason":"LINK|LLM|FLOOD|SPAM","date":"YYYY-MM-DDTHH:MM"}. '
        "Можешь вернуть несколько записей для одного пользователя, если разные даты. "
        "Никаких пояснений, только JSON-массив."
    )

    token = await asyncio.to_thread(get_token)
    raw_answer = await asyncio.to_thread(ask_gigachat, prompt, context, token)

    try:
        violations = json.loads(raw_answer)
        log.info("Ответ успешно распарсен. Кол-во нарушений: %d", len(violations))
    except json.JSONDecodeError:
        log.warning("Не удалось распарсить JSON. Ответ сохранён как строка.")
        violations = raw_answer

    with open("violations.json", "w", encoding="utf-8") as f:
        if isinstance(violations, list):
            json.dump(violations, f, ensure_ascii=False, indent=2)
        else:
            f.write(raw_answer)
    log.info("Файл violations.json записан.")

    # ⭢ Запуск третьего файла
    launch_third()

if __name__ == "__main__":
    asyncio.run(main())
