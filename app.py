from flask import Flask, render_template
from flask_socketio import SocketIO, emit
import random
import eventlet

eventlet.monkey_patch()  

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")

ram = []
swap = []
cache = set()
ram_size = 3
swap_size = 3

@app.route("/")
def index():
    return render_template("index.html")

@socketio.on("allocate_page")
def allocate_page():
    global ram, swap
    page_id = random.randint(1, 10)
    
    if len(ram) < ram_size:
        ram.append(page_id)
    else:
        removed = ram.pop(0)
        if len(swap) < swap_size:
            swap.append(removed)
        ram.append(page_id)
        emit("log_message", f"Page {removed} moved to swap.")

    emit("update_memory", {"ram": ram, "swap": swap, "cache": list(cache)}, broadcast=True)

@socketio.on("access_page")
def access_page(data):
    page_id = int(data["page_id"])
    if page_id in ram:
        emit("log_message", f"Page {page_id} accessed in RAM.")
    else:
        emit("log_message", f"Page fault! Page {page_id} not found in RAM.")

@socketio.on("add_to_cache")
def add_to_cache():
    global cache
    page_id = random.randint(1, 10)
    
    if len(cache) >= 3:
        removed = list(cache)[0]
        cache.remove(removed)
        emit("log_message", f"Page {removed} removed from cache.")
    
    cache.add(page_id)
    emit("log_message", f"Page {page_id} added to cache.")
    emit("update_memory", {"ram": ram, "swap": swap, "cache": list(cache)}, broadcast=True)

if __name__ == "__main__":
    socketio.run(app, debug=True, host="0.0.0.0", port=5000)
