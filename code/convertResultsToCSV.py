import os
import csv 
from pathlib import Path


rootDir = 'C:\\Users\\ricar\\Documents\\Research_GMRF-CRISPR\\Results_GMRF-CRISPR\\Experiment3'

filesToCSVList = []
listOfResults = []
row = ["Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null"]
header = ["Original_file","Name","Training Time","TestingTime","Accuracy","Precision(Benign)","Precision(Bot)","Precision(DoS attacks-SlowHTTPTest)","Precision(DoS attacks-Hulk)","Precision(Infilteration)","Precision(DoS attacks-GoldenEye)","Precision(FTP-BruteForce)","Precision(SSH-Bruteforce)","Precision(DDOS attack-HOIC)","Precision(DoS attacks-Slowloris)","Precision(Brute Force -Web)","Precision(Brute Force -XSS)","Precision(SQL Injection)","Precision(DDOS attack-LOIC-UDP)","Recall(Benign)","Recall(Bot)","Recall(DoS attacks-SlowHTTPTest)","Recall(DoS attacks-Hulk)","Recall(Infilteration)","Recall(DoS attacks-GoldenEye)","Recall(FTP-BruteForce)","Recall(SSH-Bruteforce)","Recall(DDOS attack-HOI)","Recall(DoS attacks-Slowloris)","Recall(Brute Force -Web)","Recall(Brute Force -XSS)","Recall(SQL Injection)","Recall(DDOS attack-LOIC-UDP)","FPR(Benign)","FPR(Bot)","FPR(DoS attacks-SlowHTTPTest)","FPR(DoS attacks-Hulk)","FPR(Infilteration)","FPR(DoS attacks-GoldenEye)","FPR(FTP-BruteForce)","FPR(SSH-Bruteforce)","FPR(DDOS attack-HOIC)","FPR(DoS attacks-Slowloris)","FPR(Brute Force -Web)","FPR(Brute Force -XSS)","FPR(SQL Injection)","FPR(DDOS attack-LOIC-UDP)","F1-Score(Benign)","F1-Score(Bot)","F1-Score(DoS attacks-SlowHTTPTest)","F1-Score(DoS attacks-Hulk)","F1-Score(Infilteration)","F1-Score(DoS attacks-GoldenEye)","F1-Score(FTP-BruteForce)","F1-Score(SSH-Bruteforce)","F1-Score(DDOS attack-HOIC)","F1-Score(DoS attacks-Slowloris)","F1-Score(Brute Force -Web)","F1-Score(Brute Force -XSS)","F1-Score(SQL Injection)","F1-Score(DDOS attack-LOIC-UDP)","WeightedPrecision","WeightedRecall","WeightedF1Score","WeightedFalsePositiveRate", "NumForests","NumTrees","Algo","TrainingSplit"]


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

	#Go throw each file
	file = dirName + '\\' + biggestFile
	try:
		with open(file, 'r') as f:
			#print(file)
			for line in f:
				#print(f.name)
				#print(line)
				l = line.rstrip()
				if l:
					#print(l)
					characteristics = l.split('=')
					#print(characteristics)

					if characteristics[0] == 'Name':
						listOfResults.append(row)
						row = ["Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null","Null"]

						row[0] = f.name
						#name = characteristics[1]
						row[1] = characteristics[1]#name[:-1]

						split_path = f.name.split('\\')
						forest_spects = split_path[7]
						split_forest_spects = forest_spects.split('-')
						forests_label, num_forest = split_forest_spects[3].split('.')
						trees_label, num_trees = split_forest_spects[4].split('.')

						training_split_label,training_split = split_forest_spects[10].split('.')
						row[65] = num_forest
						row[66] = num_trees
						row[67] = forest_spects
						row[68] = training_split
					elif characteristics[0] == 'TrainingTime':
						row[2] = characteristics[1]
					elif characteristics[0] == 'TestingTime':
						row[3] = characteristics[1]
					elif characteristics[0] == 'Accuracy':
						row[4] = characteristics[1]
					elif characteristics[0] == 'Precision(0.0)':
						row[5] = characteristics[1]
					elif characteristics[0] == 'Precision(1.0)':
						row[6] = characteristics[1]
					elif characteristics[0] == 'Precision(2.0)':
						row[7] = characteristics[1]
					elif characteristics[0] == 'Precision(3.0)':
						row[8] = characteristics[1]
					elif characteristics[0] == 'Precision(4.0)':
						row[9] = characteristics[1]
					elif characteristics[0] == 'Precision(5.0)':
						row[10] = characteristics[1]
					elif characteristics[0] == 'Precision(6.0)':
						row[11] = characteristics[1]
					elif characteristics[0] == 'Precision(7.0)':
						row[12] = characteristics[1]
					elif characteristics[0] == 'Precision(8.0)':
						row[13] = characteristics[1]
					elif characteristics[0] == 'Precision(9.0)':
						row[14] = characteristics[1]
					elif characteristics[0] == 'Precision(10.0)':
						row[15] = characteristics[1]
					elif characteristics[0] == 'Precision(11.0)':
						row[3] = characteristics[1]
					elif characteristics[0] == 'Precision(12.0)':
						row[16] = characteristics[1]
					elif characteristics[0] == 'Precision(13.0)':
						row[17] = characteristics[1]
					elif characteristics[0] == 'Recall(0.0)':
						row[18] = characteristics[1]
					elif characteristics[0] == 'Recall(1.0)':
						row[19] = characteristics[1]
					elif characteristics[0] == 'Recall(2.0)':
						row[20] = characteristics[1]
					elif characteristics[0] == 'Recall(3.0)':
						row[21] = characteristics[1]
					elif characteristics[0] == 'Recall(4.0)':
						row[22] = characteristics[1]
					elif characteristics[0] == 'Recall(5.0)':
						row[23] = characteristics[1]
					elif characteristics[0] == 'Recall(6.0)':
						row[24] = characteristics[1]
					elif characteristics[0] == 'Recall(7.0)':
						row[25] = characteristics[1]
					elif characteristics[0] == 'Recall(8.0)':
						row[26] = characteristics[1]
					elif characteristics[0] == 'Recall(9.0)':
						row[27] = characteristics[1]
					elif characteristics[0] == 'Recall(10.0)':
						row[28] = characteristics[1]
					elif characteristics[0] == 'Recall(11.0)':
						row[29] = characteristics[1]
					elif characteristics[0] == 'Recall(12.0)':
						row[30] = characteristics[1]
					elif characteristics[0] == 'Recall(13.0)':
						row[31] = characteristics[1]
					elif characteristics[0] == 'FPR(0.0)':
						row[32] = characteristics[1]
					elif characteristics[0] == 'FPR(1.0)':
						row[33] = characteristics[1]
					elif characteristics[0] == 'FPR(2.0)':
						row[34] = characteristics[1]
					elif characteristics[0] == 'FPR(3.0)':
						row[35] = characteristics[1]
					elif characteristics[0] == 'FPR(4.0)':
						row[36] = characteristics[1]
					elif characteristics[0] == 'FPR(5.0)':
						row[37] = characteristics[1]
					elif characteristics[0] == 'FPR(6.0)':
						row[38] = characteristics[1]
					elif characteristics[0] == 'FPR(7.0)':
						row[39] = characteristics[1]
					elif characteristics[0] == 'FPR(8.0)':
						row[40] = characteristics[1]
					elif characteristics[0] == 'FPR(9.0)':
						row[41] = characteristics[1]
					elif characteristics[0] == 'FPR(10.0)':
						row[42] = characteristics[1]
					elif characteristics[0] == 'FPR(11.0)':
						row[43] = characteristics[1]
					elif characteristics[0] == 'FPR(12.0)':
						row[44] = characteristics[1]
					elif characteristics[0] == 'FPR(13.0)':
						row[45] = characteristics[1]
					elif characteristics[0] == 'F1-Score(0.0)':
						row[46] = characteristics[1]
					elif characteristics[0] == 'F1-Score(1.0)':
						row[47] = characteristics[1]
					elif characteristics[0] == 'F1-Score(2.0)':
						row[48] = characteristics[1]
					elif characteristics[0] == 'F1-Score(3.0)':
						row[49] = characteristics[1]
					elif characteristics[0] == 'F1-Score(4.0)':
						row[50] = characteristics[1]
					elif characteristics[0] == 'F1-Score(5.0)':
						row[51] = characteristics[1]
					elif characteristics[0] == 'F1-Score(6.0)':
						row[52] = characteristics[1]
					elif characteristics[0] == 'F1-Score(7.0)':
						row[53] = characteristics[1]
					elif characteristics[0] == 'F1-Score(8.0)':
						row[54] = characteristics[1]
					elif characteristics[0] == 'F1-Score(9.0)':
						row[55] = characteristics[1]
					elif characteristics[0] == 'F1-Score(10.0)':
						row[56] = characteristics[1]
					elif characteristics[0] == 'F1-Score(11.0)':
						row[57] = characteristics[1]
					elif characteristics[0] == 'F1-Score(12.0)':
						row[58] = characteristics[1]
					elif characteristics[0] == 'F1-Score(13.0)':
						row[59] = characteristics[1]
					elif characteristics[0] == 'WeightedPrecision':
						row[60] = characteristics[1]
					elif characteristics[0] == 'WeightedRecall':
						row[62] = characteristics[1]
					elif characteristics[0] == 'WeightedF1Score':
						row[63] = characteristics[1]
					elif characteristics[0] == 'WeightedFalsePositiveRate':
						row[64] = characteristics[1]
					#else:
						#print(characteristics)
	except Exception as e:
		print(e)
		pass

listOfResults.pop(0) #pop the first one that is empty on widnows.


listOfResults = [header] + listOfResults
#print(listOfResults)


#print(srtToCSV)

write_file = open('C:\\Users\\ricar\\Documents\\Research_GMRF-CRISPR\\Results_GMRF-CRISPR_forR\\GMRF-CRISPRdata_Experiment3.csv', 'w+',newline ='')



with write_file: 
	write = csv.writer(write_file) 
	write.writerows(listOfResults)            
	
write_file.close()
	


