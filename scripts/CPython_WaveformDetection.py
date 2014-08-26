import numpy as np
from waveDetect import waveSnippet
def process(input, position):
	SearchStart = 0
	SearchRange = 470
	distance = 300
	waveWidth = 200
	h = 7.08
	f = 1e8
	surface = waveSnippet(SearchStart,SearchRange,input)
	bottom1 = surface.getNextSnippet(distance,waveWidth)
	
	output = []
	output.append(surface.getMaxLocation())
	output.append(bottom1.getMaxAmplitude())
	return np.array(output)