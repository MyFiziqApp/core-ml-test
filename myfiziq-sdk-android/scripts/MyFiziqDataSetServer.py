from fastapi import FastAPI, Header, Request, Response
from fastapi.middleware.gzip import GZipMiddleware
from pydantic import BaseModel
from typing import Optional
import uvicorn
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import argparse
import os
import sys
import json
from PIL import Image
from starlette.responses import StreamingResponse
from starlette.responses import FileResponse
import io
import cv2
import base64
import struct

topDirectory = {"dataPath": ""}
allAttemptIds = {"count": 0, "attemptIDs": []}
allResultIds = {"count": 0, "resultIDs": []}

app = FastAPI()
app.add_middleware(GZipMiddleware, minimum_size=1000)

enctab_str = [
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', #00..12
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', #13..25
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', #26..38
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', #39..51
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '#', '$', #52..64
        '%', '&', '(', ')', '*', '+', ',', '.', '/', ':', ';', '<', '=', #65..77
        '>', '?', '@', '[', ']', '^', '_', '`', '{', '|', '}', '~', ' '] #78..90

decode_table = dict((v,k) for k,v in enumerate(enctab_str))

@app.get("/attempts")
def get_attempts_list():
    return allAttemptIds

@app.get("/attempts/{attempt_id}")
async def get_attempt(attempt_id: str):
    finalFile = getAttemptAndReturn(attempt_id)
    return finalFile

@app.get("/gt/{attempt_id}")
async def getGt(attempt_id):
    json_path = Path(str(topDirectory["dataPath"])) / f"{attempt_id}" / f"gt.json"
    return readFileFromLocation(json_path)

@app.get("/b91/{attempt_id}/{index}/{pose}/{fmt}")
async def getB91Image(attempt_id, index, pose, fmt):
    image_path = Path(str(topDirectory["dataPath"])) / f"{attempt_id}" / f"{index}" / f"{pose}.{fmt}"
    return {"base91": getImageB91String(image_path)}

@app.get("/b64/{attempt_id}/{index}/{pose}/{fmt}")
async def getImage(attempt_id, index, pose, fmt):
    image_path = Path(str(topDirectory["dataPath"])) / f"{attempt_id}" / f"{index}" / f"{pose}.{fmt}"
    return {"base64": getImageBase64String(image_path)}

    # This works HOORAY
    # img = Image.open(image_path)
    # imgByteArr = io.BytesIO()
    # img.save(imgByteArr, format=img.format)
    # imgByteArr = imgByteArr.getvalue()
    # print(len(imgByteArr))
    # return FileResponse(image_path, media_type="image/jp2")

@app.get("/meta/{attempt_id}/{index}/{item}")
async def getMeta(attempt_id, index, item):
    json_path = Path(str(topDirectory["dataPath"])) / f"{attempt_id}" / f"{index}" / f"{item}.json"
    return readFileFromLocation(json_path)

@app.get("/{environment}/list")
async def getResultsList(environment):
    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
    for name in allResultIds["resultIDs"]:
        xml += "<Contents><Key>000000/00/"
        xml += name
        xml += "/outputs.json</Key>"
        xml += "<LastModified>2020-07-07T01:15:19.000Z</LastModified>"
        xml += "<ETag>3afa567afa1d72b67a8fe8d00f4be31c</ETag>"
        xml += "</Contents>"
        #print("Send XML:"+xml)
    xml += "</ListBucketResult>"
    return Response(content=xml, media_type="application/xml")

@app.post("/{environment}/get")
async def getResultList(environment, request: Request):
    result = []
    request_data = await request.json()
    for name in request_data:
        result.append(getAttemptAndReturn(name))
    return result

@app.put("/{environment}/update")
async def putAvatar(environment, request: Request, mfz_attemptid : Optional[str] = Header(None)):
    print("Upload called for attempt " + mfz_attemptid)
    data_path = Path(str(topDirectory["dataPath"])) / f"_Outputs" / f"{mfz_attemptid}"
    if not os.path.exists(data_path):
        os.makedirs(data_path)
    result_file = data_path / "outputs.json"
    result_data = await request.body() #request.json()
    result_str = result_data.decode('utf8')
    #print("attempt data:" + result_str)
    with open(result_file, 'w', encoding='utf-8') as f:
        json.dump(json.loads(result_str), f, ensure_ascii=False, indent=4)
    allResultIds["resultIDs"].append(mfz_attemptid)
    try:
        allAttemptIds["attemptIDs"].remove(mfz_attemptid)
    except:
        print("attempt not found " + mfz_attemptid)
    return {"mfz_attemptid": mfz_attemptid}

@app.post("/payload/{attempt_id}")
async def putPayload(attempt_id, request: Request):
    print("Upload payload called for attempt " + attempt_id)
    data_path = Path(str(topDirectory["dataPath"])) / f"_Outputs" / f"{attempt_id}"
    if not os.path.exists(data_path):
        os.makedirs(data_path)
    result_file = data_path / "payload.json"
    result_data = await request.body() #request.json()
    result_str = result_data.decode('utf8')
    #print("payload data:" + result_str)
    with open(result_file, 'w', encoding='utf-8') as f:
        json.dump(json.loads(result_str), f, ensure_ascii=False, indent=4)
    return {"mfz_attemptid": attempt_id}
# Private Methods

def getImageBase64String(image_path):
    if not Path(image_path).exists():
        print("Image path does not exist: ", image_path)
        return {"Error": "Image path does not exist"}
    #print(image_path)
    with open(image_path, "rb") as f:
        encoded_string = base64.b64encode(f.read())

    return encoded_string.decode('utf-8')

def getImageB91String(image_path):
    if not Path(image_path).exists():
        print("Image path does not exist: ", image_path)
        return {"Error": "Image path does not exist"}
    #print(image_path)
    with open(image_path, "rb") as f:
        encoded_string = encodeB91(f.read())

    return encoded_string

def encodeB91(bindata):
    ''' Encode a bytearray to a Base91 string '''
    b = 0
    n = 0
    out = ''
    for count in range(len(bindata)):
        byte = bindata[count:count+1]
        b |= struct.unpack('B', byte)[0] << n
        n += 8
        if n>13:
            v = b & 8191
            if v > 88:
                b >>= 13
                n -= 13
            else:
                v = b & 16383
                b >>= 14
                n -= 14
            out += enctab_str[v % 91] + enctab_str[v // 91]
    if n:
        out += enctab_str[b % 91]
        if n>7 or b>90:
            out += enctab_str[b // 91]
    return out

def decodeB91(encoded_str):
    ''' Decode Base91 string to a bytearray '''
    v = -1
    b = 0
    n = 0
    out = bytearray()
    for strletter in encoded_str:
        if not strletter in decode_table:
            continue
        c = decode_table[strletter]
        if(v < 0):
            v = c
        else:
            v += c*91
            b |= v << n
            n += 13 if (v & 8191)>88 else 14
            while True:
                out += struct.pack('B', b&255)
                b >>= 8
                n -= 8
                if not n>7:
                    break
            v = -1
    if v+1:
        out += struct.pack('B', (b | v << n) & 255 )
    return out

def readFileFromLocation(jsonPath):
    if not os.path.isfile(jsonPath):
        return {}

    with open(jsonPath) as f:
        info = json.load(f)

    return info

def createAttemptAndReturn(attemptID):
    print("Creating attempt for id " + attemptID)
    # # This assumes there is a 0 index in every attempt...
    data_path = Path(str(topDirectory["dataPath"]))
    user_path = data_path / attemptID / "0" / "user.json"
    front_path = data_path / attemptID / "0" / "front.json"
    side_path = data_path / attemptID / "0" / "side.json"
    groundTruth = data_path / attemptID / "gt.json"
    data_list = [user_path, front_path, side_path, groundTruth]
    imageData = {
        "front": [],
        "side": []
    }
    #with ThreadPoolExecutor() as executor:
    #    results = executor.map(_getJsonResult, data_list)

    # Iterate over directories to get the attempt IDs
    imageData["front"].append(getAllImageAttempts(attemptID, "front"))
    imageData["side"].append(getAllImageAttempts(attemptID, "side"))
    # for root, dirs, files in os.walk(str(topDirectory["dataPath"] +  "/{attemptID}")):
    #     for name in dirs:
    #         imageData["front"].append(getAllImageAttempts(attemptID, "front"))
    #         imageData["side"].append(getAllImageAttempts(attemptID, "side"))
    final_res = {} #dict((name, res) for name, res in results)
    final_res["imageData"] = imageData
    final_res["attemptId"] = attemptID
    return final_res

def getAttemptAndReturn(attemptID):
    print("Getting attempt for id " + attemptID)
    inputPath = str(topDirectory["dataPath"]) + "/_Outputs/" + attemptID + "/outputs.json"
    final_res = readFileFromLocation(inputPath)
    return final_res

def getAllImageAttempts(attempt_id, pose):
    image_path = Path(str(topDirectory["dataPath"])) / f"{attempt_id}"
    base64ImagesArray = []
    for path in Path(image_path).rglob(pose + '.bmp'):
        base64ImagesArray.append(getImageBase64String(path))
    return base64ImagesArray

def _getJsonResult(data):
    if data.exists():
        with open(data, "r") as fn:
            res = json.load(fn)
            return (data.stem, res)
    else:
        print("Error fetching the data: ")
        return (data.stem, {})

# Main
def main(arguments):
    '''
        You can run this by entering in the terminal:
        [path to script]/MyFiziqDataSetServer.py -s [path to]/DataSetRAW-sans-Alpha -i [path to]/DataSet\ Base64\ images
    '''
    parser = argparse.ArgumentParser(
        description="Expects to receive the locations of the Attempts data and Base64 image json files.",
        epilog="As an alternative to the commandline, params can be placed in a file, one per line, and specified on the commandline like '%(prog)s @params.conf'.",
        fromfile_prefix_chars='@'
    )
    parser.add_argument('-s', '--srcdir', metavar=('SRC'),
                        required=True, nargs='?', help="src Directory path containing all the attempts.")
    args = parser.parse_args(arguments)

    if not os.path.exists(args.srcdir):
        raise ValueError('Source path does not exist.')
        return

    topDirectory["dataPath"] = args.srcdir
    print("Getting the data: ")

    allAttempts = []
    allResults = []

    # Iterate over directories to get the attempt IDs
    for root, dirs, files in os.walk(str(topDirectory["dataPath"])):
        for name in dirs:
            # attemptId = str(root) + "/" + name
            if len(name) > 3:
                #print("File name: ", name)
                gtJSONFilePath = str(topDirectory["dataPath"]) + "/" + name + "/gt.json"
                if os.path.isfile(gtJSONFilePath):
                    resultFilePath = str(topDirectory["dataPath"]) + "/_Outputs/" + name + "/outputs.json"
                    if os.path.isfile(resultFilePath):
                        print("Result for: ", name)
                        allResults.append(name)
                    else:
                        allAttempts.append(name)
                        #print("No result Json for attempt: " + str(topDirectory["dataPath"]) + "/" + name)
                #else:
                #    print("No GT Json for attempt: " + str(topDirectory["dataPath"]) + "/" + name)

    allAttemptIds["count"] = len(allAttempts)
    allAttemptIds["attemptIDs"] = sorted(set(allAttempts))

    allResultIds["count"] = len(allResults)
    allResultIds["resultIDs"] = sorted(set(allResults))
    # outPutPath = str(topDirectory["dataPath"]) + "/_APIJson/"
    # if not os.path.exists(outPutPath):
    #     os.makedirs(outPutPath)
    # for name in allAttempts:
    #     writePath = outPutPath + name + ".json"
    #     if not os.path.isfile(writePath):
    #         attemptJson = createAttemptAndReturn(name)
    #         with open(writePath, 'w', encoding='utf-8') as f:
    #             json.dump(attemptJson, f, ensure_ascii=False, indent=4)

    # Launch the server so it is visible across network
    uvicorn.run(app, port=5000, host='0.0.0.0')

if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
