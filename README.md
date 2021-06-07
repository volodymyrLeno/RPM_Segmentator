# RPM_Segmentator

This is a command line tool to discover routine tasks from unsegmented UI logs. It works with UI logs recorded by RPA UI Logger tool available at https://github.com/apromore/RPA_UILogger. 

## Usage

The tool requires the following input parameters:

* logPath - a path to UI log to be processed (e.g. src\logs\useCase_preprocessed.csv)
* configPath - a path to configuration file (the file has to be in json format)
* groundTruthFilePath - a path to the file with ground truth routines (if available). If the ground truth is not available set it to "null"

The example of the configuration file:

```javascript
{ "preprocessing": true,
  "algorithm": "CloFast",
  "minSupport": 0.1,
  "minCoverage": 0.05,
  "metric": : "cohesion",
  "context": [
      "target.id",
      "target.name",
      "target.innerText",
      "target.sheetName"
  ]}
```

* preprocessing - specifies whether the redundant actions should be removed
* algorithm - the algorithm used to mine frequent patterns (at the moment BIDE and CloFast are supported)
* minSupport - the miminal relative frequency of the pattern to be considered a candidate routine
* minCoverage - the minimal amount of the behavior in the log that has to be covered by the routine
* metric - used for patterns selection (frequency, coverage, cohesion and length are supported metrics)
* context - a collection of the attributes that are used to distinguish the actions in the log

Example how to run the tool:

```bash
cd out/artifacts/RPM_Segmentator_jar/
java -jar RPM_Segmentator.jar logs/StudentRecord.csv config.json null
```

The tool generates .DOT file that represents a directly follows graph constructed from the given UI log. You can visualize this graph by using any graph vizualization tool (e.g, http://www.webgraphviz.com/)
