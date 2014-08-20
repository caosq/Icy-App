This folder is used by FeatureExtractionInPython plugin as a script library, 
in order to show the scripts in the library, you need to name the file with the pattern "eva_XXXXXX.py",
replace XXXXXX with your own name.

To define a usable feature extraction function, you need to define a function named "process".
It should be defined like this:

def process(input, position):

	output = input
	# do something here with output
	
    return output