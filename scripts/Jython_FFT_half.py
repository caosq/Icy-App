from org.python.modules import jarray
from edu.emory.mathcs.jtransforms.fft import DoubleFFT_1D
from java.lang import Math
import copy

def process(input, position):
    size = len(input)
    output = copy.deepcopy(input)
    fft = DoubleFFT_1D(size)
    fft.realForward(output)
    for j in range(size/2):
        output[j]= Math.sqrt(Math.pow(output[2*j],2)+Math.pow(output[2*j+1],2));
    return output[:len(input)/2]