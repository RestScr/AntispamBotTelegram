import flask
import mysql.connector
import config

app = flask.Flask(__name__)


@app.route("/antispam_bot/api/logs/", methods=["GET"])
def logs():
    connection = mysql.connector.connect(
        host=config.MYSQL_HOST,
        user=config.MYSQL_USER,
        password=config.MYSQL_PASSWORD,
        database=config.MYSQL_DATABASE
    )
    cursor = connection.cursor()
    cursor.execute("SELECT * FROM admin_logs")
    logs = cursor.fetchall()
    print(logs)


if __name__ == "__main__":
    app.run(True)

