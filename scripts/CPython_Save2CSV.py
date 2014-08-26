import os
print('save file to' + os.getcwd())
path = 'output.csv'
if os.path.exists(path):
    os.remove(path)
    
def process(input, position):
	global path
    
	output = input
    
	file = open(path,'a')
	outputStr = [str(d) for d in output]
	file.write("%f,%f,%s\\n"%(position[0],position[1],",".join(outputStr)))
	file.close()
	return  output
