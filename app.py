from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # Allow cross-origin requests (for frontend)

ram = []
swap = []
cache = set()
RAM_SIZE = 3
SWAP_SIZE = 3
CACHE_SIZE = 3

@app.route('/allocate', methods=['POST'])
def allocate_page():
    global ram, swap
    page_id = request.json.get("pageId")

    if len(ram) < RAM_SIZE:
        ram.append(page_id)
    else:
        removed = ram.pop(0)  # Remove oldest page from RAM
        if len(swap) < SWAP_SIZE:
            swap.append(removed)
        ram.append(page_id)

    return jsonify({"ram": ram, "swap": swap, "cache": list(cache)})

@app.route('/access', methods=['POST'])
def access_page():
    page_id = request.json.get("pageId")

    if page_id in ram:
        return jsonify({"message": f"Page {page_id} accessed in RAM.", "status": "hit"})
    else:
        return jsonify({"message": f"Page fault! Page {page_id} not found in RAM.", "status": "miss"})

@app.route('/cache', methods=['POST'])
def add_to_cache():
    global cache
    page_id = request.json.get("pageId")

    if len(cache) >= CACHE_SIZE:
        cache.pop()  # Remove oldest cached item

    cache.add(page_id)
    return jsonify({"ram": ram, "swap": swap, "cache": list(cache)})

if __name__ == '__main__':
    app.run(debug=True)
