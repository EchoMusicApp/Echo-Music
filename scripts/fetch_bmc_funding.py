import urllib.request
import json
import os
import datetime

# Target date for the start of the lossless funding (YYYY-MM-DD)
current_month_start = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-01")
TARGET_DATE_STR = os.environ.get("BMC_START_DATE", current_month_start)
TARGET_DATE = datetime.datetime.strptime(TARGET_DATE_STR, "%Y-%m-%d").replace(tzinfo=datetime.timezone.utc)
TARGET_AMOUNT = 200.0

# Buy Me A Coffee API token
BMC_TOKEN = os.environ.get("BMC_TOKEN")

if not BMC_TOKEN:
    print("BMC_TOKEN environment variable not set.")
    exit(1)

API_URL = "https://developers.buymeacoffee.com/api/v1/supporters"

headers = {
    "Authorization": f"Bearer {BMC_TOKEN}",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json"
}

total_raised = 0.0
page = 1

print(f"Fetching Buy Me A Coffee supporters since {TARGET_DATE_STR}...")

while True:
    req = urllib.request.Request(f"{API_URL}?page={page}", headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
    except urllib.error.URLError as e:
        print(f"Error fetching data: {e}")
        exit(1)

    supporters = data.get("data", [])
    if not supporters:
        break

    for supporter in supporters:
        created_on_str = supporter.get("support_created_on") # Format: "2023-01-01 12:00:00"
        
        # Parse datetime and assume UTC
        created_on = datetime.datetime.strptime(created_on_str, "%Y-%m-%d %H:%M:%S").replace(tzinfo=datetime.timezone.utc)
        
        if created_on >= TARGET_DATE:
            coffees = supporter.get("support_coffees", 1)
            price = float(supporter.get("support_coffee_price", 5.0))
            total_raised += (coffees * price)

    # Check if there's a next page
    if data.get("current_page") >= data.get("last_page"):
        break
    
    page += 1

print(f"Total raised since {TARGET_DATE_STR}: ${total_raised:.2f}")

# Generate the JSON output
output_data = {
    "target": TARGET_AMOUNT,
    "raised": total_raised,
    "percentage": min(100, int((total_raised / TARGET_AMOUNT) * 100)),
    "last_updated": datetime.datetime.now(datetime.timezone.utc).isoformat()
}

os.makedirs("app/src/main/assets", exist_ok=True)
with open("app/src/main/assets/funding.json", "w") as f:
    json.dump(output_data, f, indent=4)

print("Saved funding.json to app/src/main/assets/funding.json")
