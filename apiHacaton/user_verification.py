import flask
import hashlib
import hmac
import time
import config

app = flask.Flask(__name__)


def check_telegram_auth(status : str, user : dict, auth_date : str, hash_to_check):
    """
    Проверка подписи Telegram
    """
    data = {
        "status" : status,
        "user" : user,
        "auth_date" : auth_date,
    }
    data_check_string = '\n'.join(
        sorted(f"{k}={v}" for k, v in data.items())
    )
    calculated_hash = hmac.new(config.BOT_TOKEN_SECRET,
                               data_check_string.encode(),
                               hashlib.sha256).hexdigest()

    if calculated_hash != hash_to_check:
        return False

    auth_date = int(auth_date)
    if time.time() - auth_date > 86400:
        return False

    return True


def create_user(id : str, first_name : str, last_name : str, username : str, language_code : str) -> dict:
    return {
        "id": int(id),
        "first_name": first_name,
        "last_name": last_name,
        "username": username,
        "language_code": language_code
    }


@app.route('/antispam_bot/api/auth/telegram', methods=['GET'])
def telegram_auth():
    needed_arguments = config.REQUIRED_USER_TELEGRAM_VERIFICATION_ARGUMENTS - set(flask.request.args.keys())
    if needed_arguments != set():
        return f"Invalid Arguments. Required: {needed_arguments}"
    data = flask.request.args.to_dict()
    user = create_user(
        data["user-id"],
        data["user-first-name"],
        data["user-last-name"],
        data["user-name"],
        data["language-code"]
    )
    if check_telegram_auth(data["status"], user, data["auth-date"], data["hash"]):
        return flask.jsonify({"status": "success", "user": data})
    else:
        return flask.jsonify({"status": "error", "message": "Invalid Telegram data"}), 403


if __name__ == '__main__':
    app.run(debug=True)
