from flask import Flask, jsonify
import json
import subprocess
import sys
from pathlib import Path
import atexit

app = Flask(__name__)

PROJECT_DIR = Path(__file__).resolve().parent
FOURTH_PATH = PROJECT_DIR / "safdasasd.py"
CMD_FOURTH = [sys.executable, str(FOURTH_PATH)]

def launch_fourth():
    try:
        subprocess.Popen(CMD_FOURTH)
        app.logger.info("⭢ fourth_bot.py запущен: %s", " ".join(CMD_FOURTH))
    except Exception as e:
        app.logger.error("❌ Ошибка запуска fourth_bot: %s", e)

@atexit.register
def on_shutdown():
    app.logger.info("Flask завершается. Запускаем fourth_bot.py…")
    launch_fourth()

@app.route("/violations", methods=["GET"])
def get_violations():
    try:
        with open("violations.json", "r", encoding="utf-8") as file:
            data = json.load(file)
        return jsonify(data)
    except FileNotFoundError:
        return jsonify({"error": "violations.json not found"}), 404
    except json.JSONDecodeError:
        return jsonify({"error": "Invalid JSON format"}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0",port=5002)
