from icy.main import Icy

seq = Icy.getMainInterface().getFocusedSequence()

if seq != None:
	Icy.addSequence(seq.getCopy())