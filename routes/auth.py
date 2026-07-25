from flask import Blueprint


auth_bp = Blueprint("auth", __name__)


@auth_bp.route("/auth-test")
def auth_test():
    return "Auth route working"