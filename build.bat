if not exist "bin" mkdir "bin"

javac -encoding UTF8 -cp src/de/codecoverage/utils/ -d bin src/de/codecoverage/utils/SizeOfObj.java src/de/codecoverage/utils/TestDriver.java
