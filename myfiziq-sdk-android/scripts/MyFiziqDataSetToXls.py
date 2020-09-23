from fastapi import FastAPI, Header, Request
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
import xlsxwriter
from xlsxwriter.utility import xl_rowcol_to_cell

topDirectory = {"dataPath": ""}
allAttemptIds = {"count": 0, "attemptIDs": []}
allResults = []

# Private Methods
class counter(object):
    def __init__(self,v=0):
        self.set(v)

    def preinc(self):
        self.v += 1
        return self.v
    def predec(self):
        self.v -= 1
        return self.v

    def postinc(self):
        self.v += 1
        return self.v - 1
    def postdec(self):
        self.v -= 1
        return self.v + 1

    def __add__(self,addend):
        return self.v + addend
    def __sub__(self,subtrahend):
        return self.v - subtrahend
    def __mul__(self,multiplier):
        return self.v * multiplier
    def __div__(self,divisor):
        return self.v / divisor

    def __getitem__(self):
        return self.v

    def __str__(self):
        return str(self.v)

    def set(self,v):
        if type(v) != int:
            v = 0
        self.v = v

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

def inc(name, local={}):
    #Equivalent to name++
    if name in local:
        local[name]+=1
        return local[name]-1
    globals()[name]+=1
    return globals()[name]-1

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
                gtJSONFilePath = str(topDirectory["dataPath"]) + "/" + name + "/gt.json"
                if os.path.isfile(gtJSONFilePath):
                    resultFilePath = str(topDirectory["dataPath"]) + "/_Outputs/" + name + "/outputs.json"
                    if os.path.isfile(resultFilePath):
                        allResults.append(name)
                        #print("Result: ", name)
                    else:
                        allAttempts.append(name)
                        #print("Attempt: ", name)
                        #print("No result Json for attempt: " + str(topDirectory["dataPath"]) + "/" + name)
                #else:
                #    print("No GT Json for attempt: " + str(topDirectory["dataPath"]) + "/" + name)

    allResults = sorted(set(allResults))
    allAttemptIds["count"] = len(allAttempts)
    allAttemptIds["attemptIDs"] = sorted(set(allAttempts))

    workbook = xlsxwriter.Workbook(str(topDirectory["dataPath"]) + "/_Outputs/results.xlsx")
    worksheet = workbook.add_worksheet()

    cell_format = workbook.add_format()
    onedp_format = workbook.add_format({'num_format': '0.0'})

    #cell_format.set_pattern(1)  # This is optional when using a solid fill.
    #cell_format.set_bg_color('green')
    cell_format.set_left(1)
    calc_acc = 1;
    col = counter()
    worksheet.write(0, col.postinc(), 'Attempt')
    worksheet.write(0, col.postinc(), 'Gender')
    worksheet.write(0, col.postinc(), 'Height')
    worksheet.write(0, col.postinc(), 'Weight')

    worksheet.write(0, col.postinc(), 'GT Chest', cell_format)
    worksheet.write(0, col.postinc(), 'MCP Chest')
    worksheet.write(0, col.postinc(), 'RES Chest')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.write(0, col.postinc(), 'GT Waist', cell_format)
    worksheet.write(0, col.postinc(), 'MCP Waist')
    worksheet.write(0, col.postinc(), 'RES Waist')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.write(0, col.postinc(), 'GT Hips', cell_format)
    worksheet.write(0, col.postinc(), 'MCP Hips')
    worksheet.write(0, col.postinc(), 'RES Hips')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.write(0, col.postinc(), 'GT Thigh', cell_format)
    worksheet.write(0, col.postinc(), 'MCP Thigh')
    worksheet.write(0, col.postinc(), 'RES Thigh')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.write(0, col.postinc(), 'GT Inseam', cell_format)
    worksheet.write(0, col.postinc(), 'MCP Inseam')
    worksheet.write(0, col.postinc(), 'RES Inseam')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.write(0, col.postinc(), 'GT PBF', cell_format)
    worksheet.write(0, col.postinc(), 'MCP PBF')
    worksheet.write(0, col.postinc(), 'RES PBF')
    if (calc_acc):
        worksheet.write(0, col.postinc(), 'GT MCP %')
        worksheet.write(0, col.postinc(), 'GT %')

    worksheet.freeze_panes(1, 0)

    row = 1
    for name in allResults:
        print("Process: ", name)
        col = counter()
        result_file = Path(str(topDirectory["dataPath"])) / f"_Outputs" / f"{name}" / "outputs.json"
        gt_file = Path(str(topDirectory["dataPath"])) / f"{name}" / f"gt.json"
        mcp_file = Path(str(topDirectory["dataPath"])) / f"_MCP" / f"MCPv2_results" / f"{name}.json"
        result_data = readFileFromLocation(result_file)
        gt_data = readFileFromLocation(gt_file)
        mcp_data = readFileFromLocation(mcp_file)
        worksheet.write(row, col.postinc(), name)
        worksheet.write(row, col.postinc(), gt_data['gender'])
        worksheet.write(row, col.postinc(), gt_data['height'])
        worksheet.write(row, col.postinc(), gt_data['weight'])

        worksheet.write(row, col.postinc(), gt_data['chest'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['chest'])
        worksheet.write(row, col.postinc(), result_data['chest'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        worksheet.write(row, col.postinc(), gt_data['waist'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['waist'])
        worksheet.write(row, col.postinc(), result_data['waist'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        worksheet.write(row, col.postinc(), gt_data['hips'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['hip'])
        worksheet.write(row, col.postinc(), result_data['hip'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        worksheet.write(row, col.postinc(), gt_data['thigh'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['thigh'])
        worksheet.write(row, col.postinc(), result_data['thigh'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        worksheet.write(row, col.postinc(), gt_data['inseam'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['inseam'])
        worksheet.write(row, col.postinc(), result_data['inseam'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        worksheet.write(row, col.postinc(), gt_data['PBF'], cell_format)
        worksheet.write(row, col.postinc(), mcp_data['PercentBodyFat'])
        worksheet.write(row, col.postinc(), result_data['PercentBodyFat'])
        if (calc_acc):
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-3)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-3)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)
            formula = '=100-(ABS('+xl_rowcol_to_cell(row, col-4)+'-'+xl_rowcol_to_cell(row, col-2)+')'+'/'+xl_rowcol_to_cell(row, col-4)+'*100)'
            worksheet.write_formula(row, col.postinc(), formula, onedp_format)

        row += 1

    workbook.close()
    print("Processed "+str(row)+" items")

if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
