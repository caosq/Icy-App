import numpy as np
class waveSnippet():
	def __init__(self,offset, length, parentWaveform):
		self.offset = offset
		self.length = length
		self.waveform = parentWaveform[offset:offset+length]
		self.parentWaveform = parentWaveform
		self.getLocation = self.getMaxLocation
		self.paramDict = {}
	def getMaxLocation(self):
		if not 'maxLocation' in self.paramDict:
			self.paramDict['maxLocation'] = self.offset + self.waveform.argmax()
		return self.paramDict['maxLocation']
	def getMinLocation(self):
		if not 'minLocation' in self.paramDict:
			self.paramDict['minLocation'] =  self.offset + self.waveform.argmin()
		return self.paramDict['minLocation']
	def getMaxAmplitude(self):
		if not 'maxAmplitude' in self.paramDict:
			self.paramDict['maxAmplitude'] =  self.waveform.max()
		return self.paramDict['maxAmplitude']
	def getMinAmplitude(self):
		if not 'minAmplitude' in self.paramDict:
			self.paramDict['minAmplitude'] =  self.waveform.min()
		return self.paramDict['minAmplitude']
	def getWaveRangeArray(self):
		if not 'waveRangeArray' in self.paramDict:
			self.paramDict['waveRangeArray'] = np.zeros((self.parentWaveform.shape[0]))
			self.paramDict['waveRangeArray'][self.offset:self.offset+self.length] +=self.getMaxAmplitude() *0.5
		return self.paramDict['waveRangeArray']
	def getNextSnippet(self,distance,length):
		newoffset = self.getLocation() + distance - length/2
		return waveSnippet(newoffset,length,self.parentWaveform)

def process(input, position):
	SearchStart = 0
	SearchRange = 470
	distance = 300
	waveWidth = 200
	h = 7.08
	f = 1e8
	surface = waveSnippet(SearchStart,SearchRange,input)
	bottom1 = surface.getNextSnippett(distance,waveWidth)
	output = np.array([surface.getMaxLocation(),bottom1.getMaxAmplitude()])
	return output

