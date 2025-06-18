import asyncio, json, logging, subprocess, threading, sys
from collections import deque, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List

from flask import Flask, request, jsonify
from flask_cors import CORS
from telethon import TelegramClient
from telethon.tl.types import Channel, Chat, ChannelParticipantsSearch
from telethon.tl.functions.channels import GetParticipantsRequest
from telethon.tl.functions.messages import GetHistoryRequest

API_ID = 0
API_HASH = ""
TG_USERNAME = ""          # ← уникальное имя сессии
client = TelegramClient(TG_USERNAME, API_ID, API_HASH)

PROJECT_DIR = Path(__file__).resolve().parent
BOT_PATH = PROJECT_DIR / "bot.py"
CALL_CMD = [sys.executable, str(BOT_PATH)]

app = Flask(__name__)
queue = deque()

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

@app.route("/submit_link", methods=["POST"])
def submit_link():
    link = (request.get_json(silent=True) or {}).get("link")
    if not link:
        return jsonify({"error": "No link provided"}), 400
    queue.append(link)
    log.info("Ссылка %s добавлена в очередь (%d)", link, len(queue))
    return jsonify({"status": "accepted", "link": link})

async def dump_all_participants(channel):
    users: List = []
    if isinstance(channel, Channel):
        offset = 0
        while True:
            part = await client(GetParticipantsRequest(channel, ChannelParticipantsSearch(""), offset, 100, 0))
            if not part.users:
                break
            users.extend(part.users)
            offset += len(part.users)
    elif isinstance(channel, Chat):
        users.extend(await client.get_participants(channel))

    with open("channel_users.json", "w", encoding="utf-8") as f:
        json.dump(
            [{
                "id": u.id,
                "first_name": u.first_name,
                "last_name": u.last_name,
                "username": u.username,
                "phone": u.phone,
                "is_bot": u.bot,
            } for u in users],
            f, ensure_ascii=False, indent=2
        )

async def dump_last_10_messages_per_user(channel):
    msgs: Dict[int, List] = defaultdict(list)
    offset = 0
    class Enc(json.JSONEncoder):
        def default(self, o): return o.isoformat() if isinstance(o, datetime) else super().default(o)
    while True:
        hist = await client(GetHistoryRequest(channel, offset, None, 0, 100, 0, 0, 0))
        if not hist.messages:
            break
        for m in hist.messages:
            if m.from_id and m.message:
                uid = getattr(m.from_id, "user_id", m.from_id)
                if len(msgs[uid]) < 10:
                    msgs[uid].append((m.id, m.message, uid))
        offset = hist.messages[-1].id
        if all(len(v) >= 10 for v in msgs.values()):
            break
    with open("channel_messages_last10.json", "w", encoding="utf-8") as f:
        json.dump(msgs, f, ensure_ascii=False, cls=Enc, indent=2)

def trigger_next():
    if not BOT_PATH.exists():
        log.error("bot.py не найден по пути %s", BOT_PATH); return
    proc = subprocess.Popen(CALL_CMD, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    log.info("Второй бот запущен (PID=%s): %s", proc.pid, " ".join(CALL_CMD))

async def worker():
    await client.start()
    log.info("Telethon-клиент запущен.")
    while True:
        if queue:
            link = queue.popleft()
            log.info("Обрабатываю %s", link)
            try:
                entity = await client.get_entity(link)
                await dump_all_participants(entity)
                await dump_last_10_messages_per_user(entity)
                log.info("Завершено для %s", link)
            except Exception as e:
                log.error("Ошибка при обработке %s: %s", link, e)
            await client.disconnect()   # снимаем блокировку .session
            trigger_next()
            break                      # выходим; если нужно дальше слушать — убери break
        else:
            await asyncio.sleep(1)

def start_worker():
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    loop.run_until_complete(worker())

if __name__ == "__main__":
    threading.Thread(target=start_worker, daemon=True).start()
    app.run(port=5001, debug=True, use_reloader=False)
