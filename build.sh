#!/bin/bash

if [ ! -d "bin" ]; then
  mkdir -p "bin"
fi

javac -encoding UTF8 -cp src/de/codecoverage/utils/ -d bin src/de/codecoverage/utils/SizeOfObj.java src/de/codecoverage/utils/TestDriver.java