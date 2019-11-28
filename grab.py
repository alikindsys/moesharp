from selenium import webdriver
from browsermobproxy import Server
import psutil
import json
import time
import sys
import os
if(len(sys.argv) == 1):
    print("Did you just called the script mannualy to see what it does without supplying an url?")
else:
    lnk = sys.argv[1]
    for proc in psutil.process_iter():
        # check whether the process name matches
        if proc.name() == "browsermob-proxy":
            proc.kill()

    dict = {'port': 8090}
    server = Server(path="./bmp/bin/browsermob-proxy", options=dict)
    server.start()
    time.sleep(1)
    proxy = server.create_proxy()
    time.sleep(1)
    chrome_options = webdriver.ChromeOptions()
    selenium_proxy = proxy.selenium_proxy()
    path = "./chromedriver"
    if (os.name == 'nt' ):
        path = "".join([path,".exe"])
    chrome_options.add_argument("--headless")
    chrome_options.add_argument("--proxy-server={0}".format(proxy.proxy))
    driver = webdriver.Chrome(
        executable_path=path, chrome_options=chrome_options)

    proxy.new_har(lnk, options={
        'captureHeaders': True, 'captureCookies': True})
    f = open("har.json", 'w')
    driver.get(lnk)
    print(json.dumps(proxy.har, sort_keys=True, indent='\t',
                     separators=(',', ': ')), file=f)  # returns a HAR JSON blob
    server.stop()
    driver.quit()
