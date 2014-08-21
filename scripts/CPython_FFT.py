import numpy as np
def process(input, position):
	#put your python(CPython) code below
	#do something with input
	output = np.fft.fft(input)
	output = np.abs(output)
	return output