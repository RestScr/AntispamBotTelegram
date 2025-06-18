from flask import Flask, request, jsonify
import mysql.connector, os, config
from flask_cors import CORS

app = Flask(__name__)
CORS(app, resources={
    r"/antispam_bot/*": {
        "origins": [
            "https://xxxxxneonxxxxx.github.io/hacaton",
            "http://localhost:3000"
        ]
    }
})

@app.route("/antispam_bot/api/user", methods=["GET", "POST", "OPTIONS"])
def users():
    if request.method == "OPTIONS":
        resp = jsonify({"ok": True})
        resp.headers.update({
            "Access-Control-Allow-Origin": "http://localhost:3000",
            "Access-Control-Allow-Headers": "Content-Type,Authorization",
            "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
        })
        return resp, 200

    # --- 1. tg-id ---
    if request.method == "POST":
        body = request.get_json(silent=True) or {}
        print(f"[REQ] POST body: {body}")
        user_id = body.get("tg")
    else:
        user_id = request.args.get("tg")
        print(f"[REQ] GET param tg: {user_id}")

    if not user_id:
        print("[ERR] tg-id отсутствует")
        return jsonify({"error": "tg-id is required"}), 400
    try:
        user_id = int(user_id)
    except (ValueError, TypeError):
        print(f"[ERR] tg-id не число: {user_id}")
        return jsonify({"error": "tg-id must be int"}), 400

    # --- 2. DB connect ---
    try:
        conn = mysql.connector.connect(
            host=config.MYSQL_HOST,
            user=config.MYSQL_USER,
            password=config.MYSQL_PASSWORD,
            database=config.MYSQL_DATABASE
        )
        print("[DB] Успешное подключение")
    except Exception as e:
        print("[DB] connect fail:", e)
        return jsonify({"error": "DB connect failed"}), 500

    output = []
    try:
        cur = conn.cursor(dictionary=True)

        # 3. chat_id для tg_id
        cur.execute("SELECT chat_id FROM chat_admins WHERE tg_id = %s", (user_id,))
        chat_ids = [row["chat_id"] for row in cur.fetchall()]
        print(f"[DB] Найдено чатов у админа {user_id}: {chat_ids}")

        for cid in chat_ids:
            print(f"[DB] Обработка чата {cid}")

            # --- сведения о чате ---
            cur.execute("SELECT * FROM chats WHERE id = %s", (cid,))
            chat_row = cur.fetchone()
            if not chat_row:
                print(f"[DB] Чат {cid} не найден в таблице chats")
                continue

            print(f"[DB] Чат {cid} данные: {chat_row}")

            chat = {
                "id":    chat_row.get("id"),
                "title": chat_row.get("title") or "",
                "users": chat_row.get("users") or 0,
                "filters": [],
                "whitelist": []
            }

            # --- фильтры ---
            cur.execute("SELECT * FROM filters WHERE chatID = %s", (cid,))
            filters = cur.fetchall() or []
            print(f"[DB] Фильтров для {cid}: {len(filters)} шт.")
            chat["filters"] = [
                {
                    "id":   f.get("id"),
                    "type": f.get("type")  or "",
                    "data": f.get("data")  or ""
                } for f in filters
            ]

            # --- whitelist ---
            cur.execute("SELECT * FROM whitelist WHERE chatID = %s", (cid,))
            wl = cur.fetchall() or []
            print(f"[DB] Вайтлистов для {cid}: {len(wl)} шт.")
            chat["whitelist"] = [
                {
                    "id":       w.get("id"),
                    "username": w.get("username") or ""
                } for w in wl
            ]

            output.append(chat)

        cur.close()
    except Exception as e:
        print("[DB] error:", e)
        return jsonify({"error": "Database query failed"}), 500
    finally:
        conn.close()
        print("[DB] Соединение закрыто")

    if not output:
        print("[RES] Нет доступных чатов — отправляем data=[0, 0, 0]")
        return jsonify({"ok": True, "data": [0, 0, 0]}), 200

    print(f"[RES] Отдаём {len(output)} чатов")
    return jsonify({"ok": True, "data": output}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", 5001)), debug=True)
