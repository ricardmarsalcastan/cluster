import os
import csv 
from pathlib import Path

rootDir = 'C:\\Users\\ricar\\Documents\\Research_SupervisedMLRF\\Results_SupMLRF_readyToAnalyze'

filesToCSVList = []

for dirName, subdirList, fileList in os.walk(rootDir):

	biggestFile = " "
	biggestFileSize = 0

	for fname in fileList:
		try:
			if biggestFileSize < os.stat(dirName +'\\'+ fname).st_size:
				biggestFile = fname
				biggestFileSize = os.stat(dirName +'\\'+ fname).st_size
				
			
		except Exception as e:
			print(e)

	filesToCSVList.append(dirName + '\\' + biggestFile)

srtToCSV = [] #"Original file","Type","Accuracy","Weighted Recall","False Positive Rate"
listOfResults = []
for i in range(1, len(filesToCSVList)):
	listOfResults.append(filesToCSVList[i] + "\n")
	with open(filesToCSVList[i]) as f:
		for line in f:
			listOfResults += f.readlines()


#print(listOfResults)
count = 1
row = []
for obj in listOfResults:
	if "C:\\" in obj:
		currentDir = obj
		
	else:
		row.append(obj.rstrip())


	if count % 5 == 0:
		row = [currentDir] + row
		srtToCSV.append(row)
		print("full " ,row)
		row = []
		count = 1

	
	count += 1
	print(row)

	


print(srtToCSV)

writeFile = open('C:\\Users\\ricar\\Documents\\Research_SupervisedMLRF\\Results_SupMLRF_forR\\CRISPRdata.csv', 'w+',newline ='')

with writeFile:     
	write = csv.writer(writeFile) 
	write.writerows(srtToCSV)            

writeFile.close()
	



		