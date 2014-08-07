/*
import the different Classes needed
	- Icy is needed to use getSequence() and Icy.addSequence()
	- Thresholder is needed to do the threshold
*/
importClass(Packages.icy.main.Icy)
importClass(Packages.plugins.adufour.thresholder.Thresholder)

/* 
Get the focused Sequence. This is a shortcut for:
	Icy.getMainInterface().getFocusedSequence()
*/
seq = getSequence()

// Check if sequence is null, as we cannot perform a threshold on 
// something that does not exist.
if (seq != null) {
	/*
	This one is tricky, normal call should be: 
		Thresholder.threshold(seq, 1, [127], false)
	However, this call is ambiguous as Javascript does not know which 
	of the two threshold function to call. We have to explicitly call
	the right function.
	*/
	result = Thresholder["threshold(icy.sequence.Sequence,int,double[],boolean)"](seq, 1, [127], false)
	Icy.addSequence(result)
}

