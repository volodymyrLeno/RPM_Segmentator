# RPM_Segmentator

This is a command line tool to identify task traces (instances of a task) and assign each action within UI log to specific trace. It works with UI logs recorded by RPA UI Logger tool available at https://github.com/apromore/RPA_UILogger. 

## Usage

The tool requires the following input parameters:

* logPath - a path to UI log to be processed (String, e.g. src\logs\useCase_preprocessed.csv)
* contextThreshold - a threshold which is used to identify context attributes (if ratio of unique values for an attribute is below or equal the threshold, then this attribute is considered as context attribute) (Double [0, 1])
* preprocessing - indicates whether preprocessing should be performed (Boolean)
* considerMissing - specify whether consider attributes with missing values as candidates for context attributes (Boolean)
* approach - specifies which approach is used for routines identification ("-1" for graph based approach combined with sequnce pattern mining; "-2" for general repeats mining approach). At the moment only the first approach ("-1") is stable. 

Example how to run the tool:

```
java -jar RPM_Segmentator.jar logs/useCase_preprocessed.csv 0.05 true true -1
```

The tool generates .DOT file that represents a directly follows graph constructed from the given UI log. You can visualize this graph by using any graph vizualization tool (e.g, http://www.webgraphviz.com/)
