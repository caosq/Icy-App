importClass(Packages.icy.main.Icy)

/* 
Get the focused Sequence. This is a shortcut for:
	Icy.getMainInterface().getFocusedSequence()
*/
seq = getSequence()

if (seq != null)
	Icy.addSequence(seq)