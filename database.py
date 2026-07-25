import os

import pymysql
from dotenv import load_dotenv


# Project folder eke .env file eka load karanawa
load_dotenv()


def get_mysql_connection():
    """
    NaturalFoliageLab MySQL database ekata connection ekak return karanawa.
    """

    return pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USER"),
        password=os.getenv("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DATABASE"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def test_mysql_connection():
    """
    MySQL connection eka hariyata wada karanawada test karanawa.
    """

    connection = None

    try:
        connection = get_mysql_connection()

        with connection.cursor() as cursor:
            cursor.execute("SELECT DATABASE() AS database_name")
            result = cursor.fetchone()

        print("✅ MySQL connection successful")
        print("✅ Connected database:", result["database_name"])

    except Exception as error:
        print("❌ MySQL connection failed")
        print("Error:", error)

    finally:
        if connection:
            connection.close()


if __name__ == "__main__":
    test_mysql_connection()